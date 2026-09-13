# CODEX_AUDIT_INSTRUCTIONS_V4 — Guia de Execução e Verificação da Auditoria Independente (V4)

**Destinatário:** Codex / Auditor Técnico Independente  
**Objetivo:** Fornecer um guia direto, reproduzível e auditável para verificar a estabilização das Ondas 0, 1, 2 e 3 e o Redesenho Visual Integral do Scribe.  
**Data:** 13 de Setembro de 2026  
**Dossiê de Referência Principal:** [`AUDIT_REPORT_STABILIZATION.md`](../AUDIT_REPORT_STABILIZATION.md)  
**Auditoria Anterior:** [`docs/ANTIGRAVITY_AUDIT_REVIEW_V3.md`](ANTIGRAVITY_AUDIT_REVIEW_V3.md)  

---

## 1. Instruções para o Ambiente de Auditoria

### 1.1. Isolamento de Worktree (Recomendado)
Para auditar sem interferir no diretório de trabalho principal ou em caches de builds anteriores, o Codex pode criar um worktree isolado:
```bash
git worktree add ../.audit-worktree-v4 HEAD
cd ../.audit-worktree-v4
```

### 1.2. Limpeza Prévia de Daemons e Lock de Arquivos no Windows
Caso ocorram erros de bloqueio de arquivos em `build/intermediates/.../zip-cache` (comuns no Windows quando o daemon Gradle retém descritores de DLLs/ZIPs):
```powershell
.\gradlew.bat --stop
```

---

## 2. Comandos de Verificação e Resultados Esperados

### Passo 1: Execução dos Testes Formais de Aceitação da Auditoria
Este comando executa exclusivamente os **33 testes de aceitação formal** criados para comprovar a resolução dos achados S01–S20, R01–R20 e A01–A22:
```powershell
.\gradlew.bat testDebugUnitTest --tests com.scribe.caligrafia.audit.AuditFixAcceptanceTest --console=plain
```
- **Resultado Esperado:** `BUILD SUCCESSFUL`.
- **Contagem:** 33 testes executados, 0 falhas, 0 erros (100% de aprovação).
- **Evidências Comprovadas:**
  - `s01`: ZIP exportado possui cabeçalho de diretório central END válido e pode ser lido por `java.util.zip.ZipFile`.
  - `s02`: Backup inclui caminhos canônicos de produção (`personal_alphabet/`, `attempts/`, `custom_fonts/`, etc.).
  - `s03`: Falha simulada durante o restore ativa rollback atômico e preserva 100% dos dados originais.
  - `s04`: Validação rigorosa contra vulnerabilidade Zip-Slip rejeitando travessias de diretório.
  - `s07`: Usuário novo sem tentativas registradas produz nota `0.0f` e nível `BEGINNER` sem notas forçadas de 70%.
  - `s09`: `MotorDiagnosticEngine` marca dimensões sem dados suficientes como `INSUFFICIENT_DATA` e `observedValue = null`, excluindo-as do cálculo da pontuação geral.
  - `s12`: Callbacks de assinatura emitem snapshots imutáveis (`completedStrokes.toList()`), eliminando o bug de aliasing ao limpar o canvas.
  - `s15_s16`: Coordenadas negativas são calculadas com precisão em `SignatureConsistencyEngine` e `exportSvg()` grava arquivo `.svg` físico em disco com `fos.fd.sync()`.
  - `r01` & `r08`: `SessionTimer` com construtor padrão avança em tempo real e encerra com 300s exatos no 300º tick.
  - `r02`: Treino guiado real alimenta a nota da sessão e histórico SRS sem botões manuais (65%, 80%, 95%).
  - `r03`: Repositório de tentativas inicia com lista vazia (zero seeds) e duas tentativas reais formam comparativo Antes/Depois autêntico.
  - `r04`: Navegação a partir do Alfabeto seleciona o glifo correto e variante salva atualiza repositório.
  - `r05`: Estilo pessoal é reconhecido pelo `StyleEngine` sem arquivo TTF e adapta pautas no caderno.
  - `r06`: Instâncias concorrentes de `LocalLearningHistoryRepository` sincronizam via `lastKnownModified`.
  - `r07`: Calendário computa soma exata de segundos sem inflar treinos de 0s para 1 minuto.
  - `r09`: `fos.fd.sync()` executa com descritor aberto e válido em todos os repositórios.
  - `r10`: Concorrência com `Mutex` e transacionalidade estrita em `LocalPersonalAlphabetRepository`.
  - `r11`: `PersonalStyleCompiler` adota convenção angular canônica $\theta = \text{atan2}(dy, -dx)$ e thresholds monotônicos de pauta.
  - `r12`: `DualReplayEngine` em modo `REAL_TIME` preserva tempo físico decorrido.
  - `r14`: `GeometricFeedbackEvaluator` calcula cobertura ponto-a-segmento invariante à amostragem.
  - `r16`: Exportação PNG preserva aspect ratio do canvas real de origem.
  - `r17`: Estilos caligráficos adaptam ângulo alvo e pautas no treino guiado.
  - `r18`: `AttemptComparator` calcula velocidade real em px/ms e ganho de agilidade.
  - `a03`: Exclusão mútua (`pagePersistenceMutex`) serializa salvamento de páginas sem conflitos.

### Passo 2: Execução da Suíte Completa de Testes Unitários
```powershell
.\gradlew.bat testDebugUnitTest --console=plain
```
- **Resultado Esperado:** `BUILD SUCCESSFUL in ~9s`.
- **Contagem Total:** **227 testes unitários / 48 classes de teste / 0 falhas / 0 erros / 0 ignorados**.
- **Relatório HTML:** `app/build/reports/tests/testDebugUnitTest/index.html`.

### Passo 3: Compilação Completa do APK de Depuração
```powershell
.\gradlew.bat assembleDebug --console=plain
```
- **Resultado Esperado:** `BUILD SUCCESSFUL in ~10s` (36 tarefas executadas / up-to-date).
- **Artefato Gerado:** `app/build/outputs/apk/debug/app-debug.apk`.

### Passo 4: Verificação de Lint
```powershell
.\gradlew.bat :app:lintDebug --console=plain
```
- **Resultado Esperado:** `BUILD SUCCESSFUL` com 0 erros bloqueadores.

---

## 3. Roteiro de Inspeção dos 12 Fluxos de Interface

O auditor pode verificar a fidelidade das implementações contra o guia visual em [`docs/design/fluxos-v1/README.md`](design/fluxos-v1/README.md) e o painel [`docs/design/scribe-visao-12-telas-v1.png`](design/scribe-visao-12-telas-v1.png):

1. **Navegação Canônica em 4 Abas (`MainActivity.kt`):**
   - Abas implementadas: *Caderno*, *Praticar*, *Evolução*, *Mais*.
   - Telas artificiais como "Perfil" ou "Explorar" não foram criadas.
2. **Biblioteca de Cadernos (Flow 01 — `NotebookLibraryScreen.kt`):**
   - 4 capas clássicas com texturas: *Papel Artesanal*, *Azul Noite*, *Couro Sépia*, *Verde Floresta*.
   - Componente `NotebookCoverThumbnail` com lombada, vinco e monograma caligráfico.
   - Diálogo de criação com seletor de capas e tipos de pauta.
3. **Organizador de Páginas (Flow 02 — `NotebookPagesDialog.kt`):**
   - Grade em 2 colunas com miniaturas, marcador da página ativa, adição e exclusão com diálogo.
4. **Folha Caligráfica & Barra Flutuante (Flow 03 — `NotebookPracticeScreen.kt`):**
   - Canvas em tela cheia com proteção de bordas laterais (`EdgeGestureExclusionHelper`).
   - Barra flutuante inferior com abas de ferramentas (*Caneta*, *Borracha*, *Guias*, *Páginas*).
5. **Treino Guiado & Feedback Geométrico (Flow 04 & 05 — `GuidedPracticeScreen.kt`, `GeometricFeedbackSheet.kt`):**
   - Cronômetro ativo em tempo real.
   - Ghost Mode progressivo em 5 níveis (100% -> 70% -> 40% -> 10% -> 0%).
   - Bottom sheet "Entenda seu traço" com nota global, telemetria de inclinação vs alvo, aderência e botão "Salvar no Meu Alfabeto".
6. **Evolução & Replay Duplo (Flow 06 — `EvolutionScreen.kt`):**
   - Comparativo visual lado a lado, calendário de consistência e motor de Replay Duplo com timeline em tempo real.
7. **Central "Mais" & Integrações (Flow 07 a 12 — `ExpansionsScreen.kt`):**
   - Hub unificado com abas para Professor, Meu Alfabeto, Backup em 3 passos, Assinaturas (exportação SVG/PNG) e Configurações da S Pen.

---

## 4. Declaração Honesta de Limites e Estado dos Gates

- **Aprovação do Codex:** Este relatório é uma **submissão técnica** para a auditoria independente. Não presumimos nem declaramos aprovação prévia.
- **Validações de Hardware Físico:** Os itens dependentes do aparelho Samsung Galaxy S25 Ultra (latência sub-10ms da Ink API, resposta física dos 4096 níveis de pressão) e do Samsung Galaxy Watch pareado (vibração tátil via Wearable Data Layer) estão expressamente marcados como **`PENDENTE_DISPOSITIVO`**.
- **Dados Brutos:** Os traços brutos do usuário (`StrokePoint` e `Stroke`) permanecem imutáveis; nenhum vetor é substituído ou degradado por bitmap.
