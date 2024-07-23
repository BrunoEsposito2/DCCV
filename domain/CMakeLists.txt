cmake_minimum_required(VERSION 3.10)

project(domain)

# Set the OpenCV_DIR to the correct path
#set(OpenCV_DIR /usr/local/lib/cmake/opencv4)
# or
if(UNIX)
    # Per sistemi Unix-like (Linux, macOS)
    set(OpenCV_DIR /usr/local/share/opencv4)
# elseif(WIN32)
    # Per sistemi Windows
    # set(OpenCV_DIR C:/Users/*/Desktop/opencv/build)
else()
    message(FATAL_ERROR "Sistema operativo non supportato: compilare il programma c++ tramite docker")
endif()

find_package(OpenCV REQUIRED)

# Aggiungi le directory di inclusione
include_directories(${OpenCV_INCLUDE_DIRS})

add_executable(${PROJECT_NAME} src/main/cpp/app.cpp)

target_link_libraries(${PROJECT_NAME} ${OpenCV_LIBS})
