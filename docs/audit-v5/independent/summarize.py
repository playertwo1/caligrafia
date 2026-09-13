"""Rebuild evidence index and V5 matrix without changing submitted evidence."""
from pathlib import Path
import subprocess, json, xml.etree.ElementTree as ET
from collections import Counter

root = Path(__file__).resolve().parents[3]
out = Path(__file__).resolve().parent
suites = [ET.parse(p).getroot() for p in sorted((out / 'baseline-xml').glob('TEST-*.xml'))]
extra = ET.parse(out / 'additional.xml').getroot()
summary = {
    'sha': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip(),
    'baseline': dict(classes=len(suites), **{k: sum(int(s.get(k, 0)) for s in suites) for k in ['tests','failures','errors','skipped']}),
    'adversarial_v4': [dict(name=t.get('name'), status='PASS' if t.find('failure') is None else 'FAIL') for s in suites if s.get('name','').endswith('AuditV4IndependentTest') for t in s.findall('testcase')],
    'additional_v5': [dict(name=t.get('name'), status='FAIL' if t.find('failure') is not None else 'PASS', message=t.find('failure').get('message') if t.find('failure') is not None else '') for t in extra.findall('testcase')],
    'lint': dict(Counter(i.get('severity') for i in ET.parse(out / 'lint-results-debug.xml').getroot().findall('issue'))),
    'runner_exit_code': 0, 'additional_exit_code': 1,
    'device': 'emulator-5554 available; no physical S25 Ultra; no instrumented tests run',
}
(out / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
original = subprocess.check_output(['git','show','e631cb7:docs/audit-v4/INDEPENDENT_COMPLIANCE_MATRIX.md'], cwd=root).decode('utf-8')
(out / 'v4-original-matrix.md').write_text(original, encoding='utf-8')
changes = {
'S02': ('PARCIAL','Estilos raiz incluídos (aceite verde); baseline novo em signatures/ omitido do ZIP (V5-02). Contagem de histórico continua 1 por arquivo.'),
'S03': ('PARCIAL','Rollback remove raízes novas no caso testado. Restore ainda copia por arquivo, sem journal/coordenação dos escritores; não comprovado crash-safe (V5-01).'),
'S04': ('ABERTO','Não-JSON simples e versão desconhecida rejeitados; JSON malformado com formatVersion válido sobrescreve dados (V5-01, dois testes vermelhos).'),
'S07': ('PARCIAL','Leitor detecta novo manifesto no caso testado; mtime+tamanho não detectam substituição de mesmo tamanho/data (V5-03). Integração observável permanece parcial.'),
'S08': ('PARCIAL','Prescrição vazia honesta confirmada; slant ausente no salvamento ao alfabeto ainda vira default do estilo (V5-04).'),
'S09': ('PARCIAL','Retas densa/esparsa do aceite medem igual. Professor mantém alvo global 52 e não consome alvo/estilo de cada tentativa (V5-04).'),
'S11': ('PARCIAL','Cálculo em Default e finally confirmados. Leitura e saves permanecem no scope main, sem ordenação de reanálises e sem transação conjunta (V5-04).'),
'S14': ('PARCIAL','Baseline gravado/recarregado no init, com catches silenciosos e dois arquivos não transacionais. AndroidView não aplica setStrokes. Teste alegado não existe (V5-02/06).'),
'S15': ('PARCIAL','SVG enquadra o caso negativo, aceite verde; PNG permanece com coordenadas originais e canvas fixo; entrega ao destino externo não comprovada (V5-02).'),
'S16': ('PARCIAL','Diagonais opostas diferenciadas; contorno fechado invertido ainda recebe alegação muscular idêntica (V5-05).'),
'S20': ('PARCIAL','styleId/targetSlant agora sobrevivem no manifesto; Professor ignora contexto e catálogo permanece comum (V5-04).'),
'S22': ('PARCIAL','Runner passa 5 comandos, 243/46 testes/classes; seis contraprovas V5 falham. Testes unitários não comprovam integração (V5-06).'),
'S23': ('ABERTO','Dossiê declara fechamento total, teste S14 ausente e hash inexistente; matriz histórica alterada pelo implementador (V5-06).'),
'S24': ('ABERTO','Round-trip de escapes do aceite passa; extrator não valida gramática JSON e serializers Teacher/PersonalStyle não mudaram (V5-01).'),
'R03': ('PARCIAL','Leitor aberto vê adição simples; não detecta substituição de mesmo tamanho/data. Evolution não ganhou observação de alterações (V5-03).'),
'R04': ('PARCIAL','Seeds novos removidos, diretório unificado e callback de erro corrigido. Flow/cache por instância sem recarga; migração das variantes antigas não demonstrada (V5-04).'),
'R06': ('PARCIAL','Caso de mtime igual com tamanho diferente passa. Sem hash; merge prioriza registro antigo com mesmo ID. Locks continuam por instância (V5-03).'),
'R07': ('PARCIAL','21 sessões agora entram no resumo, aceite verde. Divergência UTC/local e integração do histórico permanecem como V4.'),
'R11': ('PARCIAL','Seeds novos removidos; compilador ainda prefere slant de metadados/defaults. Não recebe correção geométrica nesta revisão.'),
'R14': ('PARCIAL','Inclinação da reta densa agora medida (aceite verde); proximidade permanece média por ponto, invariância geral não comprovada.'),
'R18': ('PARCIAL','Contexto passa a persistir; timer acumulativo e seleção limitada de comparações permanecem.'),
'R20': ('ABERTO','16 aceites agora verdes; seis novas contraprovas e declarações documentais inexatas impedem aprovação global (V5-06).'),
'A13': ('PARCIAL','Caso de reta densa corrigido; catálogo não acompanha estilos e avaliação continua sensível à distribuição de pontos.'),
'A17': ('PARCIAL','243 testes/46 classes verdes, incluindo os 16 originais; seis novos testes discriminantes falham. Sem src/androidTest.'),
'A18': ('PARCIAL','Lint V5: 0 erros/48 warnings. Workflow sem gate lint/validação de assinatura; release remoto não revalidado.'),
'A22': ('ABERTO','Matriz e dossiê de submissão não correspondem ao fechamento comprovado. Parecer e matriz independentes V5 corrigem o estado vigente (V5-06).'),
}
lines = ['# Matriz independente V5 — 66 requisitos originais', '',
          'SHA auditado: `'+summary['sha']+'`. Data: 2026-09-13.', '',
          'Esta matriz é do auditor; não substitui nem reescreve a matriz submetida pelo implementador. Requisitos recuperados do commit independente `e631cb7`, anterior às alterações da matriz histórica. [Parecer](../ANTIGRAVITY_AUDIT_REVIEW_V5.md).', '',
          'CORRIGIDO_JVM/CODIGO aplica-se ao defeito delimitado, nunca ao milestone. PARCIAL indica requisitos ainda incompletos. Itens sem mudança relevante conservam a análise V4 por comparação de código; não significam nova prova dinâmica. V4-01…07 referem-se ao parecer V4 original; V5-01…06 ao novo parecer.', '',
          '| ID | Requisito original | Estado V5 | Evidência/limite |', '|---|---|---|---|']
states = Counter()
for line in original.splitlines():
    if not line.startswith(('| S','| R','| A')): continue
    cols = [c.strip() for c in line.split('|')[1:-1]]
    if len(cols) != 4 or cols[0] not in [f'{p}{i:02d}' for p,n in [('S',24),('R',20),('A',22)] for i in range(1,n+1)]: continue
    ident,req,status,note = cols
    status,note = changes.get(ident,(status,'Sem mudança relevante no código desde V4. '+note))
    states[status] += 1
    lines.append(f'| {ident} | {req} | {status} | {note} |')
assert sum(states.values()) == 66
lines += ['', 'Contagem: '+', '.join(f'{k}: {v}' for k,v in states.items())+'.', '',
          'Ondas 0–3 e fechamento M0–M8: não aprovados integralmente. Priorizar integridade/restore e integração real; manter gate físico separado.']
(out.parent / 'INDEPENDENT_REVIEW_MATRIX.md').write_text('\n'.join(lines)+'\n', encoding='utf-8')
print(json.dumps({'baseline':summary['baseline'],'lint':summary['lint'],'states':dict(states)}, ensure_ascii=False))
