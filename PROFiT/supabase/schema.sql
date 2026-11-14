-- =====================================================================
-- PROFiT — controle de acesso por convite
-- Rode este arquivo inteiro no Supabase: SQL Editor → New query → Run.
--
-- O banco guarda SÓ: e-mail da conta, status (ativo/bloqueado) e qual
-- convite foi usado. Dieta, treinos, cargas e marcações ficam no celular
-- de cada pessoa e nunca são enviados.
-- =====================================================================

-- ---------- Tabelas ----------
create table if not exists public.convites (
  codigo      text primary key,
  usos_max    int  not null default 1 check (usos_max > 0),
  usos        int  not null default 0,
  ativo       boolean not null default true,
  expira_em   timestamptz,
  observacao  text,                      -- ex.: "Ana", "turma de sábado"
  criado_em   timestamptz not null default now()
);

create table if not exists public.membros (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  email      text,
  status     text not null default 'ativo' check (status in ('ativo','bloqueado')),
  convite    text references public.convites(codigo) on delete set null,
  criado_em  timestamptz not null default now()
);

-- ---------- Segurança (RLS) ----------
alter table public.convites enable row level security;
alter table public.membros  enable row level security;

-- Ninguém pelo app lê/escreve convites diretamente (só via funções abaixo).
revoke all on public.convites from anon, authenticated;

-- Cada pessoa só enxerga a própria linha; ninguém insere/edita pelo app.
revoke insert, update, delete on public.membros from anon, authenticated;
drop policy if exists "membro ve a propria linha" on public.membros;
create policy "membro ve a propria linha" on public.membros
  for select to authenticated using (auth.uid() = user_id);

-- ---------- Funções usadas pelo app ----------

-- Confere se um código pode ser usado (chamado antes de criar a conta).
create or replace function public.validar_convite(p_codigo text)
returns boolean
language sql stable security definer set search_path = public
as $$
  select exists (
    select 1 from public.convites
    where codigo = upper(trim(p_codigo))
      and ativo
      and usos < usos_max
      and (expira_em is null or expira_em > now())
  );
$$;

-- Usa o convite e libera a conta logada. Retorna 'ativo', 'bloqueado' ou 'convite_invalido'.
create or replace function public.resgatar_convite(p_codigo text)
returns text
language plpgsql security definer set search_path = public
as $$
declare
  v_uid    uuid := auth.uid();
  v_cod    text := upper(trim(p_codigo));
  v_status text;
begin
  if v_uid is null then
    raise exception 'não autenticado';
  end if;

  select status into v_status from public.membros where user_id = v_uid;
  if found then
    return v_status;               -- já é membro: não gasta outro convite
  end if;

  update public.convites
     set usos = usos + 1
   where codigo = v_cod
     and ativo
     and usos < usos_max
     and (expira_em is null or expira_em > now());
  if not found then
    return 'convite_invalido';
  end if;

  insert into public.membros (user_id, email, convite)
  values (v_uid, auth.jwt() ->> 'email', v_cod);
  return 'ativo';
end;
$$;

-- Situação da conta logada: 'ativo' | 'bloqueado' | 'sem_convite'.
create or replace function public.meu_acesso()
returns text
language sql stable security definer set search_path = public
as $$
  select coalesce(
    (select status from public.membros where user_id = auth.uid()),
    'sem_convite'
  );
$$;

revoke all on function public.validar_convite(text)  from public;
revoke all on function public.resgatar_convite(text) from public;
revoke all on function public.meu_acesso()           from public;
grant execute on function public.validar_convite(text)  to anon, authenticated;
grant execute on function public.resgatar_convite(text) to authenticated;
grant execute on function public.meu_acesso()           to authenticated;

-- ---------- Funções só para você (rode no SQL Editor) ----------

-- Gera códigos de convite. Ex.: select * from gerar_convites(5);          -- 5 convites de 1 uso
--                              select * from gerar_convites(1, 20, 'turma');  -- 1 convite para 20 pessoas
create or replace function public.gerar_convites(
  quantidade int default 1,
  usos_por_codigo int default 1,
  obs text default null,
  validade interval default null
)
returns setof text
language plpgsql security definer set search_path = public
as $$
declare
  v_cod text;
  i int;
  j int;
begin
  for i in 1..quantidade loop
    loop
      v_cod := 'PRO-';
      for j in 1..5 loop
        v_cod := v_cod || substr('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + floor(random() * 32)::int, 1);
      end loop;
      exit when not exists (select 1 from public.convites where codigo = v_cod);
    end loop;
    insert into public.convites (codigo, usos_max, observacao, expira_em)
    values (v_cod, usos_por_codigo, obs, case when validade is null then null else now() + validade end);
    return next v_cod;
  end loop;
end;
$$;
revoke all on function public.gerar_convites(int, int, text, interval) from public, anon, authenticated;

-- =====================================================================
-- COLINHA DE ADMINISTRAÇÃO
-- ---------------------------------------------------------------------
-- Criar convites ............ select * from gerar_convites(5);
-- Ver convites .............. select * from convites order by criado_em desc;
-- Desativar um convite ...... update convites set ativo = false where codigo = 'PRO-XXXXX';
-- Ver quem entrou ........... select email, status, convite, criado_em from membros order by criado_em desc;
-- Bloquear alguém ........... update membros set status = 'bloqueado' where email = 'pessoa@email.com';
-- Desbloquear ............... update membros set status = 'ativo'     where email = 'pessoa@email.com';
-- Remover de vez ............ Authentication → Users → apagar o usuário (a linha em membros some junto)
-- =====================================================================
