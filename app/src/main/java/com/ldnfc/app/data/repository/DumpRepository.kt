package com.ldnfc.app.data.repository

import android.content.Context
import com.ldnfc.app.crypto.DimensionsCrypto
import com.ldnfc.app.crypto.PasswordVariant
import com.ldnfc.app.data.model.DimensionsItem
import com.ldnfc.app.data.model.ItemCategory
import com.ldnfc.app.data.model.VehicleRebuild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Repositório responsável por carregar, analisar e indexar todos os dumps .bin de assets/
 * e gerar conteúdo para o modo avançado.
 */
class DumpRepository(private val context: Context) {

    private val cachedItems = mutableListOf<DimensionsItem>()
    private var isInitialized = false

    /**
     * Inicializa e carrega os 157 dumps a partir de assets/Dimensions/
     */
    suspend fun loadCatalog(): List<DimensionsItem> = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext cachedItems

        cachedItems.clear()

        // 1. Carrega Characters dos assets
        loadCategoryFromAssets("Dimensions/Characters", ItemCategory.CHARACTER)

        // 2. Carrega todos os veículos e gadgets de DimensionsCatalog (com IDs, nomes e níveis únicos)
        for (vDef in DimensionsCatalog.vehicles) {
            if (vDef.id <= 0 || vDef.name.equals("Unknown", ignoreCase = true)) continue
            val category = if (vDef.world.contains("Portal", ignoreCase = true) ||
                vDef.name.contains("Trap", ignoreCase = true) ||
                vDef.name.contains("Pad", ignoreCase = true)
            ) {
                ItemCategory.GADGET
            } else {
                ItemCategory.VEHICLE
            }
            val syntheticDump = createItemFromNumericId(vDef.id, category).rawDump
            cachedItems.add(
                DimensionsItem(
                    id = vDef.id,
                    name = vDef.name,
                    category = category,
                    world = vDef.world,
                    abilities = vDef.abilities,
                    rebuild = vDef.rebuild,
                    assetPath = null,
                    rawDump = syntheticDump
                )
            )
        }

        // Ordena por nome
        cachedItems.sortBy { it.name }
        isInitialized = true
        cachedItems
    }

    private fun loadCategoryFromAssets(assetDir: String, category: ItemCategory) {
        try {
            val fileList = context.assets.list(assetDir) ?: return
            for (fileName in fileList) {
                if (!fileName.endsWith(".bin", ignoreCase = true)) continue
                val itemName = fileName.removeSuffix(".bin")
                val fullPath = "$assetDir/$fileName"

                try {
                    val bytes = context.assets.open(fullPath).use { it.readBytes() }
                    if (bytes.size != 180) continue

                    val item = parseDump(itemName, category, fullPath, bytes)
                    cachedItems.add(item)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseDump(
        name: String,
        category: ItemCategory,
        path: String,
        rawDump: ByteArray
    ): DimensionsItem {
        return when (category) {
            ItemCategory.CHARACTER -> {
                val uid = DimensionsCrypto.extractUidFromDump(rawDump)
                var charId: Int? = null
                if (uid != null) {
                    val encData = rawDump.copyOfRange(36 * 4, 38 * 4)
                    charId = DimensionsCrypto.decryptCharacterId(uid, encData)
                }

                val def = if (charId != null) {
                    DimensionsCatalog.findCharacter(charId)
                } else {
                    DimensionsCatalog.findCharacterByName(name)
                }

                val finalId = charId ?: def?.id ?: 0
                val world = def?.world ?: "Desconhecido"
                val abilities = def?.abilities ?: emptyList()

                DimensionsItem(
                    id = finalId,
                    name = name,
                    category = category,
                    world = world,
                    abilities = abilities,
                    assetPath = path,
                    rawDump = rawDump
                )
            }
            ItemCategory.VEHICLE, ItemCategory.GADGET -> {
                val vecIdData = rawDump.copyOfRange(36 * 4, 37 * 4)
                val vecId = DimensionsCrypto.getVehicleId(vecIdData)
                val def = DimensionsCatalog.findVehicle(vecId) ?: DimensionsCatalog.findVehicleByName(name)

                val world = def?.world ?: if (category == ItemCategory.GADGET) "Gadget / Portal" else "Desconhecido"
                val abilities = def?.abilities ?: emptyList()
                val rebuild = def?.rebuild ?: VehicleRebuild.DEFAULT

                DimensionsItem(
                    id = vecId,
                    name = name,
                    category = category,
                    world = world,
                    abilities = abilities,
                    rebuild = rebuild,
                    assetPath = path,
                    rawDump = rawDump
                )
            }
        }
    }

    /**
     * Retorna itens filtrados por categoria e busca textual.
     */
    fun getItems(category: ItemCategory, query: String = ""): List<DimensionsItem> {
        val filtered = cachedItems.filter { it.category == category }
        if (query.isBlank()) return filtered
        val q = query.trim().lowercase()
        return filtered.filter {
            it.name.lowercase().contains(q) ||
                    it.world.lowercase().contains(q) ||
                    it.abilities.any { ab -> ab.lowercase().contains(q) } ||
                    it.id.toString() == q
        }
    }

    /**
     * Gera um DimensionsItem a partir de um ID numérico puro (Modo Avançado).
     * O dump sintético gerado contém a estrutura de memória padrão do LEGO Dimensions.
     */
    fun createItemFromNumericId(id: Int, category: ItemCategory): DimensionsItem {
        val syntheticDump = ByteArray(180) // 45 páginas de 4 bytes

        // Configuração padrão da tag:
        // Página 3: Capability Container NTAG213: E1 10 12 00
        syntheticDump[3 * 4 + 0] = 0xE1.toByte()
        syntheticDump[3 * 4 + 1] = 0x10.toByte()
        syntheticDump[3 * 4 + 2] = 0x12.toByte()
        syntheticDump[3 * 4 + 3] = 0x00.toByte()

        if (category == ItemCategory.CHARACTER) {
            val def = DimensionsCatalog.findCharacter(id)
            val name = def?.name ?: "Personagem #$id"
            val world = def?.world ?: "Personalizado"
            val abilities = def?.abilities ?: emptyList()

            // Observação: os dados na página 36/37 dependem do UID final e serão calculados
            // no momento da gravação através de DimensionsCrypto.encryptCharacterId(targetUid, id).
            return DimensionsItem(
                id = id,
                name = name,
                category = category,
                world = world,
                abilities = abilities,
                rawDump = syntheticDump
            )
        } else {
            val def = DimensionsCatalog.findVehicle(id)
            val name = def?.name ?: if (category == ItemCategory.GADGET) "Gadget #$id" else "Veículo #$id"
            val world = def?.world ?: "Personalizado"
            val abilities = def?.abilities ?: emptyList()
            val rebuild = def?.rebuild ?: VehicleRebuild.DEFAULT

            // Página 36 (0x24): ID do veículo em Little-Endian
            syntheticDump[36 * 4 + 0] = (id and 0xFF).toByte()
            syntheticDump[36 * 4 + 1] = ((id shr 8) and 0xFF).toByte()
            syntheticDump[36 * 4 + 2] = 0x00
            syntheticDump[36 * 4 + 3] = 0x00

            // Página 38 (0x26): Flag de veículo 00 01 00 00
            syntheticDump[38 * 4 + 0] = 0x00
            syntheticDump[38 * 4 + 1] = 0x01
            syntheticDump[38 * 4 + 2] = 0x00
            syntheticDump[38 * 4 + 3] = 0x00

            return DimensionsItem(
                id = id,
                name = name,
                category = category,
                world = world,
                abilities = abilities,
                rebuild = rebuild,
                rawDump = syntheticDump
            )
        }
    }
}
