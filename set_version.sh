#!/bin/bash
BASE_DIR=$(dirname $0)

version="$1"
if [[ -z "$version" ]]; then
  echo "Usage: $0 <new-version>" > /dev/stderr
  exit 1
fi

echo "Updating version to $version"
echo "============================"

"$BASE_DIR/mvnw" versions:set -DgenerateBackupPoms=false -DnewVersion="$version"
