package com.ldnfc.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ldnfc.app.crypto.DimensionsCrypto
import com.ldnfc.app.data.model.DimensionsItem
import com.ldnfc.app.data.model.ErrorType
import com.ldnfc.app.data.model.ItemCategory
import com.ldnfc.app.data.model.TagType
import com.ldnfc.app.data.model.WriteHistoryItem
import com.ldnfc.app.data.model.WriteResult
import com.ldnfc.app.data.repository.DimensionsCatalog
import com.ldnfc.app.data.repository.DumpRepository
import com.ldnfc.app.data.repository.HistoryRepository
import com.ldnfc.app.nfc.NtagWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class MainUiState(
    val isLoadingCatalog: Boolean = true,
    val selectedCategory: ItemCategory = ItemCategory.CHARACTER,
    val searchQuery: String = "",
    val catalogItems: List<DimensionsItem> = emptyList(),
    val previewItem: DimensionsItem? = null,
    val activeItemToWrite: DimensionsItem? = null,

    // Configurações de gravação
    val targetTagType: TagType = TagType.NTAG213,
    val autoDetectTagType: Boolean = true,
    val enablePasswordProtection: Boolean = false,
    val cloneUidIfMagicTag: Boolean = false,

    // Status da gravação
    val isWriting: Boolean = false,
    val writeProgress: Float = 0f,
    val writeStatusMessage: String = "",
    val lastWriteResult: WriteResult? = null,

    // Leitura e Backup
    val isReading: Boolean = false,
    val readProgress: Float = 0f,
    val readStatusMessage: String = "",
    val lastReadDump: ByteArray? = null,
    val lastReadTagType: TagType? = null,
    val lastReadItemName: String? = null,
    val exportedFilePath: String? = null,

    // Histórico
    val historyList: List<WriteHistoryItem> = emptyList(),

    // Modo Avançado
    val advancedCustomId: String = "",
    val advancedCategory: ItemCategory = ItemCategory.CHARACTER,
    val advancedResolvedItem: DimensionsItem? = null,

    // Estado do hardware NFC
    val isNfcEnabled: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dumpRepo = DumpRepository(application)
    private val historyRepo = HistoryRepository(application)
    private val ntagWriter = NtagWriter()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
        loadHistory()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCatalog = true)
            dumpRepo.loadCatalog()
            updateCatalogDisplay()
            _uiState.value = _uiState.value.copy(isLoadingCatalog = false)
        }
    }

    private fun loadHistory() {
        _uiState.value = _uiState.value.copy(historyList = historyRepo.getHistory())
    }

    fun selectCategory(category: ItemCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        updateCatalogDisplay()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        updateCatalogDisplay()
    }

    private fun updateCatalogDisplay() {
        val cat = _uiState.value.selectedCategory
        val query = _uiState.value.searchQuery
        val items = dumpRepo.getItems(cat, query)
        _uiState.value = _uiState.value.copy(catalogItems = items)
    }

    fun setPreviewItem(item: DimensionsItem?) {
        _uiState.value = _uiState.value.copy(previewItem = item)
    }

    fun setItemToWrite(item: DimensionsItem) {
        _uiState.value = _uiState.value.copy(
            activeItemToWrite = item,
            lastWriteResult = null,
            writeProgress = 0f,
            writeStatusMessage = "Aproxime a tag NFC do leitor"
        )
    }

    fun setTargetTagType(tagType: TagType) {
        _uiState.value = _uiState.value.copy(targetTagType = tagType)
    }

    fun setAutoDetectTagType(autoDetect: Boolean) {
        _uiState.value = _uiState.value.copy(autoDetectTagType = autoDetect)
    }

    fun setPasswordProtectionEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enablePasswordProtection = enabled)
    }

    fun setCloneUidIfMagicTag(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(cloneUidIfMagicTag = enabled)
    }

    fun setNfcEnabledState(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isNfcEnabled = enabled)
    }

    /**
     * Chamado quando o Android detecta uma tag NFC enquanto a tela de gravação está ativa.
     */
    fun onTagDiscoveredForWriting(tag: Tag) {
        val item = _uiState.value.activeItemToWrite ?: return
        if (_uiState.value.isWriting) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWriting = true,
                writeProgress = 0.05f,
                writeStatusMessage = "Conectando à tag NFC...",
                lastWriteResult = null
            )

            val result = ntagWriter.writeItemToTag(
                tag = tag,
                item = item,
                targetType = _uiState.value.targetTagType,
                enablePasswordProtection = _uiState.value.enablePasswordProtection,
                cloneUidIfMagic = _uiState.value.cloneUidIfMagicTag,
                onProgress = { progress, message ->
                    _uiState.value = _uiState.value.copy(
                        writeProgress = progress,
                        writeStatusMessage = message
                    )
                }
            )

            _uiState.value = _uiState.value.copy(
                isWriting = false,
                lastWriteResult = result
            )

            // Registra no histórico
            when (result) {
                is WriteResult.Success -> {
                    val historyItem = WriteHistoryItem(
                        itemName = item.name,
                        category = item.category,
                        tagType = result.tagType,
                        uidHex = result.uidHex,
                        success = true,
                        verificationDetails = "Gravado e verificado com sucesso (${result.pagesWritten} páginas)."
                    )
                    historyRepo.addHistoryItem(historyItem)
                    loadHistory()
                }
                is WriteResult.PageMismatch -> {
                    val historyItem = WriteHistoryItem(
                        itemName = item.name,
                        category = item.category,
                        tagType = result.tagType,
                        uidHex = result.uidHex,
                        success = false,
                        verificationDetails = "Divergência em ${result.divergentPages.size} página(s)."
                    )
                    historyRepo.addHistoryItem(historyItem)
                    loadHistory()
                }
                is WriteResult.Error -> {
                    val tagUid = tag.id?.joinToString(":") { "%02X".format(it) } ?: "Desconhecido"
                    val historyItem = WriteHistoryItem(
                        itemName = item.name,
                        category = item.category,
                        tagType = _uiState.value.targetTagType,
                        uidHex = tagUid,
                        success = false,
                        verificationDetails = result.message
                    )
                    historyRepo.addHistoryItem(historyItem)
                    loadHistory()
                }
            }
        }
    }

    /**
     * Chamado quando o Android detecta uma tag NFC na tela de leitura e backup.
     */
    fun onTagDiscoveredForReading(tag: Tag) {
        if (_uiState.value.isReading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isReading = true,
                readProgress = 0.05f,
                readStatusMessage = "Iniciando leitura da tag...",
                lastReadDump = null,
                lastReadItemName = null,
                exportedFilePath = null
            )

            val readRes = ntagWriter.readFullTag(tag) { progress, message ->
                _uiState.value = _uiState.value.copy(
                    readProgress = progress,
                    readStatusMessage = message
                )
            }

            if (readRes.isSuccess) {
                val (dump, type) = readRes.getOrThrow()
                val uid = tag.id ?: ByteArray(7)

                // Identifica se é um personagem ou veículo conhecido
                val name = identifyTagContent(uid, dump)

                _uiState.value = _uiState.value.copy(
                    isReading = false,
                    lastReadDump = dump,
                    lastReadTagType = type,
                    lastReadItemName = name,
                    readStatusMessage = "Tag lida com sucesso! $name"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isReading = false,
                    readStatusMessage = "Falha ao ler a tag: ${readRes.exceptionOrNull()?.localizedMessage}"
                )
            }
        }
    }

    private fun identifyTagContent(uid: ByteArray, dump: ByteArray): String {
        if (dump.size < 40 * 4) return "Dump incompleto"
        val p36 = dump.copyOfRange(36 * 4, 37 * 4)
        val p38 = dump.copyOfRange(38 * 4, 39 * 4)

        return if (DimensionsCrypto.isVehicle(p38)) {
            val vid = DimensionsCrypto.getVehicleId(p36)
            val def = DimensionsCatalog.findVehicle(vid)
            if (def != null) "Veículo: ${def.name} (${def.world})" else "Veículo ID #$vid"
        } else {
            val cid = DimensionsCrypto.decryptCharacterId(uid, dump.copyOfRange(36 * 4, 38 * 4))
            if (cid != null) {
                val def = DimensionsCatalog.findCharacter(cid)
                if (def != null) "Personagem: ${def.name} (${def.world})" else "Personagem ID #$cid"
            } else {
                "Tag LEGO Dimensions desconhecida ou vazia"
            }
        }
    }

    /**
     * Exporta o último dump lido para um arquivo .bin no armazenamento.
     */
    fun exportLastDumpToFile(outputStream: OutputStream): Boolean {
        val dump = _uiState.value.lastReadDump ?: return false
        return try {
            outputStream.use { it.write(dump) }
            _uiState.value = _uiState.value.copy(exportedFilePath = "Arquivo exportado com sucesso!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Métodos para o Modo Avançado
    fun setAdvancedCustomId(idStr: String) {
        _uiState.value = _uiState.value.copy(advancedCustomId = idStr)
        val numId = idStr.toIntOrNull()
        if (numId != null) {
            val item = dumpRepo.createItemFromNumericId(numId, _uiState.value.advancedCategory)
            _uiState.value = _uiState.value.copy(advancedResolvedItem = item)
        } else {
            _uiState.value = _uiState.value.copy(advancedResolvedItem = null)
        }
    }

    fun setAdvancedCategory(category: ItemCategory) {
        _uiState.value = _uiState.value.copy(advancedCategory = category)
        val numId = _uiState.value.advancedCustomId.toIntOrNull()
        if (numId != null) {
            val item = dumpRepo.createItemFromNumericId(numId, category)
            _uiState.value = _uiState.value.copy(advancedResolvedItem = item)
        }
    }

    fun clearHistory() {
        historyRepo.clearHistory()
        loadHistory()
    }
}
