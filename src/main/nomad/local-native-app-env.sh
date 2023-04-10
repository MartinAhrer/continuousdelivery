#!/usr/bin/env bash
export NOMAD_VAR_api_service_domain="$(ipconfig getifaddr en0).nip.io"
export NOMAD_NAMESPACE="bootiful-native"