#!/bin/bash
DIR="$( pwd )"
echo "Current directory: $DIR"

# Path del binario e delle librerie
BINARY_PATH="$DIR/build/release/domain/bin/domain"
LIB_PATH="$DIR/build/release/domain/libs"
VIDEO_PATH="$DIR/video/video.avi"

# Debug dei percorsi
echo "Binary path: $BINARY_PATH"
echo "Library path: $LIB_PATH"
echo "Video path: $VIDEO_PATH"

# Verifica che i file/directory esistano
echo "Checking paths:"
echo "Binary exists: $( [ -f "$BINARY_PATH" ] && echo "yes" || echo "no" )"
echo "Library dir exists: $( [ -d "$LIB_PATH" ] && echo "yes" || echo "no" )"
echo "Video exists: $( [ -f "$VIDEO_PATH" ] && echo "yes" || echo "no" )"

# Lista i contenuti delle directory rilevanti
echo "Contents of current directory:"
ls -la "$DIR"
echo "Contents of build directory (if exists):"
ls -la "$DIR/build" 2>/dev/null || echo "build directory not found"

# Imposta LD_LIBRARY_PATH
export LD_LIBRARY_PATH="$LIB_PATH:$LD_LIBRARY_PATH"
echo "LD_LIBRARY_PATH: $LD_LIBRARY_PATH"

# Esegui il binario solo se esiste
if [ -f "$BINARY_PATH" ]; then
    echo "Executing binary..."
    "$BINARY_PATH" -v="$VIDEO_PATH"
else
    echo "Error: Binary not found at $BINARY_PATH"
    exit 1
fi