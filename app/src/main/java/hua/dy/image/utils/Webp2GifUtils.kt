package hua.dy.image.utils

object Webp2GifUtils {
    init {
        System.loadLibrary("webp2gif")
    }

    external fun convert(webpPath: String, gifPath: String): Boolean
}