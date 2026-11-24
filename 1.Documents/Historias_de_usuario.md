# Histórias de usuário

## C1 — Modelos reutilizáveis

Como supervisor, quero cadastrar modelos por tipo de máquina para aplicar as mesmas verificações a máquinas equivalentes.

- Dado um modelo com nome, tipo e itens, quando publico o modelo, então ele fica disponível no cadastro das máquinas.
- Dadas duas máquinas vinculadas ao mesmo modelo, quando abro suas inspeções, então ambas exibem os itens e a criticidade definidos no modelo.
- Dado um modelo publicado, quando altero seus itens, então a versão anterior é arquivada e as inspeções enviadas mantêm suas respostas originais.

## C2 — Preenchimento da inspeção

Como operador, quero selecionar a máquina e responder aos itens do modelo para registrar sua condição antes do turno.

- Dada uma máquina com modelo publicado, quando a seleciono, então vejo os itens e as alternativas Conforme e Não conforme.
- Dado um formulário com item sem resposta, quando tento concluir, então o envio permanece desabilitado.
- Dado um formulário preenchido parcialmente, quando giro a tela, então as respostas e a observação são preservadas.

## C3 — Observação da não conformidade

Como supervisor, quero que toda não conformidade tenha observação para entender o problema encontrado pelo operador.

- Dado qualquer item não conforme, quando envio sem observação ou somente com espaços, então o app recusa e indica o campo obrigatório.
- Dada uma não conformidade com descrição preenchida, quando envio a inspeção completa, então a observação fica salva junto ao registro.
- Dada uma tentativa direta de gravação sem observação com falhas, quando as regras do Firestore são avaliadas, então a gravação é negada.

## C4 — Bloqueio e liberação

Como supervisor, quero revisar uma falha crítica e justificar a liberação para controlar a retomada da máquina.

- Dado um item crítico não conforme com observação, quando o operador envia, então a máquina fica Bloqueada e não aceita nova inspeção até a revisão.
- Dado um bloqueio, quando o operador tenta removê-lo, então as regras negam a gravação.
- Dado um supervisor com justificativa e as duas confirmações, quando libera a máquina, então o bloqueio é removido e a autoria e a data da liberação ficam no histórico.
- Dados dois supervisores revisando o mesmo bloqueio, quando o primeiro libera, então o segundo recebe o estado atualizado ou a indicação de conflito.

## C5 — Painel diário

Como supervisor, quero acompanhar a situação diária de todas as máquinas para identificar bloqueios e inspeções pendentes.

- Dada uma máquina cadastrada sem inspeção no dia e sem bloqueio, quando abro o painel, então ela aparece como Sem inspeção.
- Dada uma inspeção sem falhas críticas enviada em outro aparelho, quando o Firestore atualiza, então a máquina aparece Liberada sem recarregar.
- Dado um bloqueio aberto no dia anterior, quando muda a data, então a máquina continua Bloqueada.
- Dada uma máquina liberada no dia anterior, quando muda a data sem nova inspeção, então ela aparece Sem inspeção.

## C6 — Histórico

Como supervisor, quero consultar as inspeções de uma máquina para identificar quem respondeu, quando e quais problemas foram registrados.

- Dada uma máquina com inspeções, quando abro seu histórico, então vejo seus registros ordenados do mais recente para o mais antigo, com operador e horário.
- Dada uma inspeção nova, quando abro os detalhes, então vejo as respostas e os textos dos itens como estavam no envio.
- Dada uma máquina sem registros, quando abro o histórico, então vejo uma mensagem de estado vazio.

## Complementar — QR

Como operador, quero ler o QR da máquina para abrir seu formulário diretamente.

- Dado um QR contendo o código de uma máquina cadastrada, quando faço a leitura, então o aplicativo abre a inspeção correspondente.
- Dado um QR desconhecido, quando faço a leitura, então recebo uma mensagem e posso selecionar a máquina pela lista.

## Complementar — Indicador mensal

Como supervisor, quero consultar a conformidade mensal por setor para acompanhar a proporção de itens conformes.

- Dados três itens conformes em quatro respostas no mês e setor, quando consulto o indicador, então vejo 75%.
- Dado um mês sem respostas completas, quando consulto, então vejo uma mensagem de estado vazio.
- Dados registros antigos sem respostas completas, quando consulto, então vejo a quantidade de registros excluídos do cálculo.

## Quadro do GitHub Projects

Crie uma issue para cada história acima e associe o responsável real. Use A fazer, Fazendo e Feito. Mova para Feito após executar todos os critérios no aplicativo e no Firestore. Inclua `Closes #numero` no PR e solicite a revisão de outro integrante.
