# Contrato de Dados e Inventário de Persistência — Scribe

Este documento especifica a estrutura canônica de arquivos, formatos, esquemas de versionamento e garantias atômicas de persistência utilizadas pelo Scribe.

## 1. Diretórios Canônicos e Arquivos

Todos os dados locais do usuário residem dentro de `context.filesDir` (ou fallback em testes JVM):

| Entidade / Módulo | Caminho Relativo em Disco | Formato / Schema | Descrição |
|---|---|---|---|
| **Cadernos (Metadados)** | `notebooks/manifest.json` | JSON (v1) | Lista de cadernos criados, títulos, datas, estilos e IDs de páginas |
| **Páginas de Caderno** | `notebooks/{notebookId}/page_{pageId}.scribe` | Binário `.scribe` (v2) | Strokes vetoriais da página com cabeçalho de integridade, pontos (x,y,t,p,tilt), cor e largura |
| **Tentativas de Prática** | `attempts/manifest.json` | JSON (v1) | Lista de tentativas avaliadas, pontuações, inclinação, timestamps e IDs de referência |
| **Strokes de Tentativas** | `attempts/{attemptId}.scribe` | Binário `.scribe` (v2) | Traços vetoriais reais da tentativa para replay e avaliação geométrica |
| **Meu Alfabeto Pessoal** | `personal_alphabet/manifest.json` | JSON (v1) | Catálogo de glifos canônicos, lista de variantes, timestamps e variante favorita |
| **Variantes de Glifos** | `personal_alphabet/variants/{variantId}.scribe` | Binário `.scribe` (v2) | Traços vetoriais da variante do glifo desenhado pelo usuário |
| **Estilos Pessoais** | `personal_styles.json` | JSON (v1) | Lista de estilos personalizados compilados do usuário (inclinação alvo, proporção e comportamento) |
| **Histórico SRS / Treino** | `learning_history.json` | JSON (v1) | Histórico de sessões deliberadas de aprendizado, repetição espaçada e tempo acumulado |
| **Diagnóstico do Professor** | `teacher/diagnostic.json` | JSON (v1) | Diagnóstico biomecânico motor acumulado, consistência e prescrições |
| **Laboratório de Assinaturas** | `signatures/baseline.scribe` | Binário `.scribe` (v2) | Traços vetoriais brutos da assinatura de referência |
| **Metadados de Assinaturas** | `signatures/baseline_meta.txt` | Texto / JSON (v1) | Metadados e limites de bounding box da assinatura de referência |
| **Fontes Importadas** | `custom_fonts/*.ttf`, `*.otf` | TTF/OTF binário | Arquivos de fontes locais carregadas como gabarito estético |
| **Pacote de Backup** | `*.scribepack` | ZIP com `manifest.json` | Arquivo comprimido offline contendo todas as pastas acima e manifesto de integridade |

## 2. Invariantes de Dados

1. **Imutabilidade de Traços Brutos:**
   Pontos vetoriais brutos (`x`, `y`, `timestampMs`, `pressure`, `tilt`) capturados da S Pen são gravados sem perda e nunca são substituídos ou sintetizados por fontes ou bitmaps.
2. **Atomicidade em Gravação (Write-Temp-Fsync-Rename):**
   Todos os repositórios gravam primeiro em arquivo temporário (`.tmp`), executam `flush()` e `fos.fd.sync()` com o descritor aberto, fecham o arquivo e realizam movimentação atômica (`StandardCopyOption.ATOMIC_MOVE` ou substituição segura).
3. **Detecção e Invalidação de Cache por Hash:**
   Tanto `LocalPracticeAttemptRepository` quanto `LocalLearningHistoryRepository` mantêm hash MD5/SHA do conteúdo em disco para detectar edições concorrentes de outras instâncias ou restaurações de backup, invalidando imediatamente metadados e caches de strokes em memória.
4. **Resistência Contra Corrupção no Backup (Zip-Slip & JSON estrito):**
   O `BackupSerializer` e `ScribeBackupManager` validam todos os caminhos canônicos e rejeitam manifestos com JSON incompleto ou sintaxe inválida, assegurando rollback atômico e impedindo que arquivos corrompidos substituam dados legítimos.
