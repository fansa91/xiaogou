package com.xiaogou.bookkeeping.backup

import java.io.File
import java.security.GeneralSecurityException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * BackupCrypto 的 JVM 单元测试（CI 的 test 步骤跑，不用模拟器）。
 * 覆盖：往返一致、口令错报错、篡改报错、明文文件返回 false、过短文件返回 false。
 */
class BackupCryptoTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun writeFile(name: String, bytes: ByteArray): File =
        temp.newFile(name).apply { writeBytes(bytes) }

    @Test
    fun roundTrip_restoresOriginalBytes() {
        val original = ByteArray(10_000) { (it * 31 % 256).toByte() }
        val src = writeFile("src.bin", original)
        val enc = temp.newFile("enc.bin")
        val dec = temp.newFile("dec.bin")
        BackupCrypto.encrypt(src, enc, "webdav-password".toCharArray())
        assertTrue(BackupCrypto.decrypt(enc, dec, "webdav-password".toCharArray()))
        assertArrayEquals(original, dec.readBytes())
        // 加密文件以 XGZ1 开头，且不是原文。
        assertEquals("XGZ1", String(enc.readBytes().copyOfRange(0, 4), Charsets.US_ASCII))
        assertFalse(enc.readBytes().contentEquals(original))
    }

    @Test
    fun roundTrip_emptyFile() {
        val src = writeFile("empty.bin", ByteArray(0))
        val enc = temp.newFile("enc.bin")
        val dec = temp.newFile("dec.bin")
        BackupCrypto.encrypt(src, enc, "pw".toCharArray())
        assertTrue(BackupCrypto.decrypt(enc, dec, "pw".toCharArray()))
        assertArrayEquals(ByteArray(0), dec.readBytes())
    }

    @Test
    fun wrongPassword_throws() {
        val src = writeFile("src.bin", "账本数据".toByteArray(Charsets.UTF_8))
        val enc = temp.newFile("enc.bin")
        BackupCrypto.encrypt(src, enc, "correct".toCharArray())
        try {
            BackupCrypto.decrypt(enc, temp.newFile("dec.bin"), "wrong".toCharArray())
            fail("expected GeneralSecurityException")
        } catch (expected: GeneralSecurityException) {
            // 口令错必须报错，不能静默当成明文覆盖。
        }
    }

    @Test
    fun tamperedCiphertext_throws() {
        val src = writeFile("src.bin", "账本数据".toByteArray(Charsets.UTF_8))
        val enc = temp.newFile("enc.bin")
        BackupCrypto.encrypt(src, enc, "pw".toCharArray())
        val tampered = enc.readBytes()
        tampered[tampered.size - 1] = (tampered[tampered.size - 1].toInt() xor 0xFF).toByte()
        val tamperedFile = writeFile("tampered.bin", tampered)
        try {
            BackupCrypto.decrypt(tamperedFile, temp.newFile("dec.bin"), "pw".toCharArray())
            fail("expected GeneralSecurityException")
        } catch (expected: GeneralSecurityException) {
            // GCM 标签校验失败必须报错。
        }
    }

    @Test
    fun plaintextFile_returnsFalse() {
        val plain = writeFile("plain.db", "SQLite format 3\u0000fake".toByteArray(Charsets.US_ASCII))
        assertFalse(BackupCrypto.decrypt(plain, temp.newFile("dec.bin"), "pw".toCharArray()))
    }

    @Test
    fun shortFile_returnsFalse() {
        val short = writeFile("short.bin", "XG".toByteArray(Charsets.US_ASCII))
        assertFalse(BackupCrypto.decrypt(short, temp.newFile("dec.bin"), "pw".toCharArray()))
    }
}
