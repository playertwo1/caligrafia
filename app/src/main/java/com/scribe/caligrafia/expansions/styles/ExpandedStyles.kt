package com.scribe.caligrafia.expansions.styles

import com.scribe.caligrafia.core.model.GuidelineRatio
import com.scribe.caligrafia.style.model.DuctusRule
import com.scribe.caligrafia.style.model.PressureBehavior
import com.scribe.caligrafia.style.model.ScribeStyle
import com.scribe.caligrafia.style.model.StyleCategory

/**
 * Catálogo canônico de estilos históricos expandidos introduzidos no Milestone M8.
 */
object ExpandedStyles {

    /**
     * Gótica Textura Quadrata (século XII–XIV)
     */
    val GOTICA_TEXTURA = ScribeStyle(
        id = "gotica_textura",
        name = "Gótica Textura Quadrata",
        description = "Estilo medieval do século XII-XIV com traços verticais densos, quebras angulares a 45°, pés quadrangulares e ritmo compacto evocando uma cerca militar teutônica.",
        category = StyleCategory.CALLIGRAPHIC,
        recommendedRatio = GuidelineRatio.Ratio212,
        defaultSlantAngle = 90.0f, // 90° rigorosamente vertical
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
     * Itálica Chanceleresca (Cancelleresca Romana, século XV–XVI)
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
     * Uncial Clássica (século IV–VIII)
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

    val allExpandedStyles: List<ScribeStyle> = listOf(
        GOTICA_TEXTURA,
        ITALICA_CHANCELERESCA,
        UNCIAL_CLASSICA
    )
}
