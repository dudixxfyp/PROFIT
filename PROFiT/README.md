# PROFiT

App Android de treino e dieta com widget na tela inicial. Para entrar, a pessoa precisa de um **código de convite**.

- **O que fica com você (no Supabase):** o e-mail de cada conta, o status (ativo ou bloqueado) e o convite que ela usou. Mais nada.
- **O que fica com cada pessoa (só no celular dela):** dieta, treinos, cargas, água e marcações. Esses dados nunca vão para o servidor. Se a pessoa tiver o backup do Android ligado, eles vão para a conta Google **dela**, não para você.
- **Sem internet:** o app continua funcionando por até 7 dias desde a última vez que confirmou o acesso. Se você bloquear alguém, isso vale assim que o celular da pessoa ficar online.

---

## 1. Supabase (uma vez só)

1. Crie um projeto em https://supabase.com.
2. Vá em **SQL Editor → New query**, cole o conteúdo de `supabase/schema.sql` e clique em **Run**.
3. Em **Authentication → Sign In / Providers → Email**, deixe o e-mail ativado. Recomendo **desligar "Confirm email"**: assim a pessoa entra na hora, e o convite já impede quem não foi chamado. Se deixar ligado, a pessoa confirma pelo link no e-mail e depois faz login.
4. Em **Settings → API**, copie a **Project URL** : https://bdottqsiaxvuiiysckdp.supabase.co/rest/v1/
 e a **anon public key** :sb_publishable_cKkh50gK_l_TTLnR3m91_g_jsyCWL-x :. Você vai precisar delas no passo 2.

### Administrar (pelo SQL Editor)

| Para…                     | Rode                                                                          |
|---------------------------|-------------------------------------------------------------------------------|
| Criar 5 convites          | `select * from gerar_convites(5);`                                            |
| 1 convite para 20 pessoas | `select * from gerar_convites(1, 20, 'turma');`                               |
| Convite que expira em 7 dias | `select * from gerar_convites(1, 1, 'Ana', interval '7 days');`            |
| Ver quem entrou           | `select email, status, convite, criado_em from membros;`                      |
| Bloquear                  | `update membros set status = 'bloqueado' where email = 'pessoa@email.com';`   |
| Desbloquear               | `update membros set status = 'ativo' where email = 'pessoa@email.com';`       |
| Cancelar um convite       | `update convites set ativo = false where codigo = 'PRO-XXXXX';`               |

Você também consegue ver e editar as tabelas `membros` e `convites` no **Table Editor**.

---

## 2. Site para baixar o app

O PROFiT ganha um site próprio, de graça, com um botão **Baixar o app**. As pessoas abrem o site (ou pesquisam no Google), tocam no botão e o APK baixa direto. Ninguém precisa de conta nem vê o GitHub: ele só compila o app e hospeda o site por trás.

O endereço do site fica assim (troque `seu-usuario` pelo seu usuário do GitHub):

```
https://seu-usuario.github.io/profit/
```

### 2.1 Preparar (uma vez só)

1. Crie um repositório **público** chamado `profit` no GitHub e envie esta pasta para ele. O site grátis do GitHub só funciona com repositório público.
   - O código não tem nenhum dado pessoal. A anon key do Supabase é pública por natureza: quem garante a segurança são as regras do banco.
2. Vá em **Settings → Secrets and variables → Actions → New repository secret** e cadastre os 6 secrets listados no arquivo `CHAVE-LEIA-ME.txt`, que vem separado deste projeto.
3. Vá em **Settings → Pages** e, em **Source**, escolha **GitHub Actions**.

### 2.2 Publicar uma versão

1. Crie uma tag. Pelo site: **Releases → Draft a new release → tag `v1.0` → Publish**. Pelo terminal:
   ```bash
   git tag v1.0 && git push origin v1.0
   ```
2. Em cerca de 5 minutos, a aba **Actions** termina. O app é compilado e o site é publicado com o `PROFiT.apk` novo.

Para atualizar o app, crie uma tag nova (`v1.1`, `v1.2`…). O site passa a baixar a versão nova e o endereço não muda. Como a chave de assinatura é sempre a mesma, a atualização instala por cima e mantém os dados das pessoas.

O texto do site fica em `site/index.html`. Se mudar alguma coisa nele, a mudança aparece na próxima versão publicada.

### 2.3 Aparecer nas pesquisas do Google

O site já tem título, descrição e `sitemap.xml` para o Google. Mesmo assim, um site novo pode levar algumas semanas para aparecer sozinho nas pesquisas. Para acelerar:

1. Entre em https://search.google.com/search-console com sua conta Google.
2. Clique em **Adicionar propriedade → Prefixo do URL** e cole o endereço do site.
3. Para comprovar que o site é seu, escolha **Tag HTML**, copie a linha `<meta name="google-site-verification" …>` e cole em `site/index.html`, logo abaixo da linha `<meta name="theme-color" …>`. Publique uma versão nova e clique em **Verificar**.
4. Em **Sitemaps**, envie `sitemap.xml`.

Enquanto o site não aparece no Google, mande o endereço direto para as pessoas. Ele funciona desde a primeira publicação.

---

## 3. Testar no seu celular (opcional, pelo Android Studio)

1. Preencha `app/src/main/assets/config.js` com a URL e a anon key.
2. Abra a pasta no Android Studio e clique em ▶ **Run** com o celular conectado.

Esse build usa a chave de debug. Antes de instalar a versão oficial (a do site), desinstale o de teste.

---

## 4. Como a pessoa usa

1. Abre o site no celular, toca em **Baixar o app** e instala o `PROFiT.apk`. Se o celular pedir, ela permite "instalar apps desconhecidos".
2. Abre o app, toca em **Tenho um convite** e digita o código, o e-mail e uma senha.
3. Monta o próprio plano na aba **✏️ Editar** (refeições, horários, treinos, meta de água).
4. Adiciona o widget: segura num espaço vazio da tela inicial → **Widgets** → **PROFiT**.

Em celulares Xiaomi ou Samsung, vale colocar o PROFiT como **sem restrição** na economia de bateria, para o widget não parar de atualizar.

---

## Estrutura

```
app/src/main/assets/index.html   painel, login e editor
app/src/main/assets/config.js    URL e anon key do Supabase
app/src/main/assets/supabase.js  cliente Supabase (v2.119.0)
app/src/main/java/.../PainelWidget.kt  widget
supabase/schema.sql              tabelas, regras e funções de convite
site/index.html                  site de download (o APK é colocado junto ao publicar)
.github/workflows/build.yml      build do APK e publicação do site
```
