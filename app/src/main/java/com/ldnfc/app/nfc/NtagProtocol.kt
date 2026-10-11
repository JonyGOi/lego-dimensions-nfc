package com.ldnfc.app.nfc

import android.nfc.TagLostException
import android.nfc.tech.NfcA
import com.ldnfc.app.data.model.TagType
import com.ldnfc.app.data.model.TagVersionInfo
import java.io.IOException

/**
 * Comandos de baixo nível para chips NXP NTAG213 / NTAG215 via NfcA.transceive().
 */
class NtagProtocol(private val nfcA: NfcA) {

    companion object {
        const val CMD_GET_VERSION = 0x60.toByte()
        const val CMD_READ = 0x30.toByte()
        const val CMD_WRITE = 0xA2.toByte()
        const val CMD_PWD_AUTH = 0x1B.toByte()
    }

    /**
     * Envia o comando GET_VERSION (0x60) e analisa a resposta de 8 bytes.
     */
    fun getVersion(): TagVersionInfo? {
        return try {
            val response = transceiveWithRetry(byteArrayOf(CMD_GET_VERSION))
            if (response.size >= 8) {
                val vendorId = response[1].toInt() and 0xFF
                val productType = response[2].toInt() and 0xFF
                val storageSize = response[6].toInt() and 0xFF
                val detectedType = TagType.fromStorageSize(response[6])
                TagVersionInfo(
                    rawBytes = response,
                    vendorId = vendorId,
                    productType = productType,
                    storageSize = storageSize,
                    detectedType = detectedType
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Executa a autenticação por senha (PWD_AUTH: 0x1B + 4 bytes).
     * Retorna o PACK de 2 bytes se bem-sucedido, ou null em caso de falha.
     */
    fun authenticate(password: ByteArray): ByteArray? {
        require(password.size == 4) { "A senha deve ter exatamente 4 bytes" }
        val cmd = ByteArray(5)
        cmd[0] = CMD_PWD_AUTH
        System.arraycopy(password, 0, cmd, 1, 4)

        return try {
            val response = transceiveWithRetry(cmd)
            if (response.size >= 2) {
                response
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Lê 16 bytes (4 páginas) a partir da página especificada (READ: 0x30 + page).
     */
    fun read16Bytes(page: Int): ByteArray {
        val cmd = byteArrayOf(CMD_READ, (page and 0xFF).toByte())
        val response = transceiveWithRetry(cmd)
        if (response.size < 16) {
            throw IOException("Resposta de leitura curta na página $page: ${response.size} bytes recebidos")
        }
        return response
    }

    /**
     * Escreve 4 bytes em uma página (WRITE: 0xA2 + page + 4 bytes de dados).
     */
    fun write4Bytes(page: Int, data: ByteArray): Boolean {
        require(data.size == 4) { "Os dados de escrita devem ter exatamente 4 bytes" }
        val cmd = ByteArray(6)
        cmd[0] = CMD_WRITE
        cmd[1] = (page and 0xFF).toByte()
        System.arraycopy(data, 0, cmd, 2, 4)

        val response = transceiveWithRetry(cmd)
        // A resposta de sucesso no NTAG é o nibble ACK (0x0A)
        return response.isNotEmpty() && (response[0].toInt() and 0x0F) == 0x0A
    }

    /**
     * Testa se a tag permite escrita na página 0 (Magic Tag / UID regravável).
     */
    fun testMagicTagPage0Writable(originalPage0: ByteArray): Boolean {
        return try {
            // Tenta reescrever o mesmo conteúdo da página 0
            write4Bytes(0, originalPage0)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executa transceive com até 3 tentativas para lidar com perda temporária de campo.
     */
    private fun transceiveWithRetry(data: ByteArray, maxRetries: Int = 3): ByteArray {
        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                return nfcA.transceive(data)
            } catch (e: TagLostException) {
                lastException = e
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(80)
                    } catch (_: InterruptedException) {}
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(50)
                    } catch (_: InterruptedException) {}
                }
            }
        }
        throw lastException ?: IOException("Falha no transceive após $maxRetries tentativas")
    }
}
