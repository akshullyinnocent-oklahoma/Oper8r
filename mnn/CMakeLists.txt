cmake_minimum_required(VERSION 3.22.1)

project("mnn_jni")

# Set MNN source path
set(MNN_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/MNN)

# Use CMake Policy CMP0077 to allow set() to override option()
if(POLICY CMP0077)
    cmake_policy(SET CMP0077 NEW)
endif()

# Set flags to avoid MNN POST_BUILD command issues
set(MNN_BUILD_FOR_ANDROID_COMMAND ON CACHE BOOL "Build from command" FORCE)

# Enable LLM support and related optimizations
set(MNN_BUILD_LLM ON CACHE BOOL "Build LLM support" FORCE)
set(MNN_LOW_MEMORY ON CACHE BOOL "Use low memory mode" FORCE)
set(MNN_SUPPORT_TRANSFORMER_FUSE ON CACHE BOOL "Support transformer fuse" FORCE)
set(MNN_CPU_WEIGHT_DEQUANT_GEMM ON CACHE BOOL "CPU weight dequant gemm" FORCE)

# Enable GPU backend support
set(MNN_VULKAN ON CACHE BOOL "Enable Vulkan backend" FORCE)
set(MNN_OPENCL ON CACHE BOOL "Enable OpenCL backend" FORCE)
set(MNN_OPENGL ON CACHE BOOL "Enable OpenGL backend" FORCE)
set(MNN_SUPPORT_RENDER ON CACHE BOOL "Enable render backend" FORCE)

# Disable separate build - compile all backends into libMNN.so (fixes "Can't Find type=3 backend")
set(MNN_SEP_BUILD OFF CACHE BOOL "Build backends separately" FORCE)

# Disable unnecessary features to speed up build
set(MNN_BUILD_BENCHMARK OFF CACHE BOOL "Build benchmark" FORCE)
set(MNN_BUILD_TEST OFF CACHE BOOL "Build test" FORCE)
set(MNN_BUILD_TOOLS OFF CACHE BOOL "Build tools" FORCE)
set(MNN_BUILD_QUANTOOLS OFF CACHE BOOL "Build quantools" FORCE)
set(MNN_EVALUATION OFF CACHE BOOL "Build evaluation" FORCE)
set(MNN_BUILD_CONVERTER OFF CACHE BOOL "Build converter" FORCE)
set(MNN_BUILD_TRAIN OFF CACHE BOOL "Build train" FORCE)

# Add MNN main project
add_subdirectory(${MNN_SOURCE_DIR} ${CMAKE_CURRENT_BINARY_DIR}/MNN)

# Include JNI binding code (only files we need)
add_library(
    MNNWrapper
    SHARED
    src/main/cpp/mnnnetnative.cpp
    src/main/cpp/mnnmodulennative.cpp
    src/main/cpp/mnnllmnative.cpp
    # mnnportraitnative.cpp uses a different package name, not needed here
)

# Include MNN headers
target_include_directories(MNNWrapper PRIVATE
    ${MNN_SOURCE_DIR}/include
    ${MNN_SOURCE_DIR}/source
    ${MNN_SOURCE_DIR}/express
    ${MNN_SOURCE_DIR}/transformers/llm/engine/include
    ${MNN_SOURCE_DIR}/transformers/llm/engine/src
    ${MNN_SOURCE_DIR}/3rd_party
)

# Add compile options to fix TLS issues
target_compile_options(MNNWrapper PRIVATE -fno-emulated-tls)
target_compile_options(MNN PRIVATE -fno-emulated-tls)
if(TARGET llm)
    target_compile_options(llm PRIVATE -fno-emulated-tls)
    if(EXISTS "${MNN_SOURCE_DIR}/transformers/llm/engine/src/minja/chat_template.hpp")
        target_compile_options(MNNWrapper PRIVATE -DLLM_USE_MINJA)
        target_include_directories(MNNWrapper PRIVATE
            ${MNN_SOURCE_DIR}/transformers/llm/engine/src
        )
    endif()
endif()

# Link MNN library
target_link_libraries(
    MNNWrapper
    MNN
    android
    log
    jnigraphics
)

# If LLM library exists, link it too
if(TARGET llm)
    target_link_libraries(MNNWrapper llm)
endif()

# Add 16KB page size support (Android 15+ requirement)
# This ensures all LOAD segments are aligned to 16KB boundaries
target_link_options(MNNWrapper PRIVATE "-Wl,-z,max-page-size=16384")
target_link_options(MNN PRIVATE "-Wl,-z,max-page-size=16384")
if(TARGET llm)
    target_link_options(llm PRIVATE "-Wl,-z,max-page-size=16384")
endif()

