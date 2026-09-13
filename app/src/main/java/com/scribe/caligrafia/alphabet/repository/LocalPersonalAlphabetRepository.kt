package com.scribe.caligrafia.alphabet.repository

import com.scribe.caligrafia.alphabet.engine.PersonalStyleCompiler
import com.scribe.caligrafia.alphabet.model.AlphabetCategory
import com.scribe.caligrafia.alphabet.model.GlyphVariant
import com.scribe.caligrafia.alphabet.model.PersonalAlphabet
import com.scribe.caligrafia.alphabet.model.PersonalGlyph
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import com.scribe.caligrafia.style.engine.PersonalStyleSerializer
import com.scribe.caligrafia.style.model.ScribeStyle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementação local do repositório de Alfabeto Pessoal (SCR-603 — M6).
 *
 * Características:
 * 1. Gravação atômica (.tmp + ATOMIC_MOVE) do manifesto central [personal_alphabet_manifest.json].
 * 2. Armazenamento vetorial imutável das variantes em arquivos compactados [.scribe] via [DedicatedFileStrategy].
 * 3. Cache em memória thread-safe e reativo via [MutableStateFlow].
 * 4. Pré-carga canônica dos 68 caracteres e ligaduras essenciais com sementes pedagógicas.
 */
class LocalPersonalAlphabetRepository(
    private val baseDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val styleCompiler: PersonalStyleCompiler = PersonalStyleCompiler()
) : PersonalAlphabetRepository {

    private val strokesDir = File(baseDir, "strokes")
    private val manifestFile = File(baseDir, "personal_alphabet_manifest.json")
    private val strokePersistence = DedicatedFileStrategy(strokesDir)
    private val mutationMutex = Mutex()

    private val alphabetFlow = MutableStateFlow(PersonalAlphabet())
    private val strokeCache = ConcurrentHashMap<String, List<Stroke>>()

    init {
        if (!baseDir.exists()) baseDir.mkdirs()
        if (!strokesDir.exists()) strokesDir.mkdirs()
        loadOrInitializeAlphabet()
    }

    override fun getAlphabet(): Flow<PersonalAlphabet> = alphabetFlow.asStateFlow()

    override suspend fun getGlyph(glyphId: String): PersonalGlyph? = withContext(ioDispatcher) {
        alphabetFlow.value.glyphs[glyphId]
    }

    override suspend fun getVariantStrokes(variantId: String): List<Stroke> = withContext(ioDispatcher) {
        val cached = strokeCache[variantId]
        if (cached != null) return@withContext cached

        val file = strokePersistence.getFile(variantId)
        if (file.exists()) {
            val loaded = strokePersistence.load(file)
            strokeCache[variantId] = loaded
            return@withContext loaded
        }
        emptyList()
    }

    override suspend fun addVariant(
        glyphId: String,
        strokes: List<Stroke>,
        score: Float,
        slantAngle: Float,
        setFavorite: Boolean,
        sourceAttemptId: String?
    ): GlyphVariant = withContext(ioDispatcher) {
        mutationMutex.withLock {
            val currentAlphabet = alphabetFlow.value
            val existingGlyph = currentAlphabet.glyphs[glyphId]
                ?: createDefaultGlyph(glyphId)

            val nextVersion = (existingGlyph.variants.maxOfOrNull { it.version } ?: 0) + 1
            val variantId = "var_${glyphId}_v${nextVersion}_${UUID.randomUUID()}"

            // Salva os traços vetoriais em .scribe
            strokePersistence.save(variantId, strokes)
            strokeCache[variantId] = strokes

            val newVariant = GlyphVariant(
                id = variantId,
                glyphId = glyphId,
                version = nextVersion,
                label = "v$nextVersion",
                score = score,
                slantAngleDegrees = slantAngle,
                isFavorite = setFavorite || existingGlyph.variants.isEmpty(),
                strokes = strokes,
                strokeCount = strokes.size,
                createdAtTimestamp = System.currentTimeMillis(),
                sourceAttemptId = sourceAttemptId
            )

            val updatedVariants = existingGlyph.variants.map { v ->
                if (setFavorite) v.copy(isFavorite = false) else v
            } + newVariant

            val updatedGlyph = existingGlyph.copy(
                selectedVariantId = if (setFavorite || existingGlyph.selectedVariantId == null) variantId else existingGlyph.selectedVariantId,
                variants = updatedVariants,
                bestScore = updatedVariants.maxOfOrNull { it.score } ?: 0f,
                updatedAtTimestamp = System.currentTimeMillis()
            )

            val updatedMap = currentAlphabet.glyphs.toMutableMap()
            updatedMap[glyphId] = updatedGlyph

            val newAlphabet = currentAlphabet.copy(glyphs = updatedMap)
            saveAlphabetManifest(newAlphabet)
            alphabetFlow.value = newAlphabet

            newVariant
        }
    }

    override suspend fun setFavoriteVariant(glyphId: String, variantId: String): Boolean = withContext(ioDispatcher) {
        mutationMutex.withLock {
            val currentAlphabet = alphabetFlow.value
            val glyph = currentAlphabet.glyphs[glyphId] ?: return@withLock false

            val variantExists = glyph.variants.any { it.id == variantId }
            if (!variantExists) return@withLock false

            val updatedVariants = glyph.variants.map { v ->
                v.copy(isFavorite = v.id == variantId)
            }

            val updatedGlyph = glyph.copy(
                selectedVariantId = variantId,
                variants = updatedVariants,
                updatedAtTimestamp = System.currentTimeMillis()
            )

            val updatedMap = currentAlphabet.glyphs.toMutableMap()
            updatedMap[glyphId] = updatedGlyph

            val newAlphabet = currentAlphabet.copy(glyphs = updatedMap)
            saveAlphabetManifest(newAlphabet)
            alphabetFlow.value = newAlphabet
            true
        }
    }

    override suspend fun deleteVariant(glyphId: String, variantId: String): Boolean = withContext(ioDispatcher) {
        mutationMutex.withLock {
            val currentAlphabet = alphabetFlow.value
            val glyph = currentAlphabet.glyphs[glyphId] ?: return@withLock false

            val variantToDelete = glyph.variants.firstOrNull { it.id == variantId } ?: return@withLock false
            val remainingVariants = glyph.variants.filter { it.id != variantId }

            val newSelectedId = if (glyph.selectedVariantId == variantId) {
                remainingVariants.firstOrNull { it.isFavorite }?.id ?: remainingVariants.lastOrNull()?.id
            } else {
                glyph.selectedVariantId
            }

            val updatedGlyph = glyph.copy(
                selectedVariantId = newSelectedId,
                variants = remainingVariants,
                bestScore = remainingVariants.maxOfOrNull { it.score } ?: 0f,
                updatedAtTimestamp = System.currentTimeMillis()
            )

            val updatedMap = currentAlphabet.glyphs.toMutableMap()
            updatedMap[glyphId] = updatedGlyph

            val newAlphabet = currentAlphabet.copy(glyphs = updatedMap)
            // R10: Salva o manifesto atômico primeiro antes de deletar o arquivo físico
            saveAlphabetManifest(newAlphabet)
            alphabetFlow.value = newAlphabet

            // Remove arquivo .scribe associado apenas após o manifesto salvo com sucesso
            val scribeFile = strokePersistence.getFile(variantId)
            if (scribeFile.exists()) scribeFile.delete()
            strokeCache.remove(variantId)

            true
        }
    }

    override suspend fun compileAndSavePersonalStyle(styleName: String?): ScribeStyle = withContext(ioDispatcher) {
        mutationMutex.withLock {
            val currentAlphabet = alphabetFlow.value
            val name = styleName ?: "Meu Estilo Pessoal"
            val styleId = "personal_style_${System.currentTimeMillis()}"

            // Garante que traços das variantes ativas estejam disponíveis para cálculo
            val populatedAlphabet = populateActiveVariantStrokes(currentAlphabet)

            val style = styleCompiler.compile(
                alphabet = populatedAlphabet,
                styleName = name,
                styleId = styleId
            )

            val updatedAlphabet = currentAlphabet.copy(
                lastCompiledStyleId = styleId,
                lastCompiledTimestamp = System.currentTimeMillis()
            )

            saveAlphabetManifest(updatedAlphabet)
            alphabetFlow.value = updatedAlphabet

            // Persiste o estilo pessoal compilado em personal_styles.json para descoberta imediata pelo StyleEngine
            val targetDirs = listOfNotNull(
                baseDir,
                baseDir.parentFile
            ).distinct()

            for (targetDir in targetDirs) {
                try {
                    val stylesFile = File(targetDir, "personal_styles.json")
                    val existingStyles = if (stylesFile.exists()) {
                        PersonalStyleSerializer.deserialize(stylesFile.readText(Charsets.UTF_8)).filter { it.id != style.id }
                    } else {
                        emptyList()
                    }
                    val parent = stylesFile.parentFile
                    if (parent != null && !parent.exists()) parent.mkdirs()
                    stylesFile.writeText(PersonalStyleSerializer.serialize(existingStyles + style), Charsets.UTF_8)
                } catch (_: Throwable) {
                    // Falha de escrita de cache de estilos não quebra o fluxo de compilação
                }
            }

            style
        }
    }

    private fun loadOrInitializeAlphabet() {
        if (manifestFile.exists()) {
            try {
                val json = manifestFile.readText(Charsets.UTF_8)
                val loaded = PersonalAlphabetSerializer.deserialize(json)
                // Garante que novos glifos canônicos sejam adicionados se o manifesto for de versão anterior
                val canonical = generateCanonicalGlyphs()
                val mergedMap = canonical.toMutableMap()
                loaded.glyphs.forEach { (id, glyph) ->
                    mergedMap[id] = glyph
                }
                val fullAlphabet = loaded.copy(glyphs = mergedMap)
                alphabetFlow.value = fullAlphabet
                return
            } catch (_: Throwable) {
                // Se o arquivo estiver corrompido, recria canonicamente
            }
        }

        val freshAlphabet = createInitialSeedAlphabet()
        saveAlphabetManifest(freshAlphabet)
        alphabetFlow.value = freshAlphabet
    }

    private fun saveAlphabetManifest(alphabet: PersonalAlphabet) {
        val parent = manifestFile.parentFile ?: baseDir
        if (!parent.exists()) parent.mkdirs()

        val tempFile = File.createTempFile("alphabet_manifest_", ".tmp", parent)
        try {
            val json = PersonalAlphabetSerializer.serialize(alphabet)
            FileOutputStream(tempFile).use { fos ->
                fos.write(json.toByteArray(Charsets.UTF_8))
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (_: Throwable) {}
            }
            Files.move(
                tempFile.toPath(),
                manifestFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun populateActiveVariantStrokes(alphabet: PersonalAlphabet): PersonalAlphabet {
        val updatedGlyphs = alphabet.glyphs.mapValues { (_, glyph) ->
            val active = glyph.activeVariant
            if (active != null && active.strokes.isEmpty()) {
                val loadedStrokes = getVariantStrokes(active.id)
                val populatedVariant = active.copy(strokes = loadedStrokes)
                val updatedVariants = glyph.variants.map { v ->
                    if (v.id == active.id) populatedVariant else v
                }
                glyph.copy(variants = updatedVariants)
            } else {
                glyph
            }
        }
        return alphabet.copy(glyphs = updatedGlyphs)
    }

    private fun createInitialSeedAlphabet(): PersonalAlphabet {
        val canonical = generateCanonicalGlyphs()
        return PersonalAlphabet(glyphs = canonical)
    }

    private fun generateCanonicalGlyphs(): MutableMap<String, PersonalGlyph> {
        val map = mutableMapOf<String, PersonalGlyph>()

        // 1. Minúsculas (a-z)
        for (ch in 'a'..'z') {
            val id = "glyph_lower_$ch"
            map[id] = PersonalGlyph(
                id = id,
                symbol = ch.toString(),
                name = "Letra '$ch'",
                category = AlphabetCategory.LOWERCASE
            )
        }

        // 2. Maiúsculas (A-Z)
        for (ch in 'A'..'Z') {
            val id = "glyph_upper_$ch"
            map[id] = PersonalGlyph(
                id = id,
                symbol = ch.toString(),
                name = "Letra '$ch'",
                category = AlphabetCategory.UPPERCASE
            )
        }

        // 3. Algarismos (0-9)
        for (ch in '0'..'9') {
            val id = "glyph_digit_$ch"
            map[id] = PersonalGlyph(
                id = id,
                symbol = ch.toString(),
                name = "Dígito '$ch'",
                category = AlphabetCategory.NUMBER
            )
        }

        // 4. Conexões e Símbolos
        val connectors = listOf(
            Triple("glyph_conn_it", "it", "Ligadura 'it'"),
            Triple("glyph_conn_al", "al", "Ligadura 'al'"),
            Triple("glyph_conn_to", "to", "Ligadura 'to'"),
            Triple("glyph_sym_amp", "&", "E comercial (&)"),
            Triple("glyph_sym_quest", "?", "Interrogação (?)"),
            Triple("glyph_sym_excl", "!", "Exclamação (!)")
        )
        for ((id, sym, name) in connectors) {
            map[id] = PersonalGlyph(
                id = id,
                symbol = sym,
                name = name,
                category = AlphabetCategory.CONNECTOR
            )
        }

        return map
    }

    private fun createDefaultGlyph(glyphId: String): PersonalGlyph {
        val symbol = glyphId.substringAfterLast("_")
        return PersonalGlyph(
            id = glyphId,
            symbol = symbol,
            name = "Glifo $symbol",
            category = AlphabetCategory.LOWERCASE
        )
    }

    private fun createSampleVariant(
        glyphId: String,
        version: Int,
        score: Float,
        slant: Float,
        strokes: List<Stroke>
    ): GlyphVariant {
        val id = "var_${glyphId}_v${version}_sample"
        return GlyphVariant(
            id = id,
            glyphId = glyphId,
            version = version,
            label = "v$version",
            score = score,
            slantAngleDegrees = slant,
            isFavorite = true,
            strokes = strokes,
            strokeCount = strokes.size,
            createdAtTimestamp = System.currentTimeMillis() - (86400000L * version)
        )
    }

    private fun generateLetterAStrokes(): List<Stroke> {
        val stroke1 = Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(70f, 30f, 1000L, 0.4f, 0f, 0f),
                StrokePoint(40f, 50f, 1050L, 0.5f, 0f, 0f),
                StrokePoint(30f, 70f, 1100L, 0.6f, 0f, 0f),
                StrokePoint(50f, 90f, 1150L, 0.5f, 0f, 0f),
                StrokePoint(70f, 70f, 1200L, 0.4f, 0f, 0f),
                StrokePoint(70f, 30f, 1250L, 0.3f, 0f, 0f)
            ),
            startedAtMs = 1000L,
            endedAtMs = 1250L,
            baseWidthPx = 5.0f
        )
        val stroke2 = Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(70f, 30f, 1300L, 0.5f, 0f, 0f),
                StrokePoint(72f, 60f, 1350L, 0.7f, 0f, 0f),
                StrokePoint(75f, 85f, 1400L, 0.6f, 0f, 0f),
                StrokePoint(85f, 90f, 1450L, 0.3f, 0f, 0f)
            ),
            startedAtMs = 1300L,
            endedAtMs = 1450L,
            baseWidthPx = 5.0f
        )
        return listOf(stroke1, stroke2)
    }

    private fun generateLetterLStrokes(): List<Stroke> {
        return listOf(
            Stroke(
                id = UUID.randomUUID().toString(),
                tool = ToolType.STYLUS,
                points = listOf(
                    StrokePoint(30f, 90f, 1000L, 0.2f, 0f, 0f),
                    StrokePoint(50f, 50f, 1050L, 0.3f, 0f, 0f),
                    StrokePoint(65f, 15f, 1100L, 0.3f, 0f, 0f),
                    StrokePoint(60f, 10f, 1120L, 0.3f, 0f, 0f),
                    StrokePoint(50f, 20f, 1150L, 0.6f, 0f, 0f),
                    StrokePoint(55f, 60f, 1200L, 0.8f, 0f, 0f),
                    StrokePoint(60f, 90f, 1250L, 0.6f, 0f, 0f),
                    StrokePoint(75f, 88f, 1300L, 0.3f, 0f, 0f)
                ),
                startedAtMs = 1000L,
                endedAtMs = 1300L,
                baseWidthPx = 5.0f
            )
        )
    }

    private fun generateLetterIStrokes(): List<Stroke> {
        val main = Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(30f, 60f, 1000L, 0.3f, 0f, 0f),
                StrokePoint(45f, 35f, 1050L, 0.4f, 0f, 0f),
                StrokePoint(48f, 65f, 1100L, 0.7f, 0f, 0f),
                StrokePoint(52f, 85f, 1150L, 0.6f, 0f, 0f),
                StrokePoint(65f, 85f, 1200L, 0.3f, 0f, 0f)
            ),
            startedAtMs = 1000L,
            endedAtMs = 1200L,
            baseWidthPx = 5.0f
        )
        val dot = Stroke(
            id = UUID.randomUUID().toString(),
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(46f, 20f, 1250L, 0.5f, 0f, 0f),
                StrokePoint(47f, 21f, 1270L, 0.5f, 0f, 0f)
            ),
            startedAtMs = 1250L,
            endedAtMs = 1270L,
            baseWidthPx = 4.0f
        )
        return listOf(main, dot)
    }

    private fun generateConnectorITStrokes(): List<Stroke> {
        return listOf(
            Stroke(
                id = UUID.randomUUID().toString(),
                tool = ToolType.STYLUS,
                points = listOf(
                    StrokePoint(20f, 60f, 1000L, 0.3f, 0f, 0f),
                    StrokePoint(35f, 35f, 1050L, 0.5f, 0f, 0f),
                    StrokePoint(38f, 75f, 1100L, 0.7f, 0f, 0f),
                    StrokePoint(50f, 50f, 1150L, 0.4f, 0f, 0f),
                    StrokePoint(65f, 20f, 1200L, 0.6f, 0f, 0f),
                    StrokePoint(68f, 75f, 1250L, 0.7f, 0f, 0f),
                    StrokePoint(80f, 80f, 1300L, 0.3f, 0f, 0f)
                ),
                startedAtMs = 1000L,
                endedAtMs = 1300L,
                baseWidthPx = 5.0f
            ),
            Stroke(
                id = UUID.randomUUID().toString(),
                tool = ToolType.STYLUS,
                points = listOf(
                    StrokePoint(55f, 40f, 1350L, 0.4f, 0f, 0f),
                    StrokePoint(75f, 40f, 1400L, 0.4f, 0f, 0f)
                ),
                startedAtMs = 1350L,
                endedAtMs = 1400L,
                baseWidthPx = 3.5f
            )
        )
    }
}
