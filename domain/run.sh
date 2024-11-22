#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export LD_LIBRARY_PATH="$DIR/libs:$LD_LIBRARY_PATH"
"/workspace/domain/build/release/domain/bin/domain" -v="/workspace/domain/video/video.avi" -id=3