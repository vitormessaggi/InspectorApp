# Checklist de implementação e entrega

Atualizado em 21/11/2026. A coluna de implementação descreve o código local. A validação externa registra o que ainda precisa ser confirmado antes de declarar a entrega concluída.

| ID | Implementação | Validação pendente |
|---|---|---|
| C1 | Modelos por tipo, cadastro de máquinas e vínculo reutilizável | Publicar regras e testar duas máquinas no mesmo modelo |
| C2 | Formulário dinâmico e respostas obrigatórias | Executar em dois aparelhos |
| C3 | Observação validada na tela, gravação e regras | Publicar regras e executar recusa pelo servidor |
| C4 | Bloqueio e liberação atômicos com justificativa | Testar perfis e concorrência no Firebase |
| C5 | Painel por máquina, filtro e virada do dia | Validar atualização entre aparelhos e fuso |
| C6 | Histórico com detalhes, operador e data | Validar registros reais e antigos |
| NC1 | Cadastro, login, sessão e saída | Confirmar as contas reais de demonstração |
| NC2 | Listeners nas listas principais | Validar atualização entre aparelhos |
| NC3 | Arquivo completo de regras e configuração de publicação | Publicar e validar no Firebase |
| NC4 | `usuarios/{uid}.perfil` com compatibilidade dos perfis anteriores | Definir perfis das contas novas no console |
| NC5 | RecyclerView e mensagens de estado vazio | Conferir tamanhos de tela usados na apresentação |
| NC6 | Validações em português | Executar critérios de aceite |
| NC7 | Estado dos campos e tarefas preservado nos formulários | Girar as telas com operações em andamento |
| NC8 | README e regra de exclusão do arquivo Firebase | Informar equipe, contas, prints e retirar arquivo já rastreado |
| ES1 | Histórias e critérios escritos | Validar com a equipe |
| ES2 | Histórias prontas para as issues | Criar quadro e atribuir responsáveis no GitHub |
| ES3 | Fluxo documentado | Branch, PR e revisão por outro integrante |
| QR | Leitura implementada | Testar em aparelho com Google Play Services |
| Indicador mensal | Cálculo por itens e setor implementado | Conferir contra dados do Firestore |
| Cloudinary | Adiado pelo responsável | Implementar em outra tarefa |

## Verificações realizadas

- Compilação de debug no Android Studio.
- Quatro testes JUnit de observação, situação da máquina e cálculo de conformidade.
- Verificação de espaços e conflitos por `git diff --check`.

As regras ainda não foram executadas no Emulator Suite nem confirmadas no Firebase remoto. A demonstração completa permanece pendente.

## Roteiro mínimo

1. No perfil Supervisor, publique um modelo Prensa hidráulica com proteção e emergência como críticos e limpeza como não crítico.
2. Cadastre PRE-001 e PRE-002 com o mesmo modelo e setor Prensas.
3. Mostre as duas máquinas como Sem inspeção.
4. No outro aparelho, entre como Operador, abra PRE-001, responda tudo Conforme e envie.
5. Mostre PRE-001 como Liberada no painel atualizado automaticamente.
6. Em PRE-002, marque um item crítico como Não conforme, deixe a observação vazia e tente enviar.
7. Registre a observação e envie. Mostre PRE-002 como Bloqueada.
8. No Supervisor, revise o bloqueio, confirme correção e segurança e registre a justificativa.
9. Mostre a liberação e abra o histórico com operador, horário e justificativa.
10. Demonstre uma tentativa de alteração sem permissão no simulador de regras do Firebase.
11. Mostre o quadro do GitHub Projects e um PR realmente revisado por outro integrante.

## Ensaios adicionais

- Repetir a recusa de observação vazia com item não crítico.
- Girar login, cadastro, modelo, inspeção e liberação durante o preenchimento.
- Interromper a conexão durante o envio e tentar novamente.
- Verificar a virada do dia com uma máquina liberada e outra bloqueada.
- Ler QR válido, desconhecido e cancelar a leitura.
- Consultar mês vazio, mês inválido e setor com múltiplas inspeções.

## Materiais acadêmicos

| Entregável | Situação |
|---|---|
| Código e README | Preparados localmente |
| APK de debug | Gerado pelo Android Studio |
| Slides em PDF | Dependem dos prints finais e dos dados da equipe |
| Vídeo da demonstração | Depende da execução e gravação do roteiro com os dois perfis |
| Checklist CORE e NC | Este documento |

O PDF apresenta tempos divergentes para a apresentação. Confirme o tempo com o professor e use no máximo seis slides. A sequência sugerida é problema, perfis, demonstração, dados e regras, organização da equipe e limitações/próximos passos.
