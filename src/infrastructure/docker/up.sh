#!/usr/bin/env bash
set -x

__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export JENKINS_URL=http://$(ipconfig getifaddr en0):18080
docker-compose up -d
# This fixes the permissions on the nexus-docker-data volume.
# Otherwise we would have to hard-code this into a docker image.
docker-compose exec --user root repository bash -c "chown -R nexus:nexus /nexus-docker-data/"
