#include <jni.h>
#include "webp2gif.h"


extern "C" JNIEXPORT jboolean
Java_hua_dy_image_utils_Webp2GifUtils_convert(JNIEnv *env, jobject thiz, jstring webp_path,
                                         jstring gif_path) {
    const char *webp_cstr = env->GetStringUTFChars(webp_path, nullptr);
    const char *gif_cstr = env->GetStringUTFChars(gif_path, nullptr);

    bool result = webp2gif(std::string(webp_cstr), std::string(gif_cstr));

    env->ReleaseStringUTFChars(webp_path, webp_cstr);
    env->ReleaseStringUTFChars(gif_path, gif_cstr);

    return result ? JNI_TRUE : JNI_FALSE;
}