-- =====================================================================
-- PROFiT — contas, aceite dos termos (LGPD) e bloqueio
-- Rode este arquivo inteiro no Supabase: SQL Editor → New query → Run.
-- Pode rodar de novo sem problema (também troca a versão antiga, de convites).
--
-- Qualquer pessoa pode criar conta com e-mail e senha. Deixe "Confirm email"
-- LIGADO no Supabase: só entra quem confirmar que o e-mail é dela.
-- Para usar o app, a pessoa precisa aceitar os Termos de Uso e a Política
-- de Privacidade. O aceite fica registrado aqui (data e versão), como prova.
--
-- O banco guarda SÓ: e-mail, status (ativo/bloqueado) e o registro do aceite.
-- Dieta, treinos, cargas e marcações ficam no celular de cada pessoa e
-- nunca são enviados.
-- =====================================================================

-- ---------- Tabela ----------
create table if not exists public.contas (
  user_id            uuid primary key references auth.users(id) on delete cascade,
  email              text,
  status             text not null default 'ativo' check (status in ('ativo','bloqueado')),
  termos_versao      text,
  termos_aceitos_em  timestamptz,
  criado_em          timestamptz not null default now()
);

-- ---------- Troca das versões antigas (convites / lista de e-mails) ----------
-- Quem já tinha conta continua com o mesmo status; só vai precisar aceitar os termos.
do $$
begin
  if to_regclass('public.membros') is not null then
    insert into public.contas (user_id, email, status)
    select user_id, email, status from public.membros
    on conflict (user_id) do nothing;
  end if;
end $$;
drop function if exists public.validar_convite(text);
drop function if exists public.resgatar_convite(text);
drop function if exists public.gerar_convites(int, int, text, interval);
drop function if exists public.email_liberado(text);
drop function if exists public.meu_acesso();
drop table if exists public.membros;
drop table if exists public.convites;
drop table if exists public.liberados;
drop function if exists public.liberados_normaliza();

-- ---------- Segurança (RLS) ----------
-- Ninguém pelo app lê ou escreve a tabela diretamente (só pelas funções abaixo).
alter table public.contas enable row level security;
revoke all on public.contas from anon, authenticated;

-- ---------- Funções usadas pelo app ----------

-- Situação da conta logada, para a versão dos termos que o app mostra:
-- 'ativo' | 'bloqueado' | 'sem_aceite' | 'nao_confirmado'
create or replace function public.meu_acesso(p_versao text)
returns text
language plpgsql stable security definer set search_path = public
as $$
declare
  v_conf   timestamptz;
  v_status text;
  v_versao text;
begin
  select email_confirmed_at into v_conf from auth.users where id = auth.uid();
  if v_conf is null then
    return 'nao_confirmado';
  end if;
  select status, termos_versao into v_status, v_versao from public.contas where user_id = auth.uid();
  if v_status = 'bloqueado' then
    return 'bloqueado';
  end if;
  if v_versao is distinct from p_versao then
    return 'sem_aceite';
  end if;
  return 'ativo';
end;
$$;

-- Registra que a pessoa logada aceitou os termos (data, hora e versão).
create or replace function public.aceitar_termos(p_versao text)
returns text
language plpgsql security definer set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'não autenticado';
  end if;
  insert into public.contas (user_id, email, termos_versao, termos_aceitos_em)
  values (auth.uid(), auth.jwt() ->> 'email', p_versao, now())
  on conflict (user_id) do update
    set termos_versao = excluded.termos_versao,
        termos_aceitos_em = excluded.termos_aceitos_em,
        email = excluded.email;
  return public.meu_acesso(p_versao);
end;
$$;

-- A pessoa apaga a própria conta (direito de exclusão da LGPD, art. 18).
create or replace function public.excluir_minha_conta()
returns void
language plpgsql security definer set search_path = public, auth
as $$
begin
  if auth.uid() is null then
    raise exception 'não autenticado';
  end if;
  delete from auth.users where id = auth.uid();   -- a linha em contas some junto
end;
$$;

revoke all on function public.meu_acesso(text)       from public;
revoke all on function public.aceitar_termos(text)   from public;
revoke all on function public.excluir_minha_conta()  from public;
grant execute on function public.meu_acesso(text)      to authenticated;
grant execute on function public.aceitar_termos(text)  to authenticated;
grant execute on function public.excluir_minha_conta() to authenticated;

-- =====================================================================
-- COLINHA DE ADMINISTRAÇÃO
-- ---------------------------------------------------------------------
-- Ver contas e aceites ...... select email, status, termos_versao, termos_aceitos_em, criado_em from contas order by criado_em desc;
-- Bloquear alguém ........... update contas set status = 'bloqueado' where email = 'pessoa@email.com';
-- Desbloquear ............... update contas set status = 'ativo'     where email = 'pessoa@email.com';
-- Apagar uma conta .......... Authentication → Users → apagar o usuário (a linha em contas some junto)
-- =====================================================================
