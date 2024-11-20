#!/bin/bash
DIR="$( pwd )"
export LD_LIBRARY_PATH="$DIR/domain/build/release/domain/libs:$LD_LIBRARY_PATH"
"$DIR/domain/build/release/domain/bin/domain" -v="$DIR/domain/video/video.avi"