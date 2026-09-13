package com.scribe.caligrafia.style.model

import com.scribe.caligrafia.core.model.GuidelineRatio

/**
 * Catálogo canônico dos três estilos pedagógicos iniciais do Scribe (SCR-021).
 *
 * Cada estilo possui sua fundamentação histórica, proporções matemáticas precisas
 * e regras de ductus para treino no canvas.
 */
object BuiltInStyles {

    /**
     * 1. Cursiva Escolar Brasileira
     * Foco em legibilidade e fluidez motora no aprendizado básico.
     * Pauta com proporção uniforme (1:1:1), inclinação suave de 68° e traço de espessura constante.
     */
    val CURSIVA_ESCOLAR = ScribeStyle(
        id = "cursiva_escolar",
        name = "Cursiva Escolar Brasileira",
        description = "Caligrafia escolar baseada em proporção uniforme (1:1:1), inclinação suave de 68° e traço contínuo com pressão homogênea. Ideal para fluidez e clareza na escrita diária.",
        category = StyleCategory.FOUNDATIONAL,
        recommendedRatio = GuidelineRatio.Ratio111,
        defaultSlantAngle = 68.0f,
        recommendedStrokeWidthPx = 4.0f,
        contrastRatio = 1.0f,
        sampleAlphabet = "Aa Bb Cc Dd Ee Ff Gg Hh Ii Jj Kk Ll Mm Nn Oo Pp Qq Rr Ss Tt Uu Vv Ww Xx Yy Zz",
        ductusRules = listOf(
            DuctusRule(
                ruleIndex = 1,
                title = "Continuidade e Conexões",
                instruction = "Mantenha o traço contínuo entre letras na mesma palavra, com pressão suave e uniforme.",
                pressureBehavior = PressureBehavior.UNIFORM
            ),
            DuctusRule(
                ruleIndex = 2,
                title = "Arco Superior e Inferior",
                instruction = "Arredonde as curvas no teto da altura-x e na linha de base sem criar cantos pontiagudos.",
                pressureBehavior = PressureBehavior.UNIFORM
            ),
            DuctusRule(
                ruleIndex = 3,
                title = "Inclinação Homogênea",
                instruction = "Mantenha todas as hastes descendentes paralelas às linhas de inclinação de 68°.",
                pressureBehavior = PressureBehavior.UNIFORM
            )
        )
    )

    /**
     * 2. Copperplate (English Roundhand)
     * Caligrafia clássica dos séculos XVII e XVIII.
     * Proporção clássica 3:2:3, inclinação rigorosa de 52° e forte modulação de espessura por pressão.
     */
    val COPPERPLATE = ScribeStyle(
        id = "copperplate",
        name = "Copperplate (English Roundhand)",
        description = "Estilo caligráfico clássico caracterizado por proporção 3:2:3, inclinação estrita de 52° e forte contraste de pressão: descidas espessas e subidas capilares ultra-finas.",
        category = StyleCategory.CALLIGRAPHIC,
        recommendedRatio = GuidelineRatio.Ratio323,
        defaultSlantAngle = 52.0f,
        recommendedStrokeWidthPx = 4.5f,
        contrastRatio = 2.8f,
        sampleAlphabet = "Aa Bb Cc Dd Ee Ff Gg Hh Ii Jj Kk Ll Mm Nn Oo Pp Qq Rr Ss Tt Uu Vv Ww Xx Yy Zz",
        ductusRules = listOf(
            DuctusRule(
                ruleIndex = 1,
                title = "Contraste de Pressão",
                instruction = "Aplique pressão firme nos traços descendentes (downstrokes) e libere toda a pressão nas subidas (upstrokes capilares).",
                pressureBehavior = PressureBehavior.DOWNSTROKE_HEAVY
            ),
            DuctusRule(
                ruleIndex = 2,
                title = "Ângulo Rígido de 52°",
                instruction = "Cada traço descendente reto deve seguir estritamente a linha de inclinação de 52°.",
                pressureBehavior = PressureBehavior.DOWNSTROKE_HEAVY
            ),
            DuctusRule(
                ruleIndex = 3,
                title = "Desconstrução em Traços Básicos",
                instruction = "Construa as letras combinando formas elementares: traço reto, underturn, overturn e ovais.",
                pressureBehavior = PressureBehavior.DOWNSTROKE_HEAVY
            )
        )
    )

    /**
     * 3. Spencerian Script
     * Escrita americana veloz e ornamental do século XIX.
     * Proporção esguia 2:1:2, inclinação de 52°, traçado predominantemente fino com sombras seletivas.
     */
    val SPENCERIAN = ScribeStyle(
        id = "spencerian",
        name = "Spencerian Script",
        description = "Escrita artística e rápida do século XIX. Caracteriza-se por altura-x delicada e proporcional (2:1:2), inclinação de 52° e traços predominantemente capilares com sombras de pressão reservadas a pontos de ênfase.",
        category = StyleCategory.ORNAMENTAL,
        recommendedRatio = GuidelineRatio.Ratio212,
        defaultSlantAngle = 52.0f,
        recommendedStrokeWidthPx = 3.0f,
        contrastRatio = 1.8f,
        sampleAlphabet = "Aa Bb Cc Dd Ee Ff Gg Hh Ii Jj Kk Ll Mm Nn Oo Pp Qq Rr Ss Tt Uu Vv Ww Xx Yy Zz",
        ductusRules = listOf(
            DuctusRule(
                ruleIndex = 1,
                title = "Toque Leve e Fluido",
                instruction = "Pratique com o mínimo de pressão na maior parte da escrita, focando na velocidade e precisão do movimento do braço.",
                pressureBehavior = PressureBehavior.SHADED_ACCENT
            ),
            DuctusRule(
                ruleIndex = 2,
                title = "Sombreamento Pontual",
                instruction = "Adicione pressão somente no centro das letras maiúsculas e em descidas selecionadas (ex: 't', 'd', 'p').",
                pressureBehavior = PressureBehavior.SHADED_ACCENT
            ),
            DuctusRule(
                ruleIndex = 3,
                title = "Curvatura Elíptica",
                instruction = "As curvas seguem elipses alongadas na inclinação de 52°, gerando elegância e dinamismo.",
                pressureBehavior = PressureBehavior.SHADED_ACCENT
            )
        )
    )

    /**
     * 4. Gótica Textura Quadrata (século XII–XIV)
     * Estilo medieval com traços verticais densos, quebras angulares a 45° e pés quadrangulares.
     */
    val GOTICA_TEXTURA = ScribeStyle(
        id = "gotica_textura",
        name = "Gótica Textura Quadrata",
        description = "Estilo medieval do século XII-XIV com traços verticais densos, quebras angulares a 45°, pés quadrangulares e ritmo compacto evocando uma cerca militar teutônica.",
        category = StyleCategory.CALLIGRAPHIC,
        recommendedRatio = GuidelineRatio.Ratio212,
        defaultSlantAngle = 90.0f,
        recommendedStrokeWidthPx = 6.0f,
        contrastRatio = 3.5f,
        sampleAlphabet = "Aa Bb Cc Dd Ee Ff Gg Hh Ii Jj Kk Ll Mm Nn Oo Pp Qq Rr Ss Tt Uu Vv Ww Xx Yy Zz",
        ductusRules = listOf(
            DuctusRule(
                ruleIndex = 1,
                title = "Ângulo Fixo da Pena (45°)",
                instruction = "Mantenha o ângulo de ataque da pena rigorosamente a 45 graus para gerar traços verticais largos e quebras capilares finas.",
                pressureBehavior = PressureBehavior.DOWNSTROKE_HEAVY
            ),
            DuctusRule(
                ruleIndex = 2,
                title = "Hastes Verticais Severas",
                instruction = "Todas as hastes principais descem perfeitamente a 90°, paralelas e equidistantes.",
                pressureBehavior = PressureBehavior.DOWNSTROKE_HEAVY
            ),
            DuctusRule(
                ruleIndex = 3,
                title = "Pés Quadrangulares (Quadrata)",
                instruction = "Finalize cada haste com uma quebra angular curta a 45° para a direita, formando o pé característico.",
                pressureBehavior = PressureBehavior.DOWNSTROKE_HEAVY
            )
        )
    )

    /**
     * 5. Itálica Chanceleresca (século XV–XVI)
     * Caligrafia renascentista com leve inclinação de 85° (5° para frente) e ritmo ágil de pena chata.
     */
    val ITALICA_CHANCELERESCA = ScribeStyle(
        id = "italica_chanceleresca",
        name = "Itálica Chanceleresca",
        description = "Caligrafia renascentista desenvolvida na chancelaria papal. Leve inclinação de 85° (5° para frente), ritmo ágil de pena chata e máxima elegância e legibilidade.",
        category = StyleCategory.CALLIGRAPHIC,
        recommendedRatio = GuidelineRatio.Ratio323,
        defaultSlantAngle = 85.0f,
        recommendedStrokeWidthPx = 4.5f,
        contrastRatio = 2.4f,
        sampleAlphabet = "Aa Bb Cc Dd Ee Ff Gg Hh Ii Jj Kk Ll Mm Nn Oo Pp Qq Rr Ss Tt Uu Vv Ww Xx Yy Zz",
        ductusRules = listOf(
            DuctusRule(
                ruleIndex = 1,
                title = "Inclinação Rítmica Suave",
                instruction = "Mantenha uma inclinação constante de 5 graus (85° na pauta) em todas as hastes.",
                pressureBehavior = PressureBehavior.UNIFORM
            ),
            DuctusRule(
                ruleIndex = 2,
                title = "Entradas e Saídas em Arco",
                instruction = "Conecte as letras com diagonais finas a 45°, mantendo os ovais elípticos e arejados.",
                pressureBehavior = PressureBehavior.UNIFORM
            ),
            DuctusRule(
                ruleIndex = 3,
                title = "Ascendentes com Serifas Triangulares",
                instruction = "Inicie hastes como 'b', 'd', 'h' e 'l' com um pequeno engaste triangular superior.",
                pressureBehavior = PressureBehavior.DOWNSTROKE_HEAVY
            )
        )
    )

    /**
     * 6. Uncial Clássica (século IV–VIII)
     * Escrita monástica da Antiguidade Tardia, com maiúsculas arredondadas contínuas e traço vertical a 90°.
     */
    val UNCIAL_CLASSICA = ScribeStyle(
        id = "uncial_classica",
        name = "Uncial Clássica",
        description = "Escrita majestosa monástica da Antiguidade Tardia, caracterizada por maiúsculas arredondadas contínuas ('A', 'D', 'E', 'M'), traço vertical e presença serena.",
        category = StyleCategory.FOUNDATIONAL,
        recommendedRatio = GuidelineRatio.Ratio111,
        defaultSlantAngle = 90.0f,
        recommendedStrokeWidthPx = 5.0f,
        contrastRatio = 2.0f,
        sampleAlphabet = "A B C D E F G H I K L M N O P Q R S T V X Y Z",
        ductusRules = listOf(
            DuctusRule(
                ruleIndex = 1,
                title = "Construção Circular",
                instruction = "Trace letras como 'C', 'E', 'O', 'Q' com curvas circulares plenas, sem deformação ovalada.",
                pressureBehavior = PressureBehavior.UNIFORM
            ),
            DuctusRule(
                ruleIndex = 2,
                title = "Ausência de Haste Excessiva",
                instruction = "As hastes superiores e inferiores permanecem contidas próximas da pauta principal.",
                pressureBehavior = PressureBehavior.UNIFORM
            )
        )
    )

    /** Lista imutável de todos os 6 estilos canônicos do Scribe (F3.19). */
    val ALL: List<ScribeStyle> = listOf(
        CURSIVA_ESCOLAR,
        COPPERPLATE,
        SPENCERIAN,
        GOTICA_TEXTURA,
        ITALICA_CHANCELERESCA,
        UNCIAL_CLASSICA
    )
}
