package hua.dy.image.utils

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

object X2GifUtils {
     fun convert(rawPath: String, gifPath: String): Boolean {
         // only applicable to the current scenario
         return if (rawPath.endsWith(".webp")) {
             Webp2GifUtils.convert(webpPath = rawPath, gifPath = gifPath)
         } else {
             val result = FFmpegKit.executeWithArguments(
                 arrayOf(
                     "-y",
                     "-i",
                     rawPath,
                     gifPath
                 )
             )
             ReturnCode.isSuccess(result.returnCode)
         }
     }
}