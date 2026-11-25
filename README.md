# Inspecta — Checklist Pré-Turno

Aplicativo Android em Java para registrar inspeções antes da operação de máquinas. O operador responde ao checklist e descreve não conformidades. Uma falha crítica bloqueia a máquina até que o supervisor registre a liberação justificada.

## Equipe

Vitor Messaggi é o integrante identificado no repositório. Os demais nomes do grupo ainda precisam ser informados.

## Recursos

- Modelos de checklist por tipo de máquina, com itens críticos e não críticos, rascunho, publicação e versões anteriores.
- Cadastro de máquinas com código, nome, setor e vínculo a um modelo compartilhado.
- Formulário dinâmico, observação obrigatória para não conformidades e bloqueio persistente por falha crítica.
- Painel diário com Liberada, Bloqueada e Sem inspeção, atualizado em tempo real e com filtro por situação.
- Histórico com operador, data, respostas, observação e justificativa de liberação.
- Cadastro e login por e-mail e senha, sessão persistente, recuperação de senha e saída.
- Leitura de QR pelo Google Code Scanner. O conteúdo do QR deve ser o código exato da máquina cadastrada.
- Percentual de itens conformes por setor e mês.

## Fotos de não conformidades

O operador deve tirar uma foto com a câmera para enviar cada inspeção. O aplicativo envia a imagem em JPEG e reduz fotos maiores que 10 MB antes do upload. A foto fica disponível no histórico e na revisão do supervisor. O Cloudinary entrega a imagem por um link acessível a quem o possui.

Crie um preset de upload *Unsigned* para imagens no Cloudinary, com os formatos permitidos e a opção de impedir IDs públicos definidos pelo cliente. Informe o *Cloud name* e o nome do preset nas propriedades do Gradle:

    cloudinaryCloudName=nome-da-conta
    cloudinaryUploadPreset=nome-do-preset

Para configurar apenas no seu computador, use o arquivo gradle.properties no diretório pessoal do Gradle. Sem esses valores, o aplicativo bloqueia o envio de inspeções. Publique as regras atualizadas de firestore.checklists.rules antes de usar esta versão com o Firebase do projeto. O requisito vale para novos registros e as inspeções antigas continuam disponíveis no histórico.

## Abrir e compilar

1. Abra a pasta `2. APP/Inspecta` no Android Studio.
2. Use JDK 25, conforme `gradle/gradle-daemon-jvm.properties`, e instale o SDK Android 36.
3. Sincronize o Gradle. O leitor de QR requer Google Play Services e pode baixar o módulo na primeira utilização.
4. Execute em Android 13 ou superior, conforme `minSdk 33`.

Na pasta do aplicativo, os comandos são `gradlew.bat :app:assembleDebug` e `gradlew.bat :app:testDebugUnitTest`.
O APK gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Configurar Firebase

1. No projeto Firebase da equipe, registre um aplicativo Android com o pacote `com.example.inspecta`.
2. Baixe o arquivo de configuração e coloque em `2. APP/Inspecta/app/google-services.json`.
3. Ative o provedor E-mail/senha no Firebase Authentication e crie o banco Cloud Firestore.
4. Publique o conteúdo completo de `2. APP/Inspecta/firestore.checklists.rules` na aba Regras. Esse arquivo contém o serviço completo e é associado por `firebase.json`.
5. Faça o cadastro das contas pelo aplicativo. Cada conta nova recebe `perfil = Pendente`.
6. No console, defina `usuarios/{uid}.perfil` como `Operador` ou `Supervisor`. Somente o console administrativo define o perfil.
7. Informe `nome` no mesmo documento. Teste os dois perfis em aparelhos ou emuladores separados.

Com a CLI do Firebase autenticada, a publicação pode ser feita por `firebase deploy --only firestore:rules --project ID_DO_PROJETO`, dentro de `2. APP/Inspecta`.
A publicação das novas regras ainda precisa ser confirmada no ambiente da equipe.

### Contas de demonstração

| Perfil | Situação |
|---|---|
| Operador | E-mail de teste ainda não informado |
| Supervisor | E-mail de teste ainda não informado |

As contas reais e as respectivas credenciais de teste devem ser definidas pela equipe antes da apresentação.

### Arquivo de configuração no Git

O `.gitignore` já inclui `app/google-services.json`. O arquivo continua rastreado no checkout atual porque a sessão de implementação não possui permissão de escrita na pasta `.git`.
Para cumprir NC8, execute `git rm --cached -- "2. APP/Inspecta/app/google-services.json"` na raiz do repositório e inclua essa remoção no próximo commit. O comando mantém a cópia local usada pelo Android Studio.

## Organização dos dados

| Coleção | Conteúdo |
|---|---|
| `usuarios/{uid}` | Nome e perfil definido administrativamente |
| `users/{uid}` | Perfis anteriores, lidos por compatibilidade quando não existe perfil em `usuarios` |
| `checklists/{id}` | Modelo, tipo de máquina, itens, índices críticos e estado de publicação |
| `checklists/{id}/versoes/{id}` | Cópia do modelo antes de uma edição publicada |
| `maquinas/{id}` | Código, nome, setor, tipo e identificador do modelo |
| `inspecoes/{id}` | Máquina, operador, data, respostas, itens do momento da inspeção, falhas e liberação |
| `bloqueiosMaquinas/{id}` | Referência à inspeção crítica que mantém a máquina bloqueada |

Os checklists anteriores continuam representando sua máquina original. Novas máquinas reutilizam um modelo pelo campo `checklistId`. Se um modelo for arquivado, as máquinas cadastradas continuam no catálogo, com o formulário indisponível. Bloqueios antigos continuam no painel até a revisão.

Inspeções antigas preservam os dados disponíveis. Elas não possuíam a lista completa de respostas, por isso o histórico identifica essa limitação e o indicador mensal informa quantas foram excluídas do cálculo.

## Regras de negócio

- Qualquer não conformidade exige observação preenchida. Espaços em branco não atendem à validação.
- Falha crítica abre a inspeção e o bloqueio na mesma transação.
- A liberação exige supervisor, justificativa, confirmação da correção e confirmação da segurança. A inspeção é atualizada e o bloqueio é removido na mesma transação.
- Um bloqueio pendente tem prioridade sobre o registro de inspeção no dia e persiste na virada da data.
- Sem bloqueio, uma máquina inspecionada no dia fica Liberada. Ao iniciar um novo dia sem inspeção, fica Sem inspeção.
- O dia e o mês usam o fuso do aparelho. Os aparelhos de demonstração devem usar o mesmo fuso.
- O indicador é `itens conformes / itens respondidos × 100`, por setor no mês escolhido. Todas as inspeções do período entram no cálculo quando possuem respostas completas.

## Validação e entrega

As histórias com critérios de aceite estão em `1.Documents/Historias_de_usuario.md`.
O roteiro e o checklist da entrega estão em `1.Documents/Checklist_entrega.md`.
Os testes automatizados de regras de negócio estão em `RegrasInspecaoTest.java`.

A compilação de debug foi verificada no Android Studio. Os quatro testes de lógica passaram. A publicação das regras, a execução dos critérios em dois aparelhos, os prints finais, a apresentação e o vídeo ainda precisam de validação no ambiente da equipe.

## Trabalho em equipe

Cada história deve ter uma issue com responsável no GitHub Projects, nas colunas A fazer, Fazendo e Feito. Uma branch por história deve entrar por PR com referência à issue e aprovação de outro integrante. A sessão atual não alterou branches nem o índice Git devido à restrição de escrita em `.git`. Revisões e aprovações precisam ser realizadas pelos integrantes.

## Referências técnicas

- [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner)
- [Condições das regras do Firestore](https://firebase.google.com/docs/firestore/security/rules-conditions)
