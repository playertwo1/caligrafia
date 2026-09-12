package com.scribe.caligrafia.expansions.passage

/**
 * Catálogo canônico de textos, pangramas e poemas em língua portuguesa
 * para treinamento contínuo de caligrafia formal e cursiva.
 */
object PassageCatalog {

    val allPassages: List<PassageItem> = listOf(
        // 1. Pangramas (exercitam todas as letras do alfabeto)
        PassageItem(
            id = "pangram_morcego",
            title = "O Morcego Veloz",
            author = "Pangrama da Língua Portuguesa",
            category = PassageCategory.PANGRAMS,
            lines = listOf(
                "O veloz morcego negro das asas de veludo",
                "voava sobre a antiga quinta do fazendeiro."
            ),
            recommendedStyleId = "cursiva_escolar_br",
            targetWpm = 16
        ),
        PassageItem(
            id = "pangram_gazeta",
            title = "Gazeta e Xadrez",
            author = "Pangrama Clássico",
            category = PassageCategory.PANGRAMS,
            lines = listOf(
                "Gazeta publica hoje no jornal",
                "breve xadrez de feijão e kiwi."
            ),
            recommendedStyleId = "cursiva_escolar_br",
            targetWpm = 15
        ),
        PassageItem(
            id = "pangram_bancos",
            title = "Bancos e Whisky",
            author = "Pangrama Tipográfico",
            category = PassageCategory.PANGRAMS,
            lines = listOf(
                "Bancos fúteis pagavam queijo",
                "e whisky com suave zumbido."
            ),
            recommendedStyleId = "copperplate_script",
            targetWpm = 12
        ),

        // 2. Poesia Clássica
        PassageItem(
            id = "camoes_amor",
            title = "Amor é Fogo",
            author = "Luís de Camões",
            category = PassageCategory.CLASSIC_POETRY,
            lines = listOf(
                "Amor é um fogo que arde sem se ver,",
                "É ferida que dói, e não se sente;",
                "É um contentamento descontente,",
                "É dor que desatina sem doer."
            ),
            recommendedStyleId = "spencerian_script",
            targetWpm = 11
        ),
        PassageItem(
            id = "machado_carolina",
            title = "A Carolina",
            author = "Machado de Assis",
            category = PassageCategory.CLASSIC_POETRY,
            lines = listOf(
                "Querida, ao pé do leito derradeiro",
                "Em que descansas dessa longa vida,",
                "Aqui venho e virei, pobre querida,",
                "Trazer-te o coração de companheiro."
            ),
            recommendedStyleId = "copperplate_script",
            targetWpm = 10
        ),
        PassageItem(
            id = "pessoa_autopsicografia",
            title = "Autopsicografia",
            author = "Fernando Pessoa",
            category = PassageCategory.CLASSIC_POETRY,
            lines = listOf(
                "O poeta é um fingidor.",
                "Finge tão completamente",
                "Que chega a fingir que é dor",
                "A dor que deveras sente."
            ),
            recommendedStyleId = "cursiva_escolar_br",
            targetWpm = 14
        ),

        // 3. Citações e Filosofia da Arte
        PassageItem(
            id = "quote_musica_visivel",
            title = "Música Visível",
            author = "Tradição Caligráfica",
            category = PassageCategory.FAMOUS_QUOTES,
            lines = listOf(
                "A caligrafia é a música visível da alma,",
                "onde cada traço é uma nota de silêncio e harmonia."
            ),
            recommendedStyleId = "copperplate_script",
            targetWpm = 12
        ),
        PassageItem(
            id = "quote_paciencia",
            title = "A Dança da Caneta",
            author = "Mestres de pena flexível",
            category = PassageCategory.FAMOUS_QUOTES,
            lines = listOf(
                "A paciência na ponta da pena revela a postura serena da mente.",
                "Quem domina a respiração, domina o fio de tinta."
            ),
            recommendedStyleId = "spencerian_script",
            targetWpm = 10
        )
    )

    fun getById(id: String): PassageItem? {
        return allPassages.find { it.id == id }
    }

    fun getByCategory(category: PassageCategory): List<PassageItem> {
        return allPassages.filter { it.category == category }
    }
}
