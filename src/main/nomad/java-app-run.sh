#!/usr/bin/env bash

nomad namespace apply "${NOMAD_NAMESPACE}"
nomad job run --var environment="${NOMAD_NAMESPACE}" --namespace="${NOMAD_NAMESPACE}" --var-file configuration.hcl --var-file credentials.hcl  --var-file java-configuration.hcl continuousdelivery.hcl

