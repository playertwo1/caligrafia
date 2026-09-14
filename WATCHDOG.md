# WATCHDOG — Guardrails de Alto Risco

Este documento é carregado **por gatilho**, não como contexto obrigatório de toda tarefa.

## Quando carregar
Use WATCHDOG quando a tarefa envolver qualquer um destes casos:
- persistência, migração, backup, restore ou deleção;
- dados reais do usuário;
- credenciais, secrets, assinatura ou release;
- permissões, arquivos externos ou compartilhamento;
- mudanças arquiteturais ou dependências amplas;
- ações destrutivas, force push ou reescrita de histórico;
- hardware/S Pen/Watch quando a conclusão depender de capacidade física.

## Guardrails críticos
1. **Preservar dados:** raw strokes e acervo real não podem ser substituídos por bitmap, seed, mock ou defaults artificiais.
2. **Falha recuperável:** migração/restore deve preservar inventário anterior quando não puder concluir com segurança.
3. **Sensores honestos:** zero físico é diferente de ausência de capability; não inventar pressure/tilt/orientation.
4. **Segredos fora do Git:** nenhum token, senha, keystore, chave privada ou material equivalente em código/log/artifact.
5. **Menor privilégio:** permissões/dependências somente quando necessárias.
6. **Arquitetura:** sem WebView para canvas; domínio desacoplado de SDK proprietário por adapters.
7. **Local-first:** backend/cloud/IA nova requer autorização explícita e não substitui o núcleo offline.
8. **Evidência real:** teste isolado não substitui ligação do chamador real; JVM não comprova requisito físico; debug build não comprova release assinada.
9. **Git seguro:** confirmar branch/SHA/diff antes de operação destrutiva; preservar trabalho existente.
10. **Sem autoaprovação:** Builder não fecha gate material; `NOT_RUN != PASS`.

## Mudança mínima
- preferir mudanças pequenas, reversíveis e com rollback claro;
- não misturar limpeza/refatoração ampla com correção funcional sem necessidade;
- após ~3 falhas semelhantes, reavaliar hipótese/causa antes de repetir tentativa;
- não relaxar testes para normalizar bug.

## Dados e concorrência
Quando houver escrita em arquivo/conjunto:
- distinguir atomicidade de arquivo, transação de conjunto, durabilidade e ordem de escrita;
- testar/revisar falha parcial e reabertura;
- mutex por instância não prova coordenação entre clientes diferentes;
- caminhos reais de produtor e consumidor devem coincidir.

## Segurança de release
Release deve:
- falhar se qualquer material de assinatura obrigatório estiver ausente;
- verificar assinatura do APK produzido;
- registrar hash do artefato;
- nunca publicar debug/unsigned como release válida.

## Stop conditions
Pare a execução e registre finding quando:
- ação necessária é irreversível sem recuperação demonstrada;
- há risco de perda de dados/chave;
- requisito contradiz decisão LOCKED sem autorização;
- evidência exigida depende de hardware indisponível;
- não é possível distinguir baseline de regressão.

Continue trabalhos reversíveis independentes sem transformar pendência em PASS.