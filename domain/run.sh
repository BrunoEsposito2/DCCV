#!/bin/bash
DIR="$( pwd )"
export LD_LIBRARY_PATH="$DIR/build/release/domain/libs:$LD_LIBRARY_PATH"
"$DIR/build/release/domain/bin/domain" -v="$DIR/video/video.avi"