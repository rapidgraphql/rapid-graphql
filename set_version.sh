#!/bin/bash
BASE_DIR=$(dirname $0)
curr_version=$(awk "-F[ <>]+" '$2=="parent" {skip=1} $2=="/parent" {skip=0} !skip && $2=="version" {print($3); exit}' "$BASE_DIR/pom.xml")
if [[ -z "$curr_version" ]]; then
  echo "Failed to extract version from pom" > /dev/stderr
  exit 1
fi
version="$1"
sed -i ".bak" -e "s/$curr_version/$version/g" $BASE_DIR/README.md
rm -fr $BASE_DIR/README.md.bak

if [[ -z "$version" ]]; then
  echo "Usage: $0 <new-version>" > /dev/stderr
  exit 1
fi

echo "Updating version to $version"
echo "============================"

"$BASE_DIR/mvnw" versions:set -DgenerateBackupPoms=false -DnewVersion="$version"
