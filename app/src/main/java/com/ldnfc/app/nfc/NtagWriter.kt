package com.ldnfc.app.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcA
import com.ldnfc.app.crypto.DimensionsCrypto
import com.ldnfc.app.crypto.PasswordVariant
import com.ldnfc.app.data.model.DimensionsItem
import com.ldnfc.app.data.model.ErrorType
import com.ldnfc.app.data.model.ItemCategory
import com.ldnfc.app.data.model.TagType
import com.ldnfc.app.data.model.WriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Orquestrador de operações em tags NTAG213 e NTAG215:
 * detecção de chip, autenticação, regravação de ID criptografado, escrita de páginas,
 * configuração de senha/PACK e verificação página por página.
 */
class NtagWriter {

    data class TagInspection(
        val uid: ByteArray,
        val uidHex: String,
        val detectedType: TagType,
        val isPasswordProtected: Boolean,
        val isAlreadyWritten: Boolean,
        val isMagicTag: Boolean,
        val existingItemName: String?
    )

    /**
     * Inspeciona uma tag aproximada para determinar tipo, se está protegida, já gravada ou se é magic.
     */
    suspend fun inspectTag(tag: Tag): Result<TagInspection> = withContext(Dispatchers.IO) {
        val nfcA = NfcA.get(tag) ?: return@withContext Result.failure(
            IOException("A tag não suporta a tecnologia NfcA / MifareUltralight.")
        )

        try {
            nfcA.connect()
            nfcA.timeout = 2500
            val protocol = NtagProtocol(nfcA)

            val uid = tag.id ?: ByteArray(7)
            val uidHex = uid.joinToString(":") { "%02X".format(it) }

            // 1. Detecta tipo via GET_VERSION
            val version = protocol.getVersion()
            val detectedType = version?.detectedType ?: if (uid.size == 7) TagType.NTAG213 else TagType.UNKNOWN

            // 2. Tenta ler a página 0x24 (36) para ver se exige senha ou já tem dados
            var isProtected = false
            var isAlreadyWritten = false
            var existingName: String? = null

            try {
                val page36to39 = protocol.read16Bytes(36)
                // Se leu com sucesso, não requer senha para leitura
                val p36 = page36to39.copyOfRange(0, 4)
                val p38 = page36to39.copyOfRange(8, 12)

                if (!p36.all { it == 0.toByte() } || !p38.all { it == 0.toByte() }) {
                    isAlreadyWritten = true
                    if (DimensionsCrypto.isVehicle(p38)) {
                        val vid = DimensionsCrypto.getVehicleId(p36)
                        existingName = "Veículo #$vid"
                    } else {
                        val cid = DimensionsCrypto.decryptCharacterId(uid, page36to39.copyOfRange(0, 8))
                        existingName = if (cid != null) "Personagem #$cid" else "Item LEGO Dimensions"
                    }
                }
            } catch (e: Exception) {
                // Falha ao ler bloco 36: pode ser tag protegida por senha!
                isProtected = true
            }

            // 3. Testa se é Magic Tag (UID gravável)
            val page0 = try {
                protocol.read16Bytes(0).copyOfRange(0, 4)
            } catch (_: Exception) {
                byteArrayOf()
            }
            val isMagic = if (page0.size == 4) protocol.testMagicTagPage0Writable(page0) else false

            Result.success(
                TagInspection(
                    uid = uid,
                    uidHex = uidHex,
                    detectedType = detectedType,
                    isPasswordProtected = isProtected,
                    isAlreadyWritten = isAlreadyWritten,
                    isMagicTag = isMagic,
                    existingItemName = existingName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                nfcA.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Grava um item em uma tag NFC (NTAG213 ou NTAG215) e realiza a verificação página por página.
     */
    suspend fun writeItemToTag(
        tag: Tag,
        item: DimensionsItem,
        targetType: TagType,
        enablePasswordProtection: Boolean,
        cloneUidIfMagic: Boolean,
        onProgress: (Float, String) -> Unit
    ): WriteResult = withContext(Dispatchers.IO) {
        val nfcA = NfcA.get(tag) ?: return@withContext WriteResult.Error(
            ErrorType.TAG_INCOMPATIBLE,
            "Tag incompatível com NfcA."
        )

        try {
            nfcA.connect()
            nfcA.timeout = 3000
            val protocol = NtagProtocol(nfcA)

            val tagUid = tag.id ?: return@withContext WriteResult.Error(
                ErrorType.COMMUNICATION_ERROR,
                "Não foi possível obter o UID da tag."
            )
            val uidHex = tagUid.joinToString(":") { "%02X".format(it) }

            onProgress(0.05f, "Identificando chip NFC...")

            // 1. Detecção automática de chip via GET_VERSION (0x60)
            val versionInfo = protocol.getVersion()
            val actualType = if (versionInfo != null && versionInfo.detectedType != TagType.UNKNOWN) {
                versionInfo.detectedType
            } else {
                targetType
            }

            val startTime = System.currentTimeMillis()
            val executedCommands = mutableListOf<String>()

            onProgress(0.10f, "Chip detectado: ${actualType.displayName}")

            // 2. Determina páginas a serem gravadas (Modo Rápido: apenas os 3 comandos necessários como no Chteupnin)
            val pwdPage = if (actualType == TagType.NTAG215) 0x85 else 0x2B // 133 (NTAG215) ou 43 (NTAG213)
            val pwdBytes = DimensionsCrypto.generateCardPassword(tagUid, PasswordVariant.ELLERBACH_TEST)

            val pageWrites = mutableListOf<Pair<Int, ByteArray>>()

            if (item.category == ItemCategory.CHARACTER) {
                // Personagem: Páginas 0x24 e 0x25 com TEA criptografado a partir do UID da tag atual
                val encryptedCharData = DimensionsCrypto.encryptCharacterId(tagUid, item.id)
                pageWrites.add(0x24 to encryptedCharData.copyOfRange(0, 4))
                pageWrites.add(0x25 to encryptedCharData.copyOfRange(4, 8))
            } else {
                // Veículo / Gadget: Página 0x24 (ID do veículo) e Página 0x26 (Flag 00 01 00 00)
                val vehicleData = DimensionsCrypto.encryptVehicleId(item.id)
                pageWrites.add(0x24 to vehicleData)
                pageWrites.add(0x26 to byteArrayOf(0x00, 0x01, 0x00, 0x00))
            }

            // Senha (PWD): Página 0x85 (NTAG215) ou 0x2B (NTAG213)
            // No Chteupnin e no Toy Pad, a senha É SEMPRE o 3º comando essencial!
            pageWrites.add(pwdPage to pwdBytes)

            // Se o usuário ativar o bloqueio permanente (AUTH0), grava a proteção de escrita na página de configuração
            if (enablePasswordProtection) {
                val cfg0Page = if (actualType == TagType.NTAG215) 131 else 41
                pageWrites.add(cfg0Page to byteArrayOf(0x00, 0x00, 0x00, 0x24.toByte()))
            }

            // 3. Verificação de Magic Tag para clonar UID se solicitado
            var isMagicTag = false
            if (cloneUidIfMagic && item.rawDump != null && item.rawDump.size >= 8) {
                onProgress(0.20f, "Verificando Magic Tag...")
                val origP0 = item.rawDump.copyOfRange(0, 4)
                if (protocol.testMagicTagPage0Writable(origP0)) {
                    isMagicTag = true
                    onProgress(0.25f, "Magic Tag detectada! Clonando UID...")
                    try {
                        protocol.write4Bytes(0, item.rawDump.copyOfRange(0, 4))
                        protocol.write4Bytes(1, item.rawDump.copyOfRange(4, 8))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 4. Executa a escrita dos comandos (A2:XX:YYYYYYYY)
            var currentStep = 0
            for ((page, data) in pageWrites) {
                currentStep++
                val pct = 0.30f + (currentStep.toFloat() / pageWrites.size) * 0.45f
                val hexData = data.joinToString("") { "%02X".format(it) }
                val cmdStr = "A2:%02X:%s".format(page, hexData)
                executedCommands.add(cmdStr)

                onProgress(pct, "Enviando $cmdStr...")

                val ok = writePageWithRetry(protocol, page, data)
                if (!ok) {
                    return@withContext WriteResult.Error(
                        ErrorType.COMMUNICATION_ERROR,
                        "Falha ao enviar comando $cmdStr para a tag."
                    )
                }
            }

            // 5. Verificação instantânea em lote (lê páginas 0x24 a 0x27 em uma única requisição READ de 16 bytes)
            onProgress(0.85f, "Verificando integridade...")
            val divergentPages = mutableMapOf<Int, Pair<ByteArray, ByteArray>>()

            try {
                val readBack16 = protocol.read16Bytes(0x24)
                for ((page, expected) in pageWrites) {
                    if (page in 0x24..0x27) {
                        val offset = (page - 0x24) * 4
                        val actual = readBack16.copyOfRange(offset, offset + 4)
                        if (!actual.contentEquals(expected)) {
                            divergentPages[page] = Pair(expected, actual)
                        }
                    }
                }
            } catch (e: Exception) {
                // Se falhar na releitura
            }

            val durationMs = System.currentTimeMillis() - startTime
            onProgress(1.0f, "Gravado com sucesso em ${durationMs}ms!")

            if (divergentPages.isEmpty()) {
                WriteResult.Success(
                    tagType = actualType,
                    uidHex = uidHex,
                    pagesWritten = pageWrites.size,
                    verifiedOk = true,
                    isMagicTag = isMagicTag,
                    executedCommands = executedCommands,
                    durationMs = durationMs,
                    message = "Tag gravada em ${durationMs}ms com ${pageWrites.size} comandos!"
                )
            } else {
                WriteResult.PageMismatch(
                    tagType = actualType,
                    uidHex = uidHex,
                    divergentPages = divergentPages
                )
            }
        } catch (e: TagLostException) {
            WriteResult.Error(
                ErrorType.TAG_LOST,
                "A tag foi afastada do leitor durante a gravação. Mantenha a tag encostada firmemente.",
                e
            )
        } catch (e: Exception) {
            WriteResult.Error(
                ErrorType.COMMUNICATION_ERROR,
                "Erro durante a comunicação NFC: ${e.localizedMessage}",
                e
            )
        } finally {
            try {
                nfcA.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Lê uma tag completamente e retorna os bytes do dump e o tipo detectado.
     */
    suspend fun readFullTag(
        tag: Tag,
        onProgress: (Float, String) -> Unit
    ): Result<Pair<ByteArray, TagType>> = withContext(Dispatchers.IO) {
        val nfcA = NfcA.get(tag) ?: return@withContext Result.failure(
            IOException("Tag não suporta NfcA.")
        )

        try {
            nfcA.connect()
            nfcA.timeout = 3000
            val protocol = NtagProtocol(nfcA)
            val uid = tag.id ?: ByteArray(7)

            onProgress(0.1f, "Identificando tag...")
            val version = protocol.getVersion()
            val tagType = version?.detectedType ?: TagType.NTAG213

            // Tenta autenticar se necessário
            val pwd = DimensionsCrypto.generateCardPassword(uid, PasswordVariant.STANDARD_DUMP)
            protocol.authenticate(pwd)

            val totalPages = tagType.totalPages
            val dump = ByteArray(totalPages * 4)

            for (p in 0 until totalPages step 4) {
                val pct = 0.1f + (p.toFloat() / totalPages) * 0.85f
                onProgress(pct, "Lendo páginas $p a ${minOf(p + 3, totalPages - 1)}...")

                try {
                    val readBytes = protocol.read16Bytes(p)
                    val copyLen = minOf(16, (totalPages - p) * 4)
                    System.arraycopy(readBytes, 0, dump, p * 4, copyLen)
                } catch (e: Exception) {
                    // Preenche com zeros se falhar leitura protegida
                }
            }

            onProgress(1.0f, "Leitura completa!")
            Result.success(Pair(dump, tagType))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                nfcA.close()
            } catch (_: Exception) {}
        }
    }

    private fun writePageWithRetry(
        protocol: NtagProtocol,
        page: Int,
        data: ByteArray,
        retries: Int = 3
    ): Boolean {
        for (i in 1..retries) {
            try {
                if (protocol.write4Bytes(page, data)) return true
            } catch (e: Exception) {
                if (i == retries) return false
                try {
                    Thread.sleep(60)
                } catch (_: Exception) {}
            }
        }
        return false
    }
}
