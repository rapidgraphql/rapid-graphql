#!/bin/bash
BASE_DIR=$(dirname $0)

next_version=$(awk "-F[ <>]+" '$2=="parent" {skip=1} $2=="/parent" {skip=0} !skip && $2=="version" {split($3, vv, /\./); print(vv[1]"."vv[2]"."(vv[3]+1)); exit}' "$BASE_DIR/pom.xml")
if [[ -z "$next_version" ]]; then
  echo "Failed to extract version from pom" > /dev/stderr
  exit 1
fi

"$BASE_DIR/set_version.sh" "$next_version"

