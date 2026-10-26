package com.xiaogou.bookkeeping.backup

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份加密工具（WebDAV 备份第 1 步）。
 *
 * 方案：AES-256-GCM 认证加密 + PBKDF2WithHmacSHA256 密钥派生，
 * 密钥由 WebDAV 密码派生，不用另外记一个备份口令。
 *
 * 文件格式（固定长度头 + 密文）：
 *  [0..3]   magic  "XGZ1"
 *  [4..19]  salt   16 字节
 *  [20..31] iv     12 字节
 *  [32..]   AES-GCM 密文 + 128-bit 认证标签
 *
 * 解密口径：不是本格式的文件返回 false（调用方按旧版明文备份处理）；
 * 是本格式但口令错误或数据被篡改时，GCM 校验失败抛 GeneralSecurityException，
 * 调用方不得把它当成明文文件静默覆盖。
 */
object BackupCrypto {
    const val MAGIC = "XGZ1"
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_BITS = 256
    private const val HEADER_LEN = 4 + SALT_LEN + IV_LEN

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        if (password.isEmpty()) {
            throw GeneralSecurityException("backup password is empty")
        }
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    /** 加密 src -> dst。dst 内容为 [magic|salt|iv|ciphertext+tag]。 */
    fun encrypt(src: File, dst: File, password: CharArray) {
        val salt = ByteArray(SALT_LEN)
        val iv = ByteArray(IV_LEN)
        val random = SecureRandom()
        random.nextBytes(salt)
        random.nextBytes(iv)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))

        val buffer = ByteArray(8192)
        FileInputStream(src).use { input ->
            FileOutputStream(dst).use { output ->
                output.write(MAGIC.toByteArray(Charsets.US_ASCII))
                output.write(salt)
                output.write(iv)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                    cipher.update(chunk)?.let { output.write(it) }
                }
                output.write(cipher.doFinal())
            }
        }
    }

    /**
     * 解密 src -> dst。
     * @return true = 是本工具生成的加密文件且解密成功；
     *         false = 不是加密文件（旧版未加密备份），调用方应按原始文件处理。
     */
    fun decrypt(src: File, dst: File, password: CharArray): Boolean {
        val head = ByteArray(HEADER_LEN)
        FileInputStream(src).use { input ->
            var offset = 0
            while (offset < head.size) {
                val read = input.read(head, offset, head.size - offset)
                if (read == -1) return false
                offset += read
            }
        }
        val magic = String(head, 0, 4, Charsets.US_ASCII)
        if (magic != MAGIC) return false // 旧版明文备份
        val salt = head.copyOfRange(4, 4 + SALT_LEN)
        val iv = head.copyOfRange(4 + SALT_LEN, head.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))

        val buffer = ByteArray(8192)
        FileInputStream(src).use { input ->
            input.skip(HEADER_LEN.toLong())
            FileOutputStream(dst).use { output ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                    cipher.update(chunk)?.let { output.write(it) }
                }
                output.write(cipher.doFinal())
            }
        }
        return true
    }
}
