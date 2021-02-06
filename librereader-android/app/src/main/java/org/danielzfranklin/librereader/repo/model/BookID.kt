package org.danielzfranklin.librereader.repo.model

import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

data class BookID(private val value: String) {
    override fun toString() = value

    companion object {
        fun forEpub(epub: InputStream): BookID {
            val digest = MessageDigest.getInstance("MD5")
            val inputStream = DigestInputStream(epub, digest)
            while (inputStream.read() != -1) {
                // We need to read the whole stream to update the digest
            }

            val hash = digest
                .digest()
                .joinToString("") {
                    // Convert to hex
                    String.format("%02x", it)
                }

            return BookID("$tag:$hash")
        }

        private const val tag = "librereaderidv1"
    }
}
