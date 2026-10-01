#cmake_minimum_required(VERSION 4.3)
cmake_minimum_required(VERSION 3.20)
#project(SteamAudioJni)
project(SteamAudioJni LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(JNI REQUIRED)

#add_library(SteamAudioJni SHARED library.cpp)

add_library(SteamAudioJni SHARED
        library.cpp
        # Заголовочные файлы указывать необязательно, но полезно для отображения в IDE (Visual Studio, CLion)
        steamaudio/include/phonon.h
        steamaudio/include/phonon_interfaces.h
        steamaudio/include/phonon_version.h
)

target_include_directories(SteamAudioJni PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/steamaudio/include
        ${JNI_INCLUDE_DIRS}
)

add_library(SteamAudio IMPORTED SHARED)

if (WIN32)
    # На Windows линковка идет с .lib, а в рантайме рядом должна лежать .dll
    set_target_properties(SteamAudio PROPERTIES
            IMPORTED_IMPLIB "${CMAKE_CURRENT_SOURCE_DIR}/steamaudio/lib/windows-x64/phonon.lib"
            IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/steamaudio/bin/windows-x64/phonon.dll"
    )
elseif (APPLE)
    set_target_properties(SteamAudio PROPERTIES
            IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/steamaudio/lib/osx/libphonon.dylib"
    )
elseif (UNIX) # Linux
    set_target_properties(SteamAudio PROPERTIES
            IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/steamaudio/lib/linux-x64/libphonon.so"
    )
endif()

target_link_libraries(SteamAudioJni PRIVATE
        SteamAudio
)