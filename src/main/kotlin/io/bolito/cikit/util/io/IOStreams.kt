package io.bolito.cikit.util.io

import java.io.OutputStream

object NullOutputStream : OutputStream() {
    override fun write(byte: Int) {}
}

class TeeOutputStream(
    private val os1: OutputStream,
    private val os2: OutputStream
) : OutputStream() {

    override fun write(byteArray: ByteArray) {
        os1.write(byteArray)
        os2.write(byteArray)
    }

    override fun write(byteArray: ByteArray, limit: Int, offset: Int) {
        os1.write(byteArray, limit, offset)
        os2.write(byteArray, limit, offset)
    }

    override fun flush() {
        os1.flush()
        os2.flush()
    }

    override fun write(byte: Int) {
        os1.write(byte)
        os2.write(byte)
    }

    override fun close() = os2.use {
        os1.close()
    }
}