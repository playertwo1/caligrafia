# Scribe — O Que Foi Feito e O Que Falta (Relatório para Codex e Roadmap)

**Data de Atualização:** 12 de Setembro de 2026  
**Versão:** 0.1.0  
**Aparelho-Alvo Principal:** Samsung Galaxy S25 Ultra (com S Pen original)  
**Repositório GitHub:** https://github.com/playertwo1/caligrafia  

---

## 1. O Que Foi Feito Até Agora (Marcos M0 e M1 100% Concluídos)

### 1.1. Milestone M0 — Stylus Lab (Fundação do Motor de Caneta)
- **SCR-001 — Bootstrap do Projeto:** Android SDK 35 (API 35), Min SDK 26, Kotlin 2.2.10, Gradle 9.3.1, AGP 9.1.1, Jetpack Compose com Material 3, aceleração gráfica por hardware ativa.
- **SCR-002 — Inspeção Segura de Hardware (`DeviceCapabilityInspector`):** Diagnóstico automático de caneta Stylus, touch, ranges de sensores (`AXIS_PRESSURE`, `AXIS_TILT`, `AXIS_ORIENTATION`, `AXIS_DISTANCE`), identificação das características da S Pen EMR passiva do S25 Ultra (sem Bluetooth/bateria) e delimitação de SDKs Samsung e Android Ink API.
- **SCR-003 — Pipeline de Ingestão de Alta Precisão (`StrokeCapturePipeline`):**
  - Consumo obrigatório de amostras históricas (`event.historySize`) para não perder nenhum ponto entre frames de 120Hz.
  - Preservação estrita de timestamps e sensores em estruturas imutáveis (`StrokePoint` e `Stroke`).
  - Nunca inventar dados (se o sensor não fornecer, permanece `null`).
- **SCR-004 — Renderizador Polimórfico em Tempo Real (`SmoothedReferenceRenderer`):**
  - Interpolação Bézier quadrática suave em pontos médios.
  - Modulação de espessura por pressão física da S Pen.
  - **Regra Inviolável:** A suavização é apenas uma projeção visual no Canvas; os traços brutos continuam 100% intocados e matematicamente puros.
- **SCR-005 — Rejeição de Palma Comportamental (`PalmRejectionPolicy`):**
  - Modo estrito *Stylus Only* (dedo nunca desenha tinta de caligrafia).
  - Preempção imediata por proximidade (Hover EMR da S Pen) e toque ativo da caneta.
  - Cooldown de repouso da mão (500ms).
  - Roteamento do botão lateral da S Pen para borracha vetorial.
- **SCR-006 — Spike de Persistência e ADR:**
  - Benchmark automatizado de 3 abordagens (Tabela Relacional SQLite vs. BLOB Compactado vs. Arquivo Binário Dedicado `.scribe`).
  - `.scribe` comprovou ser **9.2x mais rápido** e **88% mais leve** (7.3 bytes/ponto contra 61.1 bytes/ponto do SQLite normalizado).
- **SCR-007 — Motor de Replay Temporal Vetorial (`StrokeReplayEngine`):**
  - Reconstrução pura e determinística em 60 fps (`computeFrameAt`).
  - Velocidades 0.5x, 1.0x e 2.0x, scrubber temporal deslizante.
- **SCR-008 — Resiliência de Ciclo de Vida e Sessão:**
  - `StylusLabViewModel` com sobrevivência a rotação de tela e split-screen.
  - Flush e commit automático de traço ativo ao perder foco de tela ou bloquear o aparelho.
  - Auto-save assíncrono de contingência em `.scribe`.
  - Detector de reinserção física da S Pen no silo do S25 Ultra (`com.samsung.pen.INSERT`).

---

### 1.2. Milestone M1 — Caderno (Caderno Vetorial Multi-Página)
- **SCR-009 — Pautas Caligráficas Geométricas (`GuidelineConfig`):**
  - 4 linhas mestras: Ascendente, Altura-X (*Waistline*), Linha de Base (*Baseline*) e Descendente.
  - Proporções clássicas: `1:1:1` (Escolar), `2:1:2` (Copperplate), `3:2:3` (Spencerian / Itálica).
  - Linhas de inclinação (*slant lines*) calculadas por trigonometria exata ($\Delta X = \Delta Y / \tan(\theta)$).
  - Renderizador nativo `GuidelineRenderer` em plano de fundo sem impacto no desempenho.
- **SCR-010 — Gestor de Caderno e Páginas (`LocalNotebookRepository`):**
  - Entidades imutáveis `Notebook` e `NotebookPage`.
  - Serializador leve de manifestos `NotebookManifestSerializer`.
  - Armazenamento em arquivos binários dedicados `.scribe` por página.
  - Criação, paginação, exclusão e carregamento assíncrono via coroutines.
- **SCR-011 — Ferramentas Caligráficas e Pilha Bidirecional de Undo/Redo:**
  - Modelo `ToolConfig` com calibração de espessura (Fina 2.5px, Média 5.0px, Grossa 8.5px).
  - Paleta de tintas nobres: Nanquim, Sépia, Azul Real, Vinho, Grafite e Verde Floresta.
  - Pilha completa de Desfazer (`undo()`) e Refazer (`redo()`) em `InMemoryStrokeRepository`.
  - Schema v2 do `.scribe` com retrocompatibilidade total com a v1.
- **SCR-012 — Borracha por Traço Completa e Segment Eraser:**
  - `StrokeEraserHelper` com AABB Bounding Box e distância ponto-a-segmento euclidiana.
  - Apagamento em varredura contínua (*sweep*) de múltiplos traços.
  - Apagamento de toques pontuais (pingos no 'i' e acentos).
- **SCR-013 — Exportação de Página para PNG em Alta Resolução (`PageExporter`):**
  - Exportação de página completa em 1440x2560 (ARGB_8888) com pautas e suavização Bézier.
  - **Regra Inviolável Garantida:** O bitmap gerado é apenas derivado para exportação; os traços vetoriais brutos permanecem intocados no disco.
- **SCR-014 — Interface Completa do Caderno (`NotebookPracticeScreen`):**
  - Barra de paginação (anterior, próxima, nova página `+`).
  - Toolbar caligráfica completa em Jetpack Compose.
  - Superfície de escrita `NotebookCanvasView` com visual e sensação de papel caligráfico.
  - Alternador direto na `MainActivity` para ir e voltar do Stylus Lab (M0).

---

## 2. Cobertura de Testes e Qualidade

- **Testes Unitários Automatizados:** 73 testes passando (0 falhas).
- **Verificação do Watchdog (`watchdog.ps1`):** Aprovado.
  - Zero WebViews.
  - Zero dependências não autorizadas de nuvem/backend.
- **Compilação:**
  - APK Debug: `app-debug.apk` (22.1 MB)
  - APK Release Assinado: `app-release.apk` (16.1 MB)

---

## 3. Os 4 Secrets Criados para Assinatura e Atualizações

Para permitir que o app seja atualizado continuamente pelo GitHub Actions sem gerar erro de incompatibilidade de assinatura no Android:
1. `RELEASE_KEYSTORE_BASE64`: Arquivo de chaves `.jks` codificado em Base64.
2. `RELEASE_KEYSTORE_PASSWORD`: Senha mestra da keystore.
3. `RELEASE_KEY_ALIAS`: Identificador da chave privada (`scribe_release_key`).
4. `RELEASE_KEY_PASSWORD`: Senha da chave privada.

---

## 4. O Que Falta Implementar nos Próximos Marcos (Roadmap M2 a M8)

Conforme a especificação [ROADMAP.md](file:///c:/Users/fael/Documents/Codex/scribe/ROADMAP.md) e [PRODUCT_SPEC.md](file:///c:/Users/fael/Documents/Codex/scribe/PRODUCT_SPEC.md):

### 4.1. M2 — Treino Guiado (PRÓXIMO MARCO)
- **SCR-015 — Glyph de Referência:** Modelo de caracteres e formas caligráficas estruturados como gabarito visual (traço de esqueleto e contorno).
- **SCR-016 — Ghost Mode Dinâmico:** Controle suave de opacidade do gabarito (100% $\to$ 70% $\to$ 40% $\to$ 10% $\to$ 0%).
- **SCR-017 — Fluxo Pedagógico:** Fases: Rastrear (*Trace*) $\to$ Copiar ao lado (*Copy*) $\to$ Escrever sozinho com pautas (*Solo*).
- **SCR-018 — Exercícios Estruturados por Letra:** Exercícios progressivos do alfabeto (minúsculas, maiúsculas, numerais) com feedback geométrico determinístico sem heurísticas punitivas.

### 4.2. M3 — Style Engine
- Especificação de arquivo `ScribeStyle v1`.
- Três estilos pedagógicos iniciais (ex: Cursiva Escolar, Copperplate Básica, Fundacional/Itálica).
- Motor de importação de fontes TTF/OTF para referência visual (renderização como gabarito sem substituir o modelo de strokes).

### 4.3. M4 — Learning System
- Currículo pedagógico estruturado: Traços fundamentais $\to$ Famílias de letras $\to$ Conexões/ligações $\to$ Palavras $\to$ Frases.
- Duração de sessões adaptativa: 5, 10, 15 ou 20 minutos.
- Histórico de prática e motor de revisão por regras locais determinísticas (sem nuvem).

### 4.4. M5 — Evolução & Análise Visual
- Comparador Before / After da escrita do usuário.
- Overlay de transparência entre a primeira tentativa e a atual.
- Replay lado a lado (*Side-by-side Replay*) de duas sessões diferentes para ver a melhoria na velocidade e estabilidade.
- Calendário de regularidade e tempo total praticado (indicadores positivos não punitivos).

### 4.5. M6 — Meu Alfabeto
- Tela "Meu Alfabeto" para arquivar a melhor tentativa de cada letra do usuário.
- Variantes favoritas e evolução de versões (v1, v2, v3...).
- Exportação do `PersonalStyle` local para reutilizar como referência de assinatura e escrita rápida.

### 4.6. M7 — Professor IA (Apenas quando houver volume real de sessões locais)
- Interpretação de padrões de escrita e diagnóstico de inconsistências de ângulo ou pressão.
- Recomendação de foco personalizada e geração de treinos sem inventar métricas absolutas.

### 4.7. M8 — Expansões e Ecossistema
- Integração opcional com Galaxy Watch para timer de treino caligráfico e haptics de postura.
- Backup/Sync local opcional.
- Laboratório de criação de assinatura pessoal.
- Modo de cópia de trechos literários clássicos.

---

## 5. Instruções para Auditoria pelo Codex

1. O arquivo completo de auditoria técnica detalhada está disponível em [`AUDIT_REPORT.md`](file:///c:/Users/fael/Documents/Codex/scribe/AUDIT_REPORT.md).
2. O script de verificação estática e compilação do Watchdog pode ser executado via terminal com:
   ```powershell
   .\scripts\watchdog.ps1
   ```
3. O APK assinado pronto para instalação direta no Samsung Galaxy S25 Ultra foi copiado para o Google Drive na pasta `Apks/` e `Scribe/`.
