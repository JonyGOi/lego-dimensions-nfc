package com.ldnfc.app.data.model

/**
 * Tipo de tag NFC suportada.
 */
enum class TagType(val displayName: String, val userPages: Int, val totalPages: Int, val ccValue: ByteArray) {
    NTAG213("NTAG213", 36, 45, byteArrayOf(0xE1.toByte(), 0x10.toByte(), 0x12.toByte(), 0x00.toByte())),
    NTAG215("NTAG215", 126, 135, byteArrayOf(0xE1.toByte(), 0x10.toByte(), 0x3E.toByte(), 0x00.toByte())),
    UNKNOWN("Desconhecido", 0, 0, byteArrayOf());

    companion object {
        fun fromStorageSize(sizeByte: Byte): TagType {
            return when (sizeByte.toInt() and 0xFF) {
                0x0F -> NTAG213
                0x11 -> NTAG215
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Categorias dos itens do LEGO Dimensions.
 */
enum class ItemCategory(val displayName: String, val folderName: String) {
    CHARACTER("Personagens", "Characters"),
    VEHICLE("Veículos", "Vehicles"),
    GADGET("Gadgets", "Gadgets")
}

/**
 * Reconstruções de veículos (Tier 1, 2 ou 3).
 */
enum class VehicleRebuild(val displayName: String) {
    DEFAULT("Modelo Padrão (1)"),
    REBUILD_TWO("Segunda Forma (2)"),
    REBUILD_THREE("Terceira Forma (3)")
}

/**
 * Representa um item do catálogo (Personagem, Veículo ou Gadget).
 */
data class DimensionsItem(
    val id: Int,
    val name: String,
    val category: ItemCategory,
    val world: String = "Desconhecido",
    val abilities: List<String> = emptyList(),
    val rebuild: VehicleRebuild? = null,
    val assetPath: String? = null,
    val rawDump: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DimensionsItem
        return id == other.id && name == other.name && category == other.category
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + category.hashCode()
        return result
    }
}

/**
 * Informações retornadas pelo comando GET_VERSION (0x60) da NTAG.
 */
data class TagVersionInfo(
    val rawBytes: ByteArray,
    val vendorId: Int,
    val productType: Int,
    val storageSize: Int,
    val detectedType: TagType
) {
    val isNxp: Boolean = vendorId == 0x04
}

/**
 * Resultado da gravação e verificação de uma tag.
 */
sealed class WriteResult {
    data class Success(
        val tagType: TagType,
        val uidHex: String,
        val pagesWritten: Int,
        val verifiedOk: Boolean,
        val isMagicTag: Boolean = false,
        val executedCommands: List<String> = emptyList(),
        val durationMs: Long = 0L,
        val message: String
    ) : WriteResult()

    data class PageMismatch(
        val tagType: TagType,
        val uidHex: String,
        val divergentPages: Map<Int, Pair<ByteArray, ByteArray>> // página -> (esperado, lido)
    ) : WriteResult()

    data class Error(
        val errorType: ErrorType,
        val message: String,
        val cause: Throwable? = null
    ) : WriteResult()
}

enum class ErrorType {
    TAG_INCOMPATIBLE,
    TAG_PASSWORD_PROTECTED,
    TAG_ALREADY_WRITTEN,
    TAG_LOST,
    NFC_DISABLED,
    COMMUNICATION_ERROR,
    INVALID_DUMP
}

/**
 * Item persistido no histórico de gravações.
 */
data class WriteHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val itemName: String,
    val category: ItemCategory,
    val tagType: TagType,
    val uidHex: String,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean,
    val verificationDetails: String
)
