package com.ldnfc.app.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DimensionsCryptoTest {

    @Test
    fun testPasswordGenerationEllerbachReference() {
        // Teste de referência retirado de TestDecryption.cs no repositório Ellerbach
        val uid = byteArrayOf(0x04, 0x27, 0x50, 0x4A, 0x50, 0x49, 0x80.toByte())
        val expectedPassword = byteArrayOf(0xA1.toByte(), 0x7B.toByte(), 0x4C.toByte(), 0x95.toByte())

        val calculated = DimensionsCrypto.generateCardPassword(uid, PasswordVariant.ELLERBACH_TEST)
        assertArrayEquals("A senha calculada deve bater com o teste do repositório Ellerbach", expectedPassword, calculated)
    }

    @Test
    fun testDecryptVehicleEllerbachReference() {
        // Teste de referência de TestDecryption.cs: DecryptVehicle
        val data = byteArrayOf(
            0x63, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )
        val page26 = data.copyOfRange(8, 12)
        assertTrue(DimensionsCrypto.isVehicle(page26))

        val vehicleId = DimensionsCrypto.getVehicleId(data)
        assertEquals(1123, vehicleId) // Ghost Trap
    }

    @Test
    fun testEncryptVehicleEllerbachReference() {
        // Teste de referência de TestDecryption.cs: EncryptVehicle
        val vehicleId = 1123
        val encrypted = DimensionsCrypto.encryptVehicleId(vehicleId)
        val expected = byteArrayOf(0x63, 0x04, 0x00, 0x00)
        assertArrayEquals(expected, encrypted)
    }

    @Test
    fun testEncryptAndDecryptCharacterEllerbachReference() {
        // Teste de referência de TestDecryption.cs: EncryptCharacter & DecryptCharacter
        val uid = byteArrayOf(0x04, 0x47, 0x37, 0xE2.toByte(), 0x48, 0x3F, 0x80.toByte())
        val charId = 16 // Emmet
        val expectedEncrypted = byteArrayOf(
            0x5C.toByte(), 0xF7.toByte(), 0x1C.toByte(), 0xDE.toByte(),
            0x29.toByte(), 0xAD.toByte(), 0xEA.toByte(), 0x08.toByte()
        )

        // Criptografia
        val encrypted = DimensionsCrypto.encryptCharacterId(uid, charId)
        assertArrayEquals("Dados criptografados devem coincidir com o teste de referência", expectedEncrypted, encrypted)

        // Descriptografia
        val decrypted = DimensionsCrypto.decryptCharacterId(uid, encrypted)
        assertNotNull(decrypted)
        assertEquals(charId, decrypted)
    }

    @Test
    fun testExtractUidFromDump() {
        // Página 0: 04 65 1a 01
        // Página 1: b6 ca b7 80
        val dump = ByteArray(180)
        dump[0] = 0x04; dump[1] = 0x65; dump[2] = 0x1A; dump[3] = 0x01
        dump[4] = 0xB6.toByte(); dump[5] = 0xCA.toByte(); dump[6] = 0xB7.toByte(); dump[7] = 0x80.toByte()

        val uid = DimensionsCrypto.extractUidFromDump(dump)
        assertNotNull(uid)
        val expectedUid = byteArrayOf(0x04, 0x65, 0x1A, 0xB6.toByte(), 0xCA.toByte(), 0xB7.toByte(), 0x80.toByte())
        assertArrayEquals(expectedUid, uid)
    }

    @Test
    fun testPasswordGenerationStandardDumpVariant() {
        // Batman.bin: UID [04, 65, 1A, B6, CA, B7, 80], Senha no dump: 62 0e 6a 74
        val uid = byteArrayOf(0x04, 0x65, 0x1A, 0xB6.toByte(), 0xCA.toByte(), 0xB7.toByte(), 0x80.toByte())
        val expectedPassword = byteArrayOf(0x62, 0x0E, 0x6A, 0x74)

        val pwd = DimensionsCrypto.generateCardPassword(uid, PasswordVariant.STANDARD_DUMP)
        assertArrayEquals(expectedPassword, pwd)
    }

    @Test
    fun testUserChteupninBatmanReference() {
        // UID: 04:16:DF:76:5F:61:80
        val uid = byteArrayOf(0x04, 0x16, 0xDF.toByte(), 0x76, 0x5F, 0x61, 0x80.toByte())
        val batmanId = 1

        val enc = DimensionsCrypto.encryptCharacterId(uid, batmanId)
        val p24 = enc.copyOfRange(0, 4)
        val p25 = enc.copyOfRange(4, 8)
        val pwd = DimensionsCrypto.generateCardPassword(uid)

        // Esperado pelo site Chteupnin:
        // A2:24:DCA7B032
        // A2:25:CFCA2CE1
        // A2:85:674B8F80
        val expectedP24 = byteArrayOf(0xDC.toByte(), 0xA7.toByte(), 0xB0.toByte(), 0x32)
        val expectedP25 = byteArrayOf(0xCF.toByte(), 0xCA.toByte(), 0x2C, 0xE1.toByte())
        val expectedPwd = byteArrayOf(0x67, 0x4B, 0x8F.toByte(), 0x80.toByte())

        assertArrayEquals("Página 24 deve ser exatamente DCA7B032", expectedP24, p24)
        assertArrayEquals("Página 25 deve ser exatamente CFCA2CE1", expectedP25, p25)
        assertArrayEquals("Senha deve ser exatamente 674B8F80", expectedPwd, pwd)
    }
}
