#!/usr/bin/env bash
set -x

repository="softwarecraftsmen/"
branch="master"
version=""
build=$(cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 8 | head -n 1 | awk '{print tolower($0)}')

while getopts :r:b:v: o; do
    case "${o}" in
        r)
            repository=${OPTARG}
            ;;
        b)
            branch=${OPTARG}
            ;;
        v)
            version=${OPTARG}
            ;;
    esac
done
shift $((OPTIND-1))

if [ -z "${version}" ]; then
    APP_TAG="${branch}-${build}"
else
    APP_TAG="${version}"
fi

docker-compose build app
docker-compose push app
