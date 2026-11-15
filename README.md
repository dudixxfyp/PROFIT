# PROFiT

App Android de treino e dieta com widget na tela inicial. Qualquer pessoa pode baixar e criar conta com **e-mail e senha**. Para entrar, ela precisa **confirmar o e-mail** e **aceitar os Termos de Uso e a Política de Privacidade** (LGPD).

- **O que fica com você (no Supabase):** o e-mail de cada conta, o status (ativo ou bloqueado) e o registro do aceite dos termos (data, hora e versão). Mais nada.
- **O que fica com cada pessoa (só no celular dela):** dieta, treinos, cargas, água e marcações. Esses dados nunca vão para o servidor. Se a pessoa tiver o backup do Android ligado, eles vão para a conta Google **dela**, não para você.
- **Sem internet:** o app continua funcionando por até 7 dias desde a última vez que confirmou o acesso. Se você bloquear alguém, isso vale assim que o celular da pessoa ficar online.

---

## 1. Supabase (uma vez só)

1. Crie um projeto em https://supabase.com.
2. Vá em **SQL Editor → New query**, cole o conteúdo de `supabase/schema.sql` e clique em **Run**.
3. Em **Authentication → Sign In / Providers → Email**, deixe o e-mail ativado e **"Confirm email" LIGADO**. É isso que garante que o e-mail é mesmo da pessoa: ela só entra depois de tocar no link que chega na caixa de entrada.
   - Em **Authentication → URL Configuration → Site URL**, coloque o endereço do site do app (passo 2). É para lá que o link de confirmação leva depois de confirmar.
4. Em **Settings → API**, copie a **Project URL** : https://bdottqsiaxvuiiysckdp.supabase.co/rest/v1/
 e a **anon public key** :sb_publishable_cKkh50gK_l_TTLnR3m91_g_jsyCWL-x :. Você vai precisar delas no passo 2.

### Administrar (pelo SQL Editor)

| Para…                        | Rode                                                                                             |
|------------------------------|--------------------------------------------------------------------------------------------------|
| Ver contas e aceites         | `select email, status, termos_versao, termos_aceitos_em, criado_em from contas;`                 |
| Bloquear                     | `update contas set status = 'bloqueado' where email = 'pessoa@email.com';`                       |
| Desbloquear                  | `update contas set status = 'ativo' where email = 'pessoa@email.com';`                           |
| Apagar uma conta             | **Authentication → Users** → apagar o usuário                                                    |

Você também consegue ver e editar a tabela `contas` no **Table Editor**. Se você já tinha rodado a versão antiga (de convites), é só rodar o `schema.sql` novo: quem já tinha conta continua, e só vai precisar aceitar os termos.

### Termos de Uso e Política de Privacidade (LGPD)

O texto fica em `app/src/main/assets/termos.html`. O mesmo arquivo aparece dentro do app e no site (`privacidade.html`).

1. **Antes de publicar**, abra o arquivo e troque `[SEU NOME]` e `[SEU E-MAIL DE CONTATO]` pelos seus dados. A LGPD exige que a pessoa saiba quem é a responsável e como falar com ela.
2. No cadastro, a pessoa só cria a conta se marcar que leu e aceita os termos. O servidor guarda a data, a hora e a versão do aceite: é a sua prova.
3. A pessoa pode apagar a própria conta em **✏️ Editar → Conta → Excluir minha conta**. Isso apaga a conta do servidor e os dados do celular (direito de exclusão da LGPD).
4. Se mudar algo importante nos termos, troque a versão no topo de `termos.html` e em `TERMOS_VERSAO` no `index.html`. Na próxima vez que abrir o app, todo mundo vai precisar aceitar de novo.

O texto foi escrito para cobrir o que a LGPD pede de um app desse tamanho, mas não substitui um advogado. Se o app crescer ou passar a cobrar, vale pedir uma revisão.

---

## 2. Site para baixar o app

O PROFiT ganha um site próprio, de graça, com um botão **Baixar o app**. As pessoas abrem o site (ou pesquisam no Google), tocam no botão e o APK baixa direto. Ninguém precisa de conta nem vê o GitHub: ele só compila o app e hospeda o site por trás.

O endereço do site fica assim (troque `seu-usuario` pelo seu usuário do GitHub):

```
https://github.com/dudixxfyp/PROFIT.git
```

### 2.1 Preparar (uma vez só)

1. Crie um repositório **público** chamado `profit` no GitHub e envie esta pasta para ele. O site grátis do GitHub só funciona com repositório público.
   - O código não tem nenhum dado pessoal. A anon key do Supabase é pública por natureza: quem garante a segurança são as regras do banco.
2. Vá em **Settings → Secrets and variables → Actions → New repository secret** e cadastre os 6 secrets listados no arquivo `CHAVE-LEIA-ME.txt`. Ele fica **fora** deste projeto, em `C:\Users\mariavie\PROFIT-CHAVES\`, junto com a chave de assinatura `profit.jks`. Nunca coloque essa pasta no GitHub.
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
2. Abre o app, toca em **Criar conta**, digita o e-mail e uma senha e marca que aceita os termos.
3. Confirma pelo link que chega no e-mail, volta ao app e toca em **Entrar**.
4. Monta o próprio plano na aba **✏️ Editar** (refeições, horários, treinos, meta de água).
5. Adiciona o widget: segura num espaço vazio da tela inicial → **Widgets** → **PROFiT**.

Em celulares Xiaomi ou Samsung, vale colocar o PROFiT como **sem restrição** na economia de bateria, para o widget não parar de atualizar.

---

## Estrutura

```
app/src/main/assets/index.html   painel, login e editor
app/src/main/assets/config.js    URL e anon key do Supabase
app/src/main/assets/supabase.js  cliente Supabase (v2.119.0)
app/src/main/java/.../PainelWidget.kt  widget
app/src/main/assets/termos.html  Termos de Uso e Política de Privacidade (também vira privacidade.html no site)
supabase/schema.sql              tabela de contas, aceite dos termos, bloqueio e exclusão
site/index.html                  site de download (o APK é colocado junto ao publicar)
.github/workflows/build.yml      build do APK e publicação do site
```
