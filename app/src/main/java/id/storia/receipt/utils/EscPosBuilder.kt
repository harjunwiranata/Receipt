package id.storia.receipt.utils

import java.io.ByteArrayOutputStream

class EscPosBuilder {
    private val outputStream = ByteArrayOutputStream()
    private val lineWidth = 32

    fun reset(): EscPosBuilder {
        outputStream.write(byteArrayOf(0x1B, 0x40))
        return this
    }

    fun alignCenter(): EscPosBuilder {
        outputStream.write(byteArrayOf(0x1B, 0x61, 0x01))
        return this
    }

    fun boldOn(): EscPosBuilder {
        outputStream.write(byteArrayOf(0x1B, 0x45, 0x01))
        return this
    }

    fun boldOff(): EscPosBuilder {
        outputStream.write(byteArrayOf(0x1B, 0x45, 0x00))
        return this
    }

    fun line(text: String = ""): EscPosBuilder {
        outputStream.write((text + "\n").toByteArray(Charsets.US_ASCII))
        return this
    }

    fun feed(lines: Int = 1): EscPosBuilder {
        repeat(lines) { outputStream.write("\n".toByteArray()) }
        return this
    }

    fun separator(): EscPosBuilder {
        line("-".repeat(lineWidth))
        return this
    }

    fun build(): ByteArray = outputStream.toByteArray()
}
