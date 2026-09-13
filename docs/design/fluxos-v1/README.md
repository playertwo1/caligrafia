# Scribe — 12 referências de fluxos para o Antigravity

Propostas visuais solicitadas pelo usuário em 2026-09-12. São 12 imagens distintas, cada uma com três estados de interface. Complementam o [painel geral](../scribe-visao-12-telas-v1.png).

As imagens orientam composição, hierarquia, controles e estados. Não são screenshots da implementação nem aprovação dos gates. **Atualização de 2026-09-13:** o usuário autorizou implementar as áreas e opções como funcionalidades reais, conforme [roadmap funcional](../../ANTIGRAVITY_FUNCTIONAL_ROADMAP.md), ondas F0–F7. O painel geral tem 12 áreas do produto; estas 12 pranchas detalham fluxos complementares, não são uma lista substituta. Continuam válidos os requisitos de integridade e comprovação de [AGENTS](../../../AGENTS.md).

| Imagem | Fluxo | Dependências da auditoria |
|---|---|---|
| [01 — Biblioteca](01-biblioteca-cadernos.png) | Vazio → criar → listar cadernos | Persistência/estado R19 |
| [02 — Páginas](02-organizacao-paginas.png) | Grade → opções → confirmar exclusão | Preservação de dados e ordenação |
| [03 — Ferramentas](03-ferramentas-escrita.png) | Caneta → borracha por stroke → guias | Captura/estilos R15/S13/S20 |
| [04 — Sessão](04-sessao-ativa.png) | Praticar → pausar → resumo | Timer R01/R08 e escrita real R02 |
| [05 — Feedback](05-feedback-geometrico.png) | Evidência → detalhe → nova tentativa | Geometria R14/S09 |
| [06 — Replay](06-replay-comparacao.png) | Individual → antes/depois → sobreposição | Tempo/escala R12/R13 |
| [07 — Variantes](07-variantes-alfabeto.png) | Histórico → escolher tentativa → favorita | Integração R03/R04/R10 |
| [08 — Estilo pessoal](08-estilo-pessoal.png) | Sem dados → variantes → usar referência | Registro/métricas R05/R11 |
| [09 — Coaching](09-coaching-explicavel.png) | Observação → prescrição → treino correto | Dados/IDs S07–S10 |
| [10 — Restaurar](10-restauracao-backup.png) | Selecionar → revisar → exemplo de erro | S01–S06; corrigir integridade antes de expor restore |
| [11 — Exportar](11-exportacao-arquivos.png) | Página → assinatura → destino local | Escala/arquivo R16/S15 |
| [12 — Preferências](12-preferencias-acessibilidade.png) | Escrita → leitura → lembretes | S18/S19; opções adicionais são propostas futuras |

## Como usar sem copiar inconsistências

1. Padronizar navegação: proposta geral Caderno / Praticar / Evolução / Mais. Algumas imagens variam os rótulos ou incluem Perfil/Explorar; não criar conta, perfil ou novas rotas por causa dessas variações geradas.
2. Preservar as cinco fases pedagógicas definidas no produto. A imagem 04 simplifica para quatro etapas visuais e não redefine o timer.
3. A imagem 03 mostra Copperplate com 55° ilustrativos: usar o valor canônico de 52° e sua convenção geométrica correta, não copiar esse número.
4. Na imagem 05, o destaque indica ultrapassagem da linha superior, mas um título diz linha de base. Corrigir a copy para a linha efetivamente medida. Os desenhos são ilustrações, não ground truth de avaliação.
5. Na imagem 09, “70% da tela” é um erro de texto gerado: significa opacidade de 70% do modelo, não ocupação da tela. Ghost não altera dados brutos.
6. Na imagem 11, substituir “assinatura digital” por “imagem da assinatura”. SVG/PNG não são assinatura criptográfica nem prova de autenticidade.
7. Estado de erro da imagem 10 é um cenário alternativo, não sequência obrigatória após um arquivo validado. Só afirmar dados inalterados após comprovação de recuperação/transação. Não substituir pastas como atalho.
8. Datas, contagens, notas e exemplos manuscritos são fictícios para layout. Nunca transformá-los em seeds de progresso pessoal. Loading, erro, ausência e insuficiência de dados são estados distintos.
9. Duplicação, busca e preferências de barra/mão/texto/contraste/animação/lembretes foram incorporadas ao plano funcional autorizado. Implementar com persistência e aceite, na onda prevista. Capas continuam decorativas; não acrescentar gerador ou catálogo comercial. Elementos incidentais não enumerados continuam fora do escopo.
10. Priorizar o espaço de escrita no aparelho: cabeçalhos e slogans grandes nestas pranchas devem ser reduzidos na UI de produção. Garantir alvos de toque, texto escalável, contraste e área livre para a palma.
11. Usar componentes nativos Android para selecionar documentos/destinos. O seletor desenhado é ilustrativo; não construir um gerenciador de arquivos próprio por causa dele.
12. Toda ação visível precisa de handler, estado, persistência e teste do percurso real. Imagem aprovada não equivale a funcionalidade implementada. Implementar apenas dentro da onda correspondente e registrar evidência por ID.

## Entrega

Entrega original: PNGs e índice. Próxima entrega autorizada: percursos reais do aplicativo, pelo plano funcional; atualização deste guia não equivale a implementação.
