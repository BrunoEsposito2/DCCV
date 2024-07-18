cmake_minimum_required(VERSION 3.10)

project(app)

# Set the OpenCV_DIR to the correct path
#set(OpenCV_DIR /usr/local/lib/cmake/opencv4)
# or
set(OpenCV_DIR /usr/local/share/opencv4)

find_package(OpenCV REQUIRED)

# Aggiungi le directory di inclusione
include_directories(${OpenCV_INCLUDE_DIRS})

add_executable(${PROJECT_NAME} src/main/cpp/app.cpp)

target_link_libraries(${PROJECT_NAME} ${OpenCV_LIBS})
