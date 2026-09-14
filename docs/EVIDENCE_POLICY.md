# Evidence Policy — Scribe / Caligrafia

## Objetivo

Manter no Git apenas a evidência que ajuda uma pessoa ou IA a entender decisões, reproduzir validações e auditar gates, sem transformar o repositório em depósito de logs, XMLs e outputs regeneráveis.

## Fonte canônica

A evidência ativa deve registrar, quando aplicável:

- SHA/branch avaliados;
- comando executado;
- ambiente relevante;
- resultado e exit code;
- resumo quantitativo;
- limitações e validações não executadas;
- hashes/certificados quando necessários;
- conclusão/auditoria em Markdown ou JSON compacto.

`NOT_RUN != PASS`. Existência de arquivo não prova que uma validação ocorreu.

## KEEP no repositório

Preservar quando agregam informação não trivial ou permitem reprodução:

- matrizes e relatórios de auditoria em Markdown;
- summaries compactos JSON/TXT com resultados consolidados;
- hashes de APK/artefatos e metadados de ambiente relevantes;
- scripts, probes e testes que reproduzem a validação;
- fixtures/baselines compactos que não sejam mero dump de runner;
- evidência física ou de dispositivo que não possa ser reproduzida automaticamente;
- decisões/findings históricos que expliquem por que um gate permaneceu aberto ou foi aprovado.

## NÃO VERSIONAR COMO EVIDÊNCIA ATIVA

Por padrão, resultados reproduzíveis devem ser artifacts de CI ou saídas locais ignoradas:

- `*.log` de build/test/lint;
- `TEST-*.xml` e XMLs JUnit;
- `lint-results*.xml` e dumps equivalentes;
- diretórios de XML baseline copiados de runners;
- `delta-numstat.txt`, diffs e stats regeneráveis a partir de SHAs conhecidos;
- outputs de build e APKs intermediários;
- dumps repetidos quando existe summary canônico.

Exceção: um output bruto pode permanecer quando for a única evidência preservável de um evento não reproduzível. A exceção deve ser justificada no relatório que o referencia.

## Histórico V2–V5

A limpeza do Ideias Standard remove do **tree ativo** outputs brutos reproduzíveis das auditorias V2–V5. Isso não apaga a história: os arquivos continuam recuperáveis no histórico Git e na baseline anterior à limpeza.

Baseline histórica de referência para os artefatos removidos nesta adoção:

`cdb0fd47704edb5c0903626fb53d36cca05616b1` (`phase-f6-f7-finalization`)

Matrizes históricas podem mencionar nomes de outputs que não ficam mais no tree atual. Para investigação forense, consultar o SHA histórico acima ou regenerar a evidência pelos scripts/testes preservados.

## CI

O fluxo normal deve:

1. executar testes/lint/build aplicáveis;
2. produzir summaries legíveis;
3. publicar XML/logs/outputs brutos como GitHub Actions artifacts com retenção definida;
4. manter no Git somente contratos, scripts, summaries e decisões que precisam sobreviver ao artifact temporário.

## Segurança

Nunca persistir em evidência:

- senhas;
- tokens;
- secrets de CI;
- keystores/chaves privadas;
- dumps contendo dados pessoais ou material sensível não necessário.

Encontrou segredo em evidência: interromper a publicação, remover do tree ativo, registrar incidente sem repetir o valor e rotacionar a credencial quando aplicável.

## Regra de limpeza

Antes de remover um artefato, classificá-lo como:

- `KEEP` — canônico, não trivial ou não reproduzível;
- `CONSOLIDATE/ARCHIVE` — útil historicamente, mas não deve participar do contexto padrão;
- `REMOVE_FROM_ACTIVE_TREE` — bruto, duplicado ou reproduzível.

Na dúvida, preservar e registrar finding. Economia de espaço/contexto nunca supera integridade da evidência.