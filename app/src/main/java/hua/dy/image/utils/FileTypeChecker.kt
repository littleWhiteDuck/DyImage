package hua.dy.image.utils

object FileTypeChecker {
    private val JPEG_HEADER = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_HEADER = byteArrayOf(
        0x89.toByte(),
        0x50.toByte(),
        0x4E.toByte(),
        0x47.toByte(),
        0x0D.toByte(),
        0x0A.toByte(),
        0x1A.toByte(),
        0x0A.toByte()
    )
    private val GIF_HEADER = byteArrayOf(
        0x47.toByte(),
        0x49.toByte(),
        0x46.toByte(),
        0x38.toByte(),
        0x37.toByte(),
        0x61.toByte()
    ) // "GIF87a"
    private val GIF_HEADER2 = byteArrayOf(
        0x47.toByte(),
        0x49.toByte(),
        0x46.toByte(),
        0x38.toByte(),
        0x39.toByte(),
        0x61.toByte()
    ) // "GIF89a"

    fun isJpeg(fileBytes: ByteArray): Boolean {
        return fileBytes.size >= 3 && fileBytes.sliceArray(0 until 3)
            .contentEquals(JPEG_HEADER)
    }

    fun isPng(fileBytes: ByteArray): Boolean {
        return fileBytes.size >= 8 && fileBytes.sliceArray(0 until 8)
            .contentEquals(PNG_HEADER)
    }

    fun isGif(fileBytes: ByteArray): Boolean {
        return fileBytes.size >= 6 && (fileBytes.sliceArray(0 until 6)
            .contentEquals(GIF_HEADER) || fileBytes.sliceArray(0 until 6)
            .contentEquals(GIF_HEADER2))
    }

    fun isWebp(fileBytes: ByteArray): Boolean {
        return fileBytes.size >= 12 &&
                fileBytes.sliceArray(0 until 4).contentEquals("RIFF".toByteArray()) &&
                fileBytes.sliceArray(8 until 12).contentEquals("WEBP".toByteArray())
    }

    fun isHeic(fileBytes: ByteArray): Boolean {
        return fileBytes.size >= 12 && fileBytes.sliceArray(4 until 8)
            .contentEquals("ftyp".toByteArray())
    }

    fun getType(fileBytes: ByteArray): FileType {
        if (isJpeg(fileBytes)) return FileType.JPEG
        if (isPng(fileBytes)) return FileType.PNG
        if (isGif(fileBytes)) return FileType.GIF
        if (isWebp(fileBytes)) return FileType.WEBP
        if (isHeic(fileBytes)) return FileType.HEIC
        return FileType.UNKNOWN
    }
}

enum class FileType(val displayName: String) {
    JPEG(displayName = "jpg"),
    PNG(displayName = "png"),
    GIF(displayName = "gif"),
    WEBP(displayName = "webp"),

    HEIC(displayName = "heic"),
    UNKNOWN(displayName = "other")
}
