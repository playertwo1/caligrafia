package com.scribe.caligrafia.learning.model

/**
 * Catálogo canônico da trilha pedagógica progressiva do Scribe (SCR-401).
 *
 * Estrutura 18 lições progressivas divididas nos 5 estágios:
 * 1. Traços Fundamentais
 * 2. Famílias de Letras
 * 3. Conexões e Ligaduras
 * 4. Palavras e Ritmo
 * 5. Frases e Expressões
 */
object CurriculumCatalog {

    val ALL_LESSONS: List<CurriculumLesson> = listOf(
        // ==========================================
        // ESTÁGIO 1: TRAÇOS FUNDAMENTAIS
        // ==========================================
        CurriculumLesson(
            id = "lesson_s1_01_slant",
            title = "Controle de Inclinação e Pressão",
            description = "Pratique o traço reto descendente mantendo paralelismo uniforme de 52° ou 68° com pressão firme do topo à linha de base.",
            stage = CurriculumStage.STAGE_1_STROKES,
            type = LessonType.STROKE,
            glyphIds = listOf("basic_slant"),
            targetStyleId = "copperplate",
            recommendedMinutes = 5
        ),
        CurriculumLesson(
            id = "lesson_s1_02_underturn",
            title = "Curva Inferior (Underturn)",
            description = "Descida espessa em ângulo, transição suave e arredondada sobre a linha de base e saída capilar leve até a altura-x.",
            stage = CurriculumStage.STAGE_1_STROKES,
            type = LessonType.STROKE,
            glyphIds = listOf("basic_underturn"),
            targetStyleId = "copperplate",
            recommendedMinutes = 5
        ),
        CurriculumLesson(
            id = "lesson_s1_03_overturn",
            title = "Curva Superior (Overturn)",
            description = "Subida fina a partir da base, arco arredondado no topo da altura-x e descida firme em direção à linha de base.",
            stage = CurriculumStage.STAGE_1_STROKES,
            type = LessonType.STROKE,
            glyphIds = listOf("basic_overturn"),
            targetStyleId = "copperplate",
            recommendedMinutes = 5
        ),
        CurriculumLesson(
            id = "lesson_s1_04_compound",
            title = "Curva Composta",
            description = "Fusão de overturn e underturn em um único movimento contínuo de onda caligráfica.",
            stage = CurriculumStage.STAGE_1_STROKES,
            type = LessonType.STROKE,
            glyphIds = listOf("basic_compound"),
            targetStyleId = "copperplate",
            recommendedMinutes = 10
        ),
        CurriculumLesson(
            id = "lesson_s1_05_oval",
            title = "Forma Oval (O-Form)",
            description = "O coração da caligrafia cursiva: arco suave à esquerda, descida espessa e fechamento sutil sem cruzamentos ríspidos.",
            stage = CurriculumStage.STAGE_1_STROKES,
            type = LessonType.STROKE,
            glyphIds = listOf("basic_oval"),
            targetStyleId = "copperplate",
            recommendedMinutes = 10
        ),
        CurriculumLesson(
            id = "lesson_s1_06_ascending_loop",
            title = "Laçada Ascendente",
            description = "Subida capilar inclinada até a linha superior e descida reta espessa até a linha de base.",
            stage = CurriculumStage.STAGE_1_STROKES,
            type = LessonType.STROKE,
            glyphIds = listOf("basic_ascending_loop"),
            targetStyleId = "copperplate",
            recommendedMinutes = 10
        ),

        // ==========================================
        // ESTÁGIO 2: FAMÍLIAS DE LETRAS
        // ==========================================
        CurriculumLesson(
            id = "lesson_s2_01_underturn_family",
            title = "Família Underturn: Letra 'i' e 't'",
            description = "Aplique o traço underturn na construção das letras 'i' e 't', observando a altura estendida da haste do 't' e o pingo leve.",
            stage = CurriculumStage.STAGE_2_FAMILIES,
            type = LessonType.LETTER,
            glyphIds = listOf("letter_i", "letter_t"),
            targetStyleId = "copperplate",
            recommendedMinutes = 10
        ),
        CurriculumLesson(
            id = "lesson_s2_02_oval_family",
            title = "Família Oval: Letra 'c', 'o' e 'a'",
            description = "Transforme a forma oval elementar em letras completas, conectando o arco da base à saída natural de ligação.",
            stage = CurriculumStage.STAGE_2_FAMILIES,
            type = LessonType.LETTER,
            glyphIds = listOf("letter_c", "letter_o", "letter_a"),
            targetStyleId = "copperplate",
            recommendedMinutes = 10
        ),
        CurriculumLesson(
            id = "lesson_s2_03_loop_family",
            title = "Família Laçada: Letra 'l'",
            description = "Domine a extensão até o espaço ascendente com a letra 'l', garantindo haste paralela às guias de inclinação.",
            stage = CurriculumStage.STAGE_2_FAMILIES,
            type = LessonType.LETTER,
            glyphIds = listOf("letter_l"),
            targetStyleId = "copperplate",
            recommendedMinutes = 10
        ),

        // ==========================================
        // ESTÁGIO 3: CONEXÕES E LIGADURAS
        // ==========================================
        CurriculumLesson(
            id = "lesson_s3_01_conn_it",
            title = "Ligadura Base-Base: 'it'",
            description = "Conexão direta na linha de base saindo do underturn do 'i' para a subida do 't' sem interromper a tinta.",
            stage = CurriculumStage.STAGE_3_CONNECTIONS,
            type = LessonType.CONNECTION,
            glyphIds = listOf("letter_i", "letter_t"),
            targetStyleId = "copperplate",
            recommendedMinutes = 10,
            targetText = "it"
        ),
        CurriculumLesson(
            id = "lesson_s3_02_conn_al",
            title = "Ligadura com Ascendente: 'al'",
            description = "Transição suave da saída do 'a' na linha de base para a laçada alta do 'l'.",
            stage = CurriculumStage.STAGE_3_CONNECTIONS,
            type = LessonType.CONNECTION,
            glyphIds = listOf("letter_a", "letter_l"),
            targetStyleId = "copperplate",
            recommendedMinutes = 10,
            targetText = "al"
        ),
        CurriculumLesson(
            id = "lesson_s3_03_conn_to",
            title = "Ligadura Elevada: 'to'",
            description = "Conexão a partir da barra do 't' em direção ao topo da oval do 'o'.",
            stage = CurriculumStage.STAGE_3_CONNECTIONS,
            type = LessonType.CONNECTION,
            glyphIds = listOf("letter_t", "letter_o"),
            targetStyleId = "copperplate",
            recommendedMinutes = 10,
            targetText = "to"
        ),

        // ==========================================
        // ESTÁGIO 4: PALAVRAS E RITMO
        // ==========================================
        CurriculumLesson(
            id = "lesson_s4_01_word_lua",
            title = "Palavra: 'lua'",
            description = "Pratique a harmonia de três caracteres contínuos combinando laçada alta, underturns e oval final.",
            stage = CurriculumStage.STAGE_4_WORDS,
            type = LessonType.WORD,
            glyphIds = listOf("letter_l", "letter_a"),
            targetStyleId = "cursiva_escolar",
            recommendedMinutes = 15,
            targetText = "lua"
        ),
        CurriculumLesson(
            id = "lesson_s4_02_word_arte",
            title = "Palavra: 'arte'",
            description = "Construção equilibrada de palavra com 4 letras, mantendo inclinação idêntica em todas as hastes verticais.",
            stage = CurriculumStage.STAGE_4_WORDS,
            type = LessonType.WORD,
            glyphIds = listOf("letter_a", "letter_t"),
            targetStyleId = "cursiva_escolar",
            recommendedMinutes = 15,
            targetText = "arte"
        ),
        CurriculumLesson(
            id = "lesson_s4_03_word_calma",
            title = "Palavra: 'calma'",
            description = "Desafio de fluidez motora longa: repetição de formas ovais intercaladas com laçada e conexões múltiplas.",
            stage = CurriculumStage.STAGE_4_WORDS,
            type = LessonType.WORD,
            glyphIds = listOf("letter_c", "letter_a", "letter_l"),
            targetStyleId = "cursiva_escolar",
            recommendedMinutes = 15,
            targetText = "calma"
        ),

        // ==========================================
        // ESTÁGIO 5: FRASES E EXPRESSÕES
        // ==========================================
        CurriculumLesson(
            id = "lesson_s5_01_phrase_curta",
            title = "Expressão: 'arte e calma'",
            description = "Treino de cadência e espaçamento inter-palavras na linha de escrita caligráfica.",
            stage = CurriculumStage.STAGE_5_SENTENCES,
            type = LessonType.SENTENCE,
            glyphIds = listOf("letter_a", "letter_c"),
            targetStyleId = "cursiva_escolar",
            recommendedMinutes = 20,
            targetText = "arte e calma"
        ),
        CurriculumLesson(
            id = "lesson_s5_02_quote",
            title = "Citação: 'o traço revela a alma'",
            description = "Consolidação de fluidez, paciência e respiração rítmica ao longo de uma linha inteira de pauta.",
            stage = CurriculumStage.STAGE_5_SENTENCES,
            type = LessonType.SENTENCE,
            glyphIds = listOf("letter_o", "letter_t", "letter_l", "letter_a"),
            targetStyleId = "copperplate",
            recommendedMinutes = 20,
            targetText = "o traço revela a alma"
        ),
        CurriculumLesson(
            id = "lesson_s5_03_pangram",
            title = "Expressão: 'viva a caligrafia'",
            description = "Fluidez e equilíbrio em frase abrangendo variadas famílias de ductus e ritmo constante.",
            stage = CurriculumStage.STAGE_5_SENTENCES,
            type = LessonType.SENTENCE,
            glyphIds = listOf("letter_a", "letter_l", "letter_i", "letter_c"),
            targetStyleId = "copperplate",
            recommendedMinutes = 20,
            targetText = "viva a caligrafia"
        )
    )

    fun getLessonById(id: String): CurriculumLesson? {
        return ALL_LESSONS.firstOrNull { it.id == id }
    }

    fun getLessonsByStage(stage: CurriculumStage): List<CurriculumLesson> {
        return ALL_LESSONS.filter { it.stage == stage }
    }

    fun defaultFirstLesson(): CurriculumLesson {
        return ALL_LESSONS.first()
    }
}
