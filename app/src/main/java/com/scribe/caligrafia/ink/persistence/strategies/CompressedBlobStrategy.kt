package com.scribe.caligrafia.ink.persistence.strategies

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.persistence.StrokePersistenceStrategy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

/**
 * Estratégia B: Blob Binário Compactado no Registro do Traço (Híbrido Room/Blob).
 *
 * Modela a abordagem onde metadados relacionais (id, timestamps, ferramentas) ficam
 * nas colunas do banco, mas a lista de alta frequência de pontos é empacotada em
 * uma coluna BLOB comprimida com schemaVersion e flags de presença de sensores.
 *
 * Características:
 * - Reduz radicalmente a quantidade de linhas no banco (1 linha por traço em vez de 1 linha por ponto).
 * - Compactação Deflate/GZIP reduz em até 70-80% o tamanho dos pontos em disco.
 * - Suporte nativo a schemaVersion por blob.
 */
class CompressedBlobStrategy(private val baseDir: File) : StrokePersistenceStrategy {

    override val formatName: String = "Blob Binário Compactado (Room/BLOB)"
    override val description: String = "Metadados de traço relacionais com pontos serializados e comprimidos em coluna BLOB."

    init {
        if (!baseDir.exists()) baseDir.mkdirs()
    }

    private fun getStoreFile(sessionId: String): File =
        File(baseDir, "${sessionId}_strokes_blobs.bin")

    override fun save(sessionId: String, strokes: List<Stroke>): Long {
        val file = getStoreFile(sessionId)
        DataOutputStream(FileOutputStream(file)).use { dos ->
            dos.writeInt(SCHEMA_VERSION)
            dos.writeInt(strokes.size)

            for (stroke in strokes) {
                dos.writeUTF(stroke.id)
                dos.writeUTF(stroke.tool.name)
                dos.writeLong(stroke.startedAtMs)
                dos.writeLong(stroke.endedAtMs)
                dos.writeBoolean(stroke.isCancelled)

                // Serializar e comprimir os pontos em BLOB
                val blobBytes = serializeAndCompressPoints(stroke.points)
                dos.writeInt(blobBytes.size)
                dos.write(blobBytes)
            }
        }
        return file.length()
    }

    override fun load(sessionId: String): List<Stroke> {
        val file = getStoreFile(sessionId)
        if (!file.exists()) return emptyList()

        val strokes = mutableListOf<Stroke>()
        DataInputStream(FileInputStream(file)).use { dis ->
            val version = dis.readInt()
            require(version <= SCHEMA_VERSION) { "Versão de schema de blob não suportada: $version" }

            val count = dis.readInt()
            for (i in 0 until count) {
                val id = dis.readUTF()
                val tool = ToolType.valueOf(dis.readUTF())
                val startedAtMs = dis.readLong()
                val endedAtMs = dis.readLong()
                val isCancelled = dis.readBoolean()

                val blobSize = dis.readInt()
                val blobBytes = ByteArray(blobSize)
                dis.readFully(blobBytes)

                val points = decompressAndDeserializePoints(blobBytes)

                strokes.add(
                    Stroke(
                        id = id,
                        tool = tool,
                        points = points,
                        startedAtMs = startedAtMs,
                        endedAtMs = endedAtMs,
                        isCancelled = isCancelled
                    )
                )
            }
        }
        return strokes
    }

    override fun getStorageSizeBytes(sessionId: String): Long {
        val file = getStoreFile(sessionId)
        return if (file.exists()) file.length() else 0L
    }

    override fun delete(sessionId: String): Boolean {
        return getStoreFile(sessionId).delete()
    }

    private fun serializeAndCompressPoints(points: List<StrokePoint>): ByteArray {
        val byteOut = ByteArrayOutputStream()
        DeflaterOutputStream(byteOut).use { deflaterStream ->
            DataOutputStream(deflaterStream).use { dos ->
                dos.writeByte(BLOB_MAGIC.toInt())
                dos.writeByte(1) // sub-version
                dos.writeInt(points.size)

                for (p in points) {
                    dos.writeFloat(p.x)
                    dos.writeFloat(p.y)
                    dos.writeLong(p.tMs)

                    // Flags de sensores (1 byte)
                    var flags = 0
                    if (p.pressure != null) flags = flags or 0x01
                    if (p.tiltRad != null) flags = flags or 0x02
                    if (p.orientationRad != null) flags = flags or 0x04
                    dos.writeByte(flags)

                    if (p.pressure != null) dos.writeFloat(p.pressure)
                    if (p.tiltRad != null) dos.writeFloat(p.tiltRad)
                    if (p.orientationRad != null) dos.writeFloat(p.orientationRad)
                }
            }
        }
        return byteOut.toByteArray()
    }

    private fun decompressAndDeserializePoints(compressedBlob: ByteArray): List<StrokePoint> {
        val byteIn = ByteArrayInputStream(compressedBlob)
        val points = mutableListOf<StrokePoint>()

        InflaterInputStream(byteIn).use { inflaterStream ->
            DataInputStream(inflaterStream).use { dis ->
                val magic = dis.readByte()
                require(magic == BLOB_MAGIC) { "Magic byte de pontos inválido: $magic" }
                val subVersion = dis.readByte()
                require(subVersion == 1.toByte()) { "Sub-versão inválida: $subVersion" }

                val size = dis.readInt()
                for (i in 0 until size) {
                    val x = dis.readFloat()
                    val y = dis.readFloat()
                    val tMs = dis.readLong()
                    val flags = dis.readByte().toInt()

                    val pressure = if (flags and 0x01 != 0) dis.readFloat() else null
                    val tilt = if (flags and 0x02 != 0) dis.readFloat() else null
                    val orientation = if (flags and 0x04 != 0) dis.readFloat() else null

                    points.add(StrokePoint(x, y, tMs, pressure, tilt, orientation))
                }
            }
        }
        return points
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val BLOB_MAGIC: Byte = 0x50 // 'P'
    }
}
