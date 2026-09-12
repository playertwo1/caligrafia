package com.scribe.caligrafia.learning.model

/**
 * Estágios progressivos do aprendizado caligráfico formal (M4 — Learning System).
 *
 * Respeita a ordem natural de consolidação neuromotora:
 * Traço elementar -> Famílias morfológicas -> Conexões -> Palavras -> Frases e ritmo.
 */
enum class CurriculumStage(
    val stageNumber: Int,
    val title: String,
    val description: String
) {
    STAGE_1_STROKES(
        stageNumber = 1,
        title = "Traços Fundamentais",
        description = "Desenvolvimento do controle motor fino, paralelismo, pressão e arcos elementares."
    ),
    STAGE_2_FAMILIES(
        stageNumber = 2,
        title = "Famílias de Letras",
        description = "Agrupamento morfológico por semelhança de ductus (Underturn, Oval, Hastes e Caudas)."
    ),
    STAGE_3_CONNECTIONS(
        stageNumber = 3,
        title = "Conexões e Ligaduras",
        description = "Treino de transições e ligaduras fluidas entre letras na linha de base."
    ),
    STAGE_4_WORDS(
        stageNumber = 4,
        title = "Palavras e Ritmo",
        description = "Construção de palavras completas com espaçamento homogêneo e consistência de ritmo."
    ),
    STAGE_5_SENTENCES(
        stageNumber = 5,
        title = "Frases e Expressões",
        description = "Fluidez contínua na linha de escrita e sustentação postural e de pegada na caneta."
    )
}

/**
 * Categoria tipológica da lição caligráfica.
 */
enum class LessonType {
    /** Traço motor básico (reta, curva, laço). */
    STROKE,

    /** Letra cursiva isolada. */
    LETTER,

    /** Ligadura de duas ou mais letras consecutivas. */
    CONNECTION,

    /** Palavra completa com sentido semântico. */
    WORD,

    /** Frase caligráfica clássica ou expressão curta. */
    SENTENCE
}

/**
 * Lição estruturada do currículo pedagógico do Scribe (SCR-401).
 *
 * @param id Identificador único e estável da lição.
 * @param title Nome descritivo da lição em português.
 * @param description Instruções detalhadas sobre a execução motora esperada.
 * @param stage Estágio da trilha curricular em que a lição se insere.
 * @param type Tipologia da atividade (traço, letra, conexão, palavra, frase).
 * @param glyphIds Identificadores dos glifos canônicos de referência associados a esta lição.
 * @param targetStyleId Família formal recomendada (ex: "copperplate", "cursiva_escolar", "spencerian").
 * @param recommendedMinutes Duração sugerida para a prática diária (padrão 10 min).
 * @param targetText Texto caligráfico de demonstração caso aplicável a palavras/frases.
 */
data class CurriculumLesson(
    val id: String,
    val title: String,
    val description: String,
    val stage: CurriculumStage,
    val type: LessonType,
    val glyphIds: List<String> = emptyList(),
    val targetStyleId: String = "copperplate",
    val recommendedMinutes: Int = 10,
    val targetText: String? = null
)
