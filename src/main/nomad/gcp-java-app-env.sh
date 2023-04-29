#!/usr/bin/env bash

pushd ${TERRAFORM_GCP_PROJECT}
export NOMAD_ADDR=$(terraform output -raw lb_address_consul_nomad):4646
export NOMAD_TOKEN=$(cat nomad_bootstrap.token)
export NOMAD_VAR_api_service_domain="$(terraform output -json instance_client_ip_addresses | jq -r '.[0]').nip.io"
export NOMAD_NAMESPACE="bootiful-java"
popd