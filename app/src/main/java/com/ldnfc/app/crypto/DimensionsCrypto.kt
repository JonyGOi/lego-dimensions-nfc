package com.ldnfc.app.crypto

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Variantes de cálculo de senha para LEGO Dimensions.
 *
 * STANDARD_DUMP: Utiliza bytes [0xAA, 0x00] no final do buffer básico de scramble,
 * correspondendo a 100% dos 157 dumps oficiais extraídos do jogo.
 *
 * ELLERBACH_TEST: Utiliza bytes [0xAA, 0xAA] conforme documentado nos testes unitários
 * do repositório Ellerbach/LegoDimensions (TestDecryption.cs).
 */
enum class PasswordVariant {
    STANDARD_DUMP,
    ELLERBACH_TEST
}

/**
 * Implementação dos algoritmos de criptografia e derivação de chaves do LEGO Dimensions,
 * fielmente reproduzidos a partir do repositório Ellerbach/LegoDimensions.
 */
object DimensionsCrypto {

    /**
     * Rotaciona um valor de 32 bits para a direita (RotateRight).
     */
    fun rotateRight(value: Long, count: Int): Long {
        val v = value and 0xFFFFFFFFL
        val c = count % 32
        return ((v ushr c) or ((v shl (32 - c)) and 0xFFFFFFFFL)) and 0xFFFFFFFFL
    }

    /**
     * Gera a senha de 4 bytes necessária para autenticar e proteger a tag NTAG.
     *
     * @param uid UID de 7 bytes do chip NFC.
     * @param variant Variante do algoritmo (STANDARD_DUMP por padrão).
     * @return Array de 4 bytes contendo a senha em Little-Endian.
     */
    fun generateCardPassword(
        uid: ByteArray,
        variant: PasswordVariant = PasswordVariant.ELLERBACH_TEST
    ): ByteArray {
        require(uid.size == 7) { "UID deve ter exatamente 7 bytes. Tamanho recebido: ${uid.size}" }

        val lastByte = if (variant == PasswordVariant.STANDARD_DUMP) 0x00.toByte() else 0xAA.toByte()

        // Buffer básico: "UUUUUUU(c) Copyright LEGO 2014" + [0xAA, lastByte]
        val basic = byteArrayOf(
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x28.toByte(),
            0x63.toByte(), 0x29.toByte(), 0x20.toByte(), 0x43.toByte(),
            0x6F.toByte(), 0x70.toByte(), 0x79.toByte(), 0x72.toByte(),
            0x69.toByte(), 0x67.toByte(), 0x68.toByte(), 0x74.toByte(),
            0x20.toByte(), 0x4C.toByte(), 0x45.toByte(), 0x47.toByte(),
            0x4F.toByte(), 0x20.toByte(), 0x32.toByte(), 0x30.toByte(),
            0x31.toByte(), 0x34.toByte(), 0xAA.toByte(), lastByte
        )

        // Copia o UID nos primeiros 7 bytes
        System.arraycopy(uid, 0, basic, 0, 7)

        var v2 = 0L
        for (i in 0 until 8) {
            val v4 = rotateRight(v2, 25)
            val v5 = rotateRight(v2, 10)
            val b = ((basic[i * 4].toLong() and 0xFFL)) or
                    ((basic[i * 4 + 1].toLong() and 0xFFL) shl 8) or
                    ((basic[i * 4 + 2].toLong() and 0xFFL) shl 16) or
                    ((basic[i * 4 + 3].toLong() and 0xFFL) shl 24)

            v2 = (b + v4 + v5 - v2) and 0xFFFFFFFFL
        }

        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(v2.toInt())
        return buffer.array()
    }

    /**
     * Função auxiliar de embaralhamento (Scramble) para derivar as 4 chaves TEA a partir do UID.
     */
    private fun scramble(uid: ByteArray, cnt: Int): Long {
        val basic = byteArrayOf(
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xB7.toByte(),
            0xD5.toByte(), 0xD7.toByte(), 0xE6.toByte(), 0xE7.toByte(),
            0xBA.toByte(), 0x3C.toByte(), 0xA8.toByte(), 0xD8.toByte(),
            0x75.toByte(), 0x47.toByte(), 0x68.toByte(), 0xCF.toByte(),
            0x23.toByte(), 0xE9.toByte(), 0xFE.toByte(), 0xAA.toByte()
        )

        System.arraycopy(uid, 0, basic, 0, 7)
        basic[cnt * 4 - 1] = 0xAA.toByte()

        var v2 = 0L
        for (i in 0 until cnt) {
            val b = ((basic[i * 4].toLong() and 0xFFL)) or
                    ((basic[i * 4 + 1].toLong() and 0xFFL) shl 8) or
                    ((basic[i * 4 + 2].toLong() and 0xFFL) shl 16) or
                    ((basic[i * 4 + 3].toLong() and 0xFFL) shl 24)

            v2 = (b + rotateRight(v2, 25) + rotateRight(v2, 10) - v2) and 0xFFFFFFFFL
        }

        return v2 and 0xFFFFFFFFL
    }

    /**
     * Deriva o conjunto de 4 chaves de 32 bits a partir do UID.
     */
    fun generateKeys(uid: ByteArray): LongArray {
        require(uid.size == 7) { "UID deve ter 7 bytes" }
        return longArrayOf(
            scramble(uid, 3),
            scramble(uid, 4),
            scramble(uid, 5),
            scramble(uid, 6)
        )
    }

    /**
     * Criptografa 64 bits (dois inteiros de 32 bits) com a chave TEA de 128 bits.
     */
    fun teaEncrypt(v: LongArray, k: LongArray) {
        var v0 = v[0] and 0xFFFFFFFFL
        var v1 = v[1] and 0xFFFFFFFFL
        var sum = 0L
        val delta = 0x9E3779B9L

        val k0 = k[0] and 0xFFFFFFFFL
        val k1 = k[1] and 0xFFFFFFFFL
        val k2 = k[2] and 0xFFFFFFFFL
        val k3 = k[3] and 0xFFFFFFFFL

        for (i in 0 until 32) {
            sum = (sum + delta) and 0xFFFFFFFFL
            val term0 = (((v1 shl 4) + k0) xor (v1 + sum) xor ((v1 ushr 5) + k1)) and 0xFFFFFFFFL
            v0 = (v0 + term0) and 0xFFFFFFFFL

            val term1 = (((v0 shl 4) + k2) xor (v0 + sum) xor ((v0 ushr 5) + k3)) and 0xFFFFFFFFL
            v1 = (v1 + term1) and 0xFFFFFFFFL
        }

        v[0] = v0
        v[1] = v1
    }

    /**
     * Descriptografa 64 bits com a chave TEA de 128 bits.
     */
    fun teaDecrypt(v: LongArray, k: LongArray) {
        var v0 = v[0] and 0xFFFFFFFFL
        var v1 = v[1] and 0xFFFFFFFFL
        var sum = 0xC6EF3720L
        val delta = 0x9E3779B9L

        val k0 = k[0] and 0xFFFFFFFFL
        val k1 = k[1] and 0xFFFFFFFFL
        val k2 = k[2] and 0xFFFFFFFFL
        val k3 = k[3] and 0xFFFFFFFFL

        for (i in 0 until 32) {
            val term1 = (((v0 shl 4) + k2) xor (v0 + sum) xor ((v0 ushr 5) + k3)) and 0xFFFFFFFFL
            v1 = (v1 - term1) and 0xFFFFFFFFL

            val term0 = (((v1 shl 4) + k0) xor (v1 + sum) xor ((v1 ushr 5) + k1)) and 0xFFFFFFFFL
            v0 = (v0 - term0) and 0xFFFFFFFFL

            sum = (sum - delta) and 0xFFFFFFFFL
        }

        v[0] = v0
        v[1] = v1
    }

    /**
     * Criptografa o ID de um personagem para gravação nas páginas 0x24 e 0x25 (8 bytes no total).
     */
    fun encryptCharacterId(uid: ByteArray, charId: Int): ByteArray {
        val keys = generateKeys(uid)
        val buf = longArrayOf(charId.toLong() and 0xFFFFL, charId.toLong() and 0xFFFFL)
        teaEncrypt(buf, keys)

        val data = ByteArray(8)
        for (i in 0 until 4) {
            data[i] = ((buf[0] ushr (i * 8)) and 0xFFL).toByte()
            data[i + 4] = ((buf[1] ushr (i * 8)) and 0xFFL).toByte()
        }
        return data
    }

    /**
     * Descriptografa o ID do personagem a partir das páginas 0x24 e 0x25 (8 bytes).
     * Retorna o ID numérico ou null se a descriptografia for inválida.
     */
    fun decryptCharacterId(uid: ByteArray, data: ByteArray): Int? {
        if (data.size < 8) return null
        val keys = generateKeys(uid)

        val v0 = ((data[0].toLong() and 0xFFL)) or
                ((data[1].toLong() and 0xFFL) shl 8) or
                ((data[2].toLong() and 0xFFL) shl 16) or
                ((data[3].toLong() and 0xFFL) shl 24)

        val v1 = ((data[4].toLong() and 0xFFL)) or
                ((data[5].toLong() and 0xFFL) shl 8) or
                ((data[6].toLong() and 0xFFL) shl 16) or
                ((data[7].toLong() and 0xFFL) shl 24)

        val v = longArrayOf(v0, v1)
        teaDecrypt(v, keys)

        // No formato LEGO Dimensions, após a descriptografia os dois inteiros devem ser idênticos
        if (v[0] != v[1]) {
            return null
        }
        return (v[0] and 0xFFFFL).toInt()
    }

    /**
     * Codifica o ID de veículo para a página 0x24 (4 bytes em Little-Endian).
     */
    fun encryptVehicleId(vecId: Int): ByteArray {
        val data = ByteArray(4)
        data[0] = (vecId and 0xFF).toByte()
        data[1] = ((vecId shr 8) and 0xFF).toByte()
        data[2] = 0x00
        data[3] = 0x00
        return data
    }

    /**
     * Obtém o ID do veículo a partir dos dados da página 0x24.
     */
    fun getVehicleId(data: ByteArray): Int {
        require(data.size >= 2) { "Dados devem ter pelo menos 2 bytes" }
        return (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
    }

    /**
     * Verifica se a página 0x26 (38) indica que a tag é um veículo.
     * Veículos possuem 00 01 00 00 na página 0x26.
     */
    fun isVehicle(page26Data: ByteArray): Boolean {
        if (page26Data.size < 4) return false
        return page26Data[0] == 0x00.toByte() &&
                page26Data[1] == 0x01.toByte() &&
                page26Data[2] == 0x00.toByte() &&
                page26Data[3] == 0x00.toByte()
    }

    /**
     * Extrai o UID de 7 bytes a partir das páginas 0 e 1 de um dump NTAG.
     * Página 0: UID0, UID1, UID2, BCC0
     * Página 1: UID3, UID4, UID5, UID6
     */
    fun extractUidFromDump(dumpBytes: ByteArray): ByteArray? {
        if (dumpBytes.size < 8) return null
        return byteArrayOf(
            dumpBytes[0], dumpBytes[1], dumpBytes[2],
            dumpBytes[4], dumpBytes[5], dumpBytes[6], dumpBytes[7]
        )
    }
}
