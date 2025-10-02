cmake_minimum_required(VERSION 3.18.1)
project("webp2gif")

include_directories(include)
include_directories(.)

add_library(webp SHARED IMPORTED)
set_target_properties(webp PROPERTIES
        IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libwebp.so"
)

add_library(webpdemux SHARED IMPORTED)
set_target_properties(webpdemux PROPERTIES
        IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libwebpdemux.so"
)

add_library(webp2gif SHARED
        jni_bridge.cpp
        webp2gif.cpp
)

target_link_libraries(webp2gif
        webp
        webpdemux
        log
        android
)