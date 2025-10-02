#include "webp2gif.h"
#include "gif.h"
#include "webp/demux.h"
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <android/log.h>

#define LOG_TAG "Webp2Gif"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

bool webp2gif(const std::string& webp_path, const std::string& gif_path) {
    // 打开 WebP 文件
    FILE* webp_file = fopen(webp_path.c_str(), "rb");
    if (!webp_file) {
        LOGE("打开 WebP 失败: %s", webp_path.c_str());
        return false;
    }

    // 获取文件大小
    fseek(webp_file, 0, SEEK_END);
    size_t file_size = ftell(webp_file);
    fseek(webp_file, 0, SEEK_SET);

    // 读取文件数据
    uint8_t* webp_data = (uint8_t*)malloc(file_size);
    if (!webp_data) {
        LOGE("内存分配失败");
        fclose(webp_file);
        return false;
    }
    size_t read_bytes = fread(webp_data, 1, file_size, webp_file);
    fclose(webp_file);
    if (read_bytes != file_size) {
        LOGE("WebP 读取不完整");
        free(webp_data);
        return false;
    }

    // 初始化 WebP 数据结构
    WebPData webp = {webp_data, file_size};

    // 初始化解码器选项（禁用线程）
    WebPAnimDecoderOptions options;
    WebPAnimDecoderOptionsInit(&options);
    options.color_mode = MODE_RGBA;
    options.use_threads = false;

    // 创建 WebP 动画解码器
    WebPAnimDecoder* decoder = WebPAnimDecoderNew(&webp, &options);
    if (!decoder) {
        LOGE("WebP 解码器初始化失败");
        free(webp_data);
        return false;
    }

    // 获取动画信息
    WebPAnimInfo anim_info;
    WebPAnimDecoderGetInfo(decoder, &anim_info);
    LOGD("动画信息: 宽=%d, 高=%d, 帧数=%d", anim_info.canvas_width, anim_info.canvas_height, anim_info.frame_count);

    // 初始化 GIF 写入器
    GifWriter gif_writer;
    if (!GifBegin(&gif_writer, gif_path.c_str(), anim_info.canvas_width, anim_info.canvas_height, 1, 8, false)) {
        LOGE("GIF 创建失败: %s", gif_path.c_str());
        WebPAnimDecoderDelete(decoder);
        free(webp_data);
        return false;
    }

    // 逐帧处理
    int prev_ts = 0;
    int curr_ts = 0;
    uint8_t* frame_data = nullptr;
    bool success = true;

    while (WebPAnimDecoderHasMoreFrames(decoder)) {
        if (!WebPAnimDecoderGetNext(decoder, &frame_data, &curr_ts)) {
            LOGE("获取帧数据失败");
            success = false;
            break;
        }

        // 计算延迟（转换为 GIF 单位：1/100 秒）
        int delay = (curr_ts - prev_ts) / 10;
        if (delay < 1) delay = 1; // 最小延迟 10ms

        // 写入 GIF 帧
        if (!GifWriteFrame(&gif_writer, frame_data, anim_info.canvas_width, anim_info.canvas_height, delay, 8, false)) {
            LOGE("写入 GIF 帧失败");
            success = false;
            break;
        }

        prev_ts = curr_ts;
    }

    // 资源清理
    GifEnd(&gif_writer);
    WebPAnimDecoderDelete(decoder);
    free(webp_data);

    return success;
}