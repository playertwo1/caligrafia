# Prompt de conclusão local — F5, F6 e F7

Copie o bloco abaixo integralmente para o agente que tiver acesso local ao repositório, Android SDK/Gradle e, quando chegar à validação física, ao Galaxy S25 Ultra/S Pen/Watch.

---

Você está continuando o projeto **Scribe / Caligrafia** no repositório `playertwo1/caligrafia`.

## Regra principal

Trabalhe **autonomamente até o limite possível**, sem pedir confirmações para requisitos já decididos. Não use GitHub Actions: a cota mensal está esgotada. Use o ambiente local, Android Studio/Gradle e dispositivos físicos quando disponíveis. Não invente resultados. Se um teste, build, screenshot ou validação física não puder ser executado, registre `PENDENTE_*` com a causa real e continue tudo que for independente.

## Checkout obrigatório

1. `git fetch --all --prune`
2. `git checkout phase-f6-f7-finalization`
3. `git pull --ff-only`
4. registre `git rev-parse HEAD` e `git status --short`.
5. Preserve qualquer alteração local do usuário; não limpe/reset dados ou arquivos para fazer teste passar.

Leia, nesta ordem:

- `AGENTS.md`
- `docs/ANTIGRAVITY_EXECUTION_CHECKLIST.md`
- `docs/ANTIGRAVITY_FUNCTIONAL_ROADMAP.md`
- `docs/product-delivery/CURRENT_STATE_F5_F7.md`
- `docs/product-delivery/F6/OPTION_EFFECT_MATRIX.md`
- `docs/product-delivery/F7/STATIC_REVIEW.md`
- `docs/product-delivery/ROUTE_ACTION_MATRIX.md`
- `docs/product-delivery/EVIDENCE.md`
- `docs/design/fluxos-v1/README.md`

O checklist é a ordem canônica. Não replaneje o produto. Não adicione backend/cloud/LLM, contas, marketplace, reconhecimento biométrico de assinatura ou qualquer feature fora do escopo.

## Objetivo 1 — validar primeiro o que já foi implementado

Execute:

```powershell
powershell -ExecutionPolicy Bypass -File docs/product-delivery/run-local-f5-f7-validation.ps1
```

Se falhar, corrija **a causa no código**, preserve os testes diagnósticos e repita até obter um run limpo. Não enfraqueça testes para normalizar bug. Registre logs/XML e SHA do mesmo checkout. Se o script em si tiver erro, corrija o script e documente a correção antes de reutilizá-lo.

Além do runner, execute testes focados de F5 e F6 se os nomes/classes estiverem presentes, inclusive `PhaseF6AcceptanceTest` e os testes de assinatura/backup/restore adicionados em F5. Confirme que nenhum novo teste fica apenas compilando sem ser descoberto pelo runner.

## Objetivo 2 — concluir as pendências reais da F6

Use `docs/product-delivery/F6/OPTION_EFFECT_MATRIX.md` como lista de lacunas. Feche somente o que puder provar.

### Preferências

Completar a tela **Preferências** com grupos claros **Escrita**, **Leitura** e **Pausas**, com Voltar/Concluir coerentes. Reutilizar `ScribePreferencesStore`/`ScribePreferencesRuntime`; não criar um segundo armazenamento concorrente.

Expor e persistir na UI:

- mão direita/esquerda;
- lado da barra: recomendado/automático, esquerda ou direita;
- entrada: somente caneta ou caneta + toque;
- escala de texto, multiplicando a escala do sistema sem anulá-la;
- contraste de pautas com preview real;
- reduzir animações sem desabilitar Replay voluntário;
- curva de pressão **somente Linear, Suave e Firme** na UI contratada;
- lembrete de pausa habilitado e intervalo;
- vibração habilitada e ação **Testar vibração**.

### Editor e render

Aplicar `effectiveToolbarSide` no editor do caderno, com padding seguro para não cobrir a área da palma. Validar direita/esquerda no S25 Ultra.

Aplicar escala de texto respeitando `fontScale` do sistema; testar fonte padrão e ampliada. Nenhum controle essencial deve desaparecer.

Aplicar `reduceAnimations` às transições automáticas relevantes; Replay continua acessível manualmente.

Confirmar que contraste e curvas de pressão afetam **somente render derivado**. Comparar raw `StrokePoint` antes/depois; pressão/tilt/orientação originais não podem mudar.

### Pausas e háptico

Integrar `ActiveBreakReminder` à sessão de escrita real. Somente deltas de escrita ativa contam. Pausa manual, app em background e tela inativa não podem acumular alerta atrasado.

Vibração desabilitada deve impedir feedback. Sem vibrator/permissão disponível, retornar/mostrar indisponibilidade sem falso sucesso.

### Watch

Solicitar `BLUETOOTH_CONNECT` em runtime somente quando necessário e explicar a finalidade. Não declarar Watch conectado por pareamento antigo ou nome salvo: status precisa refletir conexão observável.

O `WatchCompanionAdapter` atual contém um probe Bluetooth/GATT e fallback local. Verifique se esse probe é adequado ao aparelho real. Se for necessário receber eventos no Wear OS, implementar a menor integração oficial compatível via adapter/Data Layer sem acoplar domínio ao SDK. **Não** usar S Pen Remote SDK como motor de escrita.

Testar desconexão do Watch durante sessão: a sessão no telefone não pode travar. Registrar recepção física no relógio separadamente; sem relógio, marcar `PENDENTE_DISPOSITIVO` e validar o fallback do telefone.

### Laboratório

Manter Laboratório isolado de Evolução, SRS e Alfabeto. Completar o fluxo explícito **Gravar → Parar → Limpar → Salvar → Reabrir → Replay**.

Na capability UI distinguir `Disponível`, `Ausente` e `A verificar`. Nunca inferir ausência porque o valor é `0.0`: zero suportado precisa continuar zero. Reabrir amostra salva e comparar raw campo a campo.

## Objetivo 3 — fechar F5.G antes de promover restore

Validar F5 integralmente em armazenamento limpo e em acervo existente:

- assinatura atual vs referência explicitamente selecionada;
- sair/voltar/reiniciar e recuperar draft/referência;
- SVG/PNG abrindo fora do app, bounds negativos, transparência e dimensões;
- página marfim+pautas e branca+sem linhas, proporção correta;
- cancelamento de seletor Android sem falso sucesso;
- `.scribepack` misto com cadernos, tentativas/sessões/SRS, variantes, estilos/fontes, Professor, assinaturas, cópias e preferências;
- pacote inválido, Zip Slip, `.scribe` inválido, falha entre arquivos e interrupção de processo;
- restore é **replace**, não merge: itens ausentes no pacote antigo não podem ressuscitar;
- rollback persistente restaura o conjunto anterior e só afirma “dados inalterados” quando verificado;
- round-trip preserva IDs, raw e vínculos e a View restaurada abre corretamente.

Só marque F5.G depois dessas provas.

## Objetivo 4 — executar a F7 de consolidação

### Matriz de rotas

Atualizar `docs/product-delivery/ROUTE_ACTION_MATRIX.md` para incluir as ações faltantes de F5/F6 detectadas em `F7/STATIC_REVIEW.md`. Cada linha deve ter origem, controle, destino, argumentos, handler/ViewModel, repositório/estado, teste e situação real.

Percorra **todas** as linhas. Botão visível sem handler/estado/persistência/teste não é entregue.

### 12 áreas e 12 fluxos

Comparar o app real com `docs/design/scribe-visao-12-telas-v1.png` e as 12 pranchas em `docs/design/fluxos-v1/`. As imagens são direção visual, não verdade de dados. Registrar diferenças justificadas; não copiar números fictícios como seeds.

Gerar screenshots **reais** no mesmo SHA. Não usar mockup/imagem gerada como evidência.

### Acessibilidade e S25 Ultra

No Galaxy S25 Ultra testar:

- fonte padrão e ampliada;
- insets/barras do sistema;
- teclado;
- alcance da S Pen;
- todos os níveis Ghost;
- mão direita/esquerda e lado da barra;
- somente caneta / caneta + toque + rejeição de palma;
- pressão zero, tilt zero e orientação zero quando suportados;
- labels/contentDescription;
- ordem de foco/TalkBack;
- contraste;
- alvos de toque essenciais.

### Jornadas E2E obrigatórias

Executar do começo ao fim, sem atalhos:

`caderno → aula → treino → feedback → resumo → comparação → variante → estilo pessoal`

continuando com:

`Professor → treino prescrito → cópia → assinatura → export → backup → restore → reinício`

Registrar vídeo curto/screenshot e IDs/dados reais criados pelo app. Repetir casos relevantes após **atualizar uma instalação com acervo antigo**, sem limpar dados.

## Objetivo 5 — build e APK

No mesmo SHA final executar novamente:

- `testDebugUnitTest`
- runner de auditoria vigente
- `assembleDebug`
- `lint`

Preservar logs/XML com o SHA.

Depois gerar o APK de entrega assinado usando segredo/keystore **fora do Git**. Não commit keystore, senha, token, API key ou arquivo de propriedades secreto. Registrar versão, versionCode, SHA Git, SHA-256 do APK e assinatura. Testar instalação limpa e atualização sobre versão anterior, comprovando preservação do acervo.

Debug APK não conta como APK final de entrega.

## Objetivo 6 — documentação e estado final

Após os resultados reais:

- atualizar `docs/product-delivery/EVIDENCE.md`;
- atualizar `docs/product-delivery/F6/OPTION_EFFECT_MATRIX.md`;
- atualizar `docs/product-delivery/F7/STATIC_REVIEW.md` ou substituí-lo por relatório final;
- atualizar `docs/product-delivery/ROUTE_ACTION_MATRIX.md`;
- atualizar `docs/ANTIGRAVITY_EXECUTION_CHECKLIST.md` caixa por caixa;
- atualizar `docs/ANTIGRAVITY_FUNCTIONAL_ROADMAP.md`;
- atualizar `PROJECT_STATE.md`, removendo o topo obsoleto que ainda aponta para F0/F5 quando houver evidência nova suficiente;
- atualizar `CHANGELOG.md`.

Use estados transparentes: `VALIDADO_SOFTWARE`, `PENDENTE_DISPOSITIVO`, `PARCIAL`, `ENTREGUE`. Não escreva “100%”, “aprovado Codex”, “teste físico OK” ou contagem de testes sem evidência correspondente.

## Git

Faça commits pequenos e descritivos na branch `phase-f6-f7-finalization`. Suba tudo para essa branch. **Não faça merge em `main` e não crie release final** até todos os gates obrigatórios estarem comprovados e o Codex emitir parecer F7.G.

Ao terminar, responda usando o formato obrigatório:

```text
Etapa: F5.G + F6.01–F6.G + F7.01–F7.G
SHA/branch:
Caixas concluídas e evidências:
Arquivos/símbolos alterados:
Percurso real demonstrado:
Testes: comando, quantidade real, falhas, XML/log:
Dados preservados/migração:
Pendências e bloqueios:
Próxima caixa:
Revisão Codex: AINDA NÃO REALIZADA / referência ao parecer
```

Se restar qualquer pendência física, não pare o trabalho de software por causa dela: conclua tudo que for independente e deixe somente a pendência física específica registrada.

---
