package com.scribe.caligrafia.ink.persistence.strategies

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.persistence.StrokePersistenceStrategy
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

/**
 * Estratégia C: Arquivo Binário Standalone por Sessão/Página (`.scribe`).
 *
 * Modela a abordagem orientada a documento vetorial (similar a GoodNotes / Notability / Concepts):
 * - O banco relacional armazena apenas metadados leves (sessão, data, título, caminho do arquivo).
 * - Os traços e amostras vetoriais de alta densidade residem em um arquivo binário dedicado compactado.
 *
 * Características:
 * - Cabeçalho formal com Magic Bytes ("SCRIBE01") e versionamento de schema.
 * - Streaming sequencial puro via buffers sem qualquer overhead de banco de dados.
 * - Isolamento de concorrência e facilidade total de backup, export e migração por arquivo.
 */
class DedicatedFileStrategy(private val baseDir: File) : StrokePersistenceStrategy {

    override val formatName: String = "Arquivo Binário Dedicado (.scribe)"
    override val description: String = "Arquivo vetorial dedicado compactado com magic bytes, streaming sequencial e índice leve."

    init {
        if (!baseDir.exists()) baseDir.mkdirs()
    }

    fun getFile(sessionId: String): File = File(baseDir, "$sessionId.scribe")

    fun save(file: File, strokes: List<Stroke>, sessionId: String = file.nameWithoutExtension): Long {
        val parent = file.parentFile ?: baseDir
        if (!parent.exists()) parent.mkdirs()

        // A02: Gravação atômica em arquivo temporário para evitar destruição de versão anterior em falhas
        val tempFile = File.createTempFile("scribe_${file.nameWithoutExtension}_", ".tmp", parent)
        try {
            FileOutputStream(tempFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    DataOutputStream(bos).use { dos ->
                        // 1. Cabeçalho de arquivo (não-comprimido para rápida identificação de arquivo)
                        dos.write(MAGIC_HEADER)
                        dos.writeShort(SCHEMA_VERSION.toInt())
                        dos.writeUTF(sessionId)
                        dos.writeInt(strokes.size)
                        dos.flush()

                        // 2. Carga útil comprimida com Deflater
                        DeflaterOutputStream(bos).use { deflater ->
                            DataOutputStream(deflater).use { payloadDos ->
                                for (stroke in strokes) {
                                    payloadDos.writeUTF(stroke.id)
                                    payloadDos.writeUTF(stroke.tool.name)
                                    payloadDos.writeLong(stroke.startedAtMs)
                                    payloadDos.writeLong(stroke.endedAtMs)
                                    payloadDos.writeBoolean(stroke.isCancelled)

                                    val hasColor = stroke.color != null
                                    payloadDos.writeBoolean(hasColor)
                                    if (hasColor) payloadDos.writeInt(stroke.color!!)

                                    val hasWidth = stroke.baseWidthPx != null
                                    payloadDos.writeBoolean(hasWidth)
                                    if (hasWidth) payloadDos.writeFloat(stroke.baseWidthPx!!)

                                    payloadDos.writeInt(stroke.points.size)

                                    for (p in stroke.points) {
                                        payloadDos.writeFloat(p.x)
                                        payloadDos.writeFloat(p.y)
                                        payloadDos.writeLong(p.tMs)

                                        var flags = 0
                                        if (p.pressure != null) flags = flags or 0x01
                                        if (p.tiltRad != null) flags = flags or 0x02
                                        if (p.orientationRad != null) flags = flags or 0x04
                                        payloadDos.writeByte(flags)

                                        if (p.pressure != null) payloadDos.writeFloat(p.pressure)
                                        if (p.tiltRad != null) payloadDos.writeFloat(p.tiltRad)
                                        if (p.orientationRad != null) payloadDos.writeFloat(p.orientationRad)
                                    }
                                }
                            }
                        }
                    }
                }
                try {
                    fos.fd.sync()
                } catch (_: Throwable) {
                    // Ignora falha de sync quando o filesystem não suporta sincronização direta no descritor
                }
            }

            try {
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
            }

            return file.length()
        } catch (t: Throwable) {
            tempFile.delete()
            throw t
        }
    }

    override fun save(sessionId: String, strokes: List<Stroke>): Long {
        return save(getFile(sessionId), strokes, sessionId)
    }

    fun load(file: File): List<Stroke> {
        if (!file.exists()) return emptyList()

        val strokes = mutableListOf<Stroke>()

        FileInputStream(file).use { fis ->
            BufferedInputStream(fis).use { bis ->
                DataInputStream(bis).use { dis ->
                    // 1. Validar cabeçalho
                    val magic = ByteArray(MAGIC_HEADER.size)
                    dis.readFully(magic)
                    require(magic.contentEquals(MAGIC_HEADER)) { "Arquivo corrompido ou formato não reconhecido" }

                    val version = dis.readShort().toInt()
                    require(version <= SCHEMA_VERSION) { "Versão de arquivo superior à suportada: $version" }

                    val readSessionId = dis.readUTF()
                    val strokeCount = dis.readInt()

                    // 2. Ler payload comprimido
                    InflaterInputStream(bis).use { inflater ->
                        DataInputStream(inflater).use { payloadDis ->
                            for (i in 0 until strokeCount) {
                                val id = payloadDis.readUTF()
                                val tool = ToolType.valueOf(payloadDis.readUTF())
                                val startedAtMs = payloadDis.readLong()
                                val endedAtMs = payloadDis.readLong()
                                val isCancelled = payloadDis.readBoolean()

                                val color = if (version >= 2) {
                                    val hasColor = payloadDis.readBoolean()
                                    if (hasColor) payloadDis.readInt() else null
                                } else null

                                val baseWidthPx = if (version >= 2) {
                                    val hasWidth = payloadDis.readBoolean()
                                    if (hasWidth) payloadDis.readFloat() else null
                                } else null

                                val pointCount = payloadDis.readInt()

                                val points = ArrayList<StrokePoint>(pointCount)
                                for (j in 0 until pointCount) {
                                    val x = payloadDis.readFloat()
                                    val y = payloadDis.readFloat()
                                    val tMs = payloadDis.readLong()
                                    val flags = payloadDis.readByte().toInt()

                                    val pressure = if (flags and 0x01 != 0) payloadDis.readFloat() else null
                                    val tilt = if (flags and 0x02 != 0) payloadDis.readFloat() else null
                                    val orientation = if (flags and 0x04 != 0) payloadDis.readFloat() else null

                                    points.add(StrokePoint(x, y, tMs, pressure, tilt, orientation))
                                }

                                strokes.add(
                                    Stroke(
                                        id = id,
                                        tool = tool,
                                        points = points,
                                        startedAtMs = startedAtMs,
                                        endedAtMs = endedAtMs,
                                        isCancelled = isCancelled,
                                        color = color,
                                        baseWidthPx = baseWidthPx
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        return strokes
    }

    override fun load(sessionId: String): List<Stroke> {
        return load(getFile(sessionId))
    }

    override fun getStorageSizeBytes(sessionId: String): Long {
        val file = getFile(sessionId)
        return if (file.exists()) file.length() else 0L
    }

    override fun delete(sessionId: String): Boolean {
        return getFile(sessionId).delete()
    }

    companion object {
        val MAGIC_HEADER = "SCRIBE01".toByteArray(Charsets.US_ASCII)
        const val SCHEMA_VERSION: Short = 2
    }
}
