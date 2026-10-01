#cmake_minimum_required(VERSION 4.3)
cmake_minimum_required(VERSION 3.20)
#project(SteamAudioJni)
project(SteamAudioJni LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(JNI REQUIRED)

# Готовая DLL должна лежать рядом с JNI-библиотекой: Java грузит обе
# из одного каталога (System.load по абсолютному пути).
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")

add_library(SteamAudioJni SHARED
        src/ambisonics_effect.cpp
        src/audio_buffer.cpp
        src/binaural_effect.cpp
        src/context.cpp
        src/direct_effect.cpp
        src/energy_field.cpp
        src/gpu_devices.cpp
        src/hrtf.cpp
        src/impulse_response.cpp
        src/instanced_mesh.cpp
        src/path_baker.cpp
        src/path_effect.cpp
        src/panning_effect.cpp
        src/probe_batch.cpp
        src/reconstructor.cpp
        src/reflection_effect.cpp
        src/reflection_mixer.cpp
        src/reflections_baker.cpp
        src/scene.cpp
        src/serialized_object.cpp
        src/simulator.cpp
        src/source.cpp
        src/static_mesh.cpp
        # Заголовочные файлы указывать необязательно, но полезно для отображения в IDE (Visual Studio, CLion)
        src/steamaudio_jni.h
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
            IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/steamaudio/lib/windows-x64/phonon.dll"
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

if (WIN32)
    add_custom_command(TARGET SteamAudioJni POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
                    "${CMAKE_CURRENT_SOURCE_DIR}/steamaudio/lib/windows-x64/phonon.dll"
                    $<TARGET_FILE_DIR:SteamAudioJni>
            COMMENT "Copying phonon.dll next to SteamAudioJni"
    )
elseif (APPLE)
    add_custom_command(TARGET SteamAudioJni POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
                    "${CMAKE_CURRENT_SOURCE_DIR}/steamaudio/lib/osx/libphonon.dylib"
                    $<TARGET_FILE_DIR:SteamAudioJni>
            COMMENT "Copying libphonon.dylib next to SteamAudioJni"
    )
elseif (UNIX)
    add_custom_command(TARGET SteamAudioJni POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
                    "${CMAKE_CURRENT_SOURCE_DIR}/steamaudio/lib/linux-x64/libphonon.so"
                    $<TARGET_FILE_DIR:SteamAudioJni>
            COMMENT "Copying libphonon.so next to SteamAudioJni"
    )
endif()