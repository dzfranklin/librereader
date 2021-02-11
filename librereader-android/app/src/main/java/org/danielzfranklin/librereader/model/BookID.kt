package org.danielzfranklin.librereader.model

import android.os.Parcel
import android.os.Parcelable
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

data class BookID(private val value: String) : Parcelable {
    private constructor(parcel: Parcel): this(parcel.readString()!!)

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

        @Suppress("UNUSED")
        @JvmField val CREATOR = object : Parcelable.Creator<BookID> {
            override fun createFromParcel(parcel: Parcel): BookID {
                return BookID(parcel)
            }

            override fun newArray(size: Int): Array<BookID?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(value)
    }

    override fun describeContents(): Int {
        return 0
    }
}
