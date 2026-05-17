package com.metashield.app.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

object SteganographyUtils {

    private const val AES_ALGORITHM = "AES/CBC/PKCS5Padding"
    private val IV = IvParameterSpec(ByteArray(16)) // Fixed IV for simplicity in this demo

    /**
     * Safely derives a 16-byte AES key from the password at the byte level.
     * Retains 100% backward compatibility with existing space-padded ASCII passwords,
     * while preventing crashes when using multi-byte characters (e.g. emojis).
     */
    private fun getAESKey(password: String): SecretKeySpec {
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        val keyBytes = ByteArray(16)
        for (i in 0 until 16) {
            keyBytes[i] = if (i < passwordBytes.size) passwordBytes[i] else 0x20.toByte()
        }
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts the message using AES and a password
     */
    private fun encrypt(message: String, password: String): String {
        val key = getAESKey(password)
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, IV)
        val encrypted = cipher.doFinal(message.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    /**
     * Decrypts the message using AES and a password
     */
    private fun decrypt(encryptedMessage: String, password: String): String {
        val key = getAESKey(password)
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, IV)
        val decoded = Base64.decode(encryptedMessage, Base64.DEFAULT)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted)
    }

    /**
     * Encodes a secret message into the LSB of a Bitmap's pixels.
     * Returns a new Bitmap with the embedded data.
     */
    fun encode(bitmap: Bitmap, message: String, password: String?): Bitmap {
        val dataToHide = if (password != null) {
            "ENC:" + encrypt(message, password)
        } else {
            "TXT:$message"
        } + "###" // End-of-message delimiter

        val bytes = dataToHide.toByteArray(Charset.forName("UTF-8"))
        val bits = BitSet()
        for (i in bytes.indices) {
            for (j in 0 until 8) {
                if ((bytes[i].toInt() shr (7 - j)) and 1 == 1) {
                    bits.set(i * 8 + j)
                }
            }
        }

        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = resultBitmap.width
        val height = resultBitmap.height
        var bitIndex = 0
        // Total bits including the bits after the last set bit (since BitSet.length() is the index of the last set bit + 1)
        // Actually, let's just use the size calculation
        val exactBitsCount = bytes.size * 8

        outer@for (y in 0 until height) {
            for (x in 0 until width) {
                if (bitIndex >= exactBitsCount) break@outer
                
                val pixel = resultBitmap.getPixel(x, y)
                var r = Color.red(pixel)
                var g = Color.green(pixel)
                var b = Color.blue(pixel)
                val a = Color.alpha(pixel)

                // We hide in the Blue channel's LSB for least visual impact
                val bit = if (bits.get(bitIndex)) 1 else 0
                b = (b and 0xFE) or bit
                
                resultBitmap.setPixel(x, y, Color.argb(a, r, g, b))
                bitIndex++
            }
        }
        return resultBitmap
    }

    /**
     * Extracts a secret message from the LSB of a Bitmap's pixels.
     */
    fun decode(bitmap: Bitmap, password: String?): String? {
        val width = bitmap.width
        val height = bitmap.height
        val bitSet = BitSet()
        var bitIndex = 0

        // Read all bits until we find the delimiter or hit max
        // To be safe, we read up to 10,000 characters (approx 80kb)
        val maxBits = Math.min(width * height, 10000 * 8)
        
        outer@for (y in 0 until height) {
            for (x in 0 until width) {
                if (bitIndex >= maxBits) break@outer
                val pixel = bitmap.getPixel(x, y)
                val b = Color.blue(pixel)
                if (b and 1 == 1) bitSet.set(bitIndex)
                bitIndex++
            }
        }

        val bytes = ByteArray(bitIndex / 8)
        for (i in bytes.indices) {
            var b = 0
            for (j in 0 until 8) {
                if (bitSet.get(i * 8 + j)) {
                    b = b or (1 shl (7 - j))
                }
            }
            bytes[i] = b.toByte()
        }

        val rawText = String(bytes, Charset.forName("UTF-8"))
        val content = rawText.substringBefore("###")
        
        return when {
            content.startsWith("ENC:") -> {
                val encrypted = content.substring(4)
                if (password == null) return "Locked Content (Password Required)"
                try {
                    decrypt(encrypted, password)
                } catch (e: Exception) {
                    "Wrong Password or Corrupt Data"
                }
            }
            content.startsWith("TXT:") -> content.substring(4)
            else -> "No hidden message found."
        }
    }

    private class BitSet {
        private var data = java.util.BitSet()
        private var currentSize = 0
        
        fun set(index: Int) {
            data.set(index)
            if (index >= currentSize) currentSize = index + 1
        }
        
        fun get(index: Int) = data.get(index)
        fun length() = currentSize
    }
}
