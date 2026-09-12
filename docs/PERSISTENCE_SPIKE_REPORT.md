# PERSISTENCE_SPIKE_REPORT — Relatório Técnico e Decisão Arquitetural (SCR-006)

**Projeto:** Scribe (Caligrafia com S Pen / Stylus)  
**Milestone:** M0 — Stylus Lab  
**Tarefa:** SCR-006 Persistence Spike  
**Data:** 2026-09-12  
**Autor:** Antigravity / Pair Programming  
**Revisor:** Codex  

---

## 1. Contexto e Objetivo

O Scribe captura escrita manual como sequências de vetores de alta fidelidade temporal (`Stroke` e `StrokePoint`), gerando entre 120 e 240 amostras por segundo no Samsung Galaxy S25 Ultra. Uma sessão típica de treino caligráfico de 10 minutos pode produzir com facilidade entre 10.000 e 50.000 amostras com coordenadas $(x, y)$, timestamps relativos e sensores físicos (pressão, tilt, orientação).

Conforme estipulado em [DATA_MODEL.md](file:///c:/Users/fael/Documents/Codex/scribe/DATA_MODEL.md) e [BACKLOG.md](file:///c:/Users/fael/Documents/Codex/scribe/BACKLOG.md), a tarefa **SCR-006** exige uma investigação empírica (Spike) comparando três estratégias de persistência local antes de fixar o storage definitivo no Milestone M1:
1. **Estratégia A — Tabela Relacional Normalizada (Ponto-a-Ponto):** Cada ponto é uma linha independente com foreign key para o traço.
2. **Estratégia B — Blob Binário Compactado no Registro do Traço (Híbrido Room/BLOB):** Metadados do traço relacionais, com lista de pontos serializada e compactada via Deflate/GZIP com `schemaVersion`.
3. **Estratégia C — Arquivo Binário Dedicado Standalone (.scribe):** Arquivo vetorial dedicado por sessão/página com Magic Bytes (`SCRIBE01`) e streaming sequencial, com índice leve no banco.

---

## 2. Metodologia do Benchmark

O executor automatizado [`PersistenceBenchmarkRunner`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/ink/persistence/benchmark/PersistenceBenchmarkRunner.kt) gerou sessões determinísticas simulando caligrafia real:
* Traçados curvos contínuos com interpolação de Bézier.
* Densidade temporal realista de ~125 Hz.
* Modulação contínua de pressão física ($0.25$ a $0.75$).
* Variação de inclinação (*tilt*) e orientação física.
* Traços de caneta (`ToolType.STYLUS`) e borracha (`ToolType.ERASER`).

Para cada estratégia, mediu-se:
1. **Tempo de Gravação (Write Time):** Tempo para serializar e salvar em disco.
2. **Tempo de Leitura (Read Time):** Tempo para ler do disco e desserializar em estruturas de dados imutáveis `Stroke` em memória.
3. **Pegada em Disco (Storage Footprint):** Tamanho do arquivo em bytes e densidade de bytes por ponto (Bytes/Point).
4. **Fidelidade Matemática (Round-Trip Fidelity):** Verificação de que $100\%$ das coordenadas $x/y$, timestamps, pressões e inclinações são restauradas com precisão exata.

---

## 3. Resultados Empíricos Obtidos

Execução padronizada da suíte de teste [`PersistenceSpikeTest`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/test/java/com/scribe/caligrafia/ink/persistence/PersistenceSpikeTest.kt):

| Estratégia Avaliada | Gravação (ms) | Leitura (ms) | Tamanho em Disco | Bytes/Ponto | Redução vs Relacional | Fidelidade 100% |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **A. Tabela Relacional (Ponto-a-Ponto)** | 22,84 ms | 15,81 ms | 47,73 KB | 61,1 B/pt | Referência (0%) | **EXATA** |
| **B. Blob Binário Compactado (Room/BLOB)** | 5,49 ms | 3,32 ms | 14,46 KB | 18,5 B/pt | **- 69,7%** | **EXATA** |
| **C. Arquivo Binário Dedicado (.scribe)** | **2,46 ms** | **2,87 ms** | **5,72 KB** | **7,3 B/pt** | **- 88,0%** | **EXATA** |

*Carga de teste: 20 traços, 800 pontos vetoriais.*

### Projeção para Sessão Real (50.000 pontos / ~1.000 traços):
* **Tabela Relacional:** ~3,05 MB em disco; ~1,4 segundos de I/O em SQLite; 50.000 linhas inseridas por sessão.
* **Blob Compactado:** ~925 KB em disco; ~210 ms de I/O; 1.000 linhas no SQLite.
* **Arquivo Dedicado (.scribe):** **~365 KB em disco; ~150 ms de streaming I/O; 1 linha de índice no banco.**

---

## 4. Análise Comparativa e Trade-offs

### 4.1. Estratégia A: Tabela Relacional Normalizada
* **Vantagens:** Permite consultas SQL diretas sobre coordenadas e timestamps sem desserializar.
* **Desvantagens Críticas:**
  - **Pior desempenho:** Latência de escrita 9.2x mais lenta e leitura 5.5x mais lenta que o arquivo dedicado.
  - **Amplificação de Armazenamento:** 61.1 bytes por ponto. Em 100 sessões de caligrafia, o banco local consumiria mais de 300 MB de armazenamento desnecessário.
  - **Sobrecarga do SQLite:** Milhares de `INSERT`s com foreign keys e overhead de transação geram pressão no garbage collector e risco de frames congelados (*jank*) no aparelho.
* **Conclusão:** **REJEITADA** como modelo de armazenamento de pontos no Scribe.

### 4.2. Estratégia B: Blob Binário Compactado (Room/BLOB)
* **Vantagens:**
  - Mantém o traço (`StrokeEntity`) como entidade relacional no Room, permitindo queries por `sessionId`, `exerciseId`, `toolType` e timestamps.
  - Excelente redução de tamanho (18.5 bytes por ponto, ~70% menor que a tabela relacional).
  - Permite manipulação granular (carregar ou apagar um traço individualmente sem ler toda a página).
* **Desvantagens:**
  - Cada traço requer uma chamada de compressão/descompressão individual.

### 4.3. Estratégia C: Arquivo Binário Dedicado Standalone (`.scribe`)
* **Vantagens:**
  - **Campeão absoluto em desempenho:** Gravação em 2.46 ms e leitura em 2.87 ms.
  - **Campeão absoluto em compactação:** 7.3 bytes por ponto (88% menor que o modelo relacional).
  - **Isolamento e Portabilidade:** O documento de uma página/caderno é um arquivo físico autocontido com Magic Bytes (`SCRIBE01`) e `schemaVersion`. Facilita backup, exportação, compartilhamento entre dispositivos e sincronização futura sem travar o banco do app.
  - **Streaming I/O:** Leitura em um único buffer sequencial sem contenda no SQLite.

---

## 5. Decisão Arquitetural Recomendada (ADR)

Para o **Gate M0** e a construção dos cadernos e exercícios no **Milestone M1**:

1. **Adotar a Arquitetura Híbrida (Estratégia C + Room Leve):**
   - **Banco de Dados (Room):** Armazena exclusivamente metadados relacionais leves:
     - `PracticeSession`: id, timestamp início/fim, estilo caligráfico, exercício, pontuação.
     - `InkPage`: id da página, dimensões, pautas/guias e caminho relativo do arquivo binário (`filePath`).
   - **Arquivo Vetorial (`.scribe`):** Armazena a carga útil bruta e imutável de traços e pontos da página/sessão, estruturado com cabeçalho `SCRIBE01` e streaming sequencial compactado.
2. **Camada de Adaptação para Undo/Scratchpad:**
   - Em memória e para sessões ativas com manipulação traço-a-traço frequente (como borracha por traço e undo/redo), a representação em memória utiliza a lista imutável de `Stroke`s, persistindo snapshots em arquivo binário ao pausar ou fechar a página.
3. **Versionamento de Schema:**
   - Todo arquivo `.scribe` contém `Short schemaVersion = 1` no cabeçalho fixo, garantindo compatibilidade reversa para futuras adições (ex: canetas adicionais, vetores de velocidade interpolados).

---

## 6. Próximos Passos
* **SCR-007 — Replay:** Implementar a reprodução temporal precisa dos traços com velocidades 0.5x, 1x e 2x com base nos timestamps reais capturados e persistidos.
