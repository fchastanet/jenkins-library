#!/bin/bash
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

mkdir -p "${BASE_DIR}/doc" || true

if (($# == 0)); then
	set -- node doc2md.js /tmp groovy /tmp/doc
fi

if command -v docker &>/dev/null; then
	docker run --rm -i \
		-v "${BASE_DIR}":/tmp \
		scrasnups/build:node-groovydoc-to-markdown-1.0.9 \
		"$@"
else
	"$@"
fi
