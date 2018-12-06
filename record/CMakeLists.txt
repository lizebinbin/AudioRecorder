# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(distribution_DIR ${CMAKE_SOURCE_DIR}/libs)

add_library(fmod
        SHARED
        IMPORTED)

set_target_properties(fmod
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libfmod.so)

add_library(fmodL
        SHARED
        IMPORTED)

set_target_properties(fmodL
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libfmodL.so)


# 查找cpp目录下的所有源文件
# 并将名称保存到 DIR_LIB_SRC 变量
aux_source_directory(src/main/cpp/ DIR_LIB_SRC)

#设置源文件
add_library(voice
        SHARED
        ${DIR_LIB_SRC})

#表明头文件位置
include_directories(src/main/cpp/inc)


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        voice fmod fmodL

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})