variable "api_artifact_name" {
    type=string
    default="continuousdelivery"
}

variable "api_artifact_version" {
    type = string
    default = "latest"
}

variable "api_artifact_repository" {
    type = string
    default = "registry.gitlab.com/martinahrer"
}

variable "registry_auth_username" {
    type = string
}

variable "registry_auth_password" {
    type = string
}

variable "environment" {
    type = string
}

variable "api_http_port" {
    type = number
    default = 8080
}

variable "api_task_driver" {
    type=string
    default="docker"
}

variable "repository_private_token" {
    type=string
    default="UNUSED"
}

variable "api_service_domain" {
    type=string
    default="192.168.1.36.nip.io"
}

variable "api_count" {
    type=number
    default=3
}

job "continuousdelivery" {
    datacenters = ["dc1"]
    type = "service"

    group "db" {
        count = 1
        network {
            port "postgresdb" {
                to = "5432"
            }
        }

        task "db" {
            driver = "docker"
            config {
                image = "bitnami/postgresql:11"
                ports = [ "postgresdb" ]
            }
            env {
                POSTGRESQL_DATABASE="app"
                POSTGRESQL_USERNAME="spring"
                POSTGRESQL_PASSWORD="boot"
            }
            service {
                name = "continuousdelivery-db-${var.environment}"
                provider = "consul"
                port = "postgresdb"
                tags = [ "db" ]
                check {
                    name     = "db-check"
                    type     = "tcp"
                    interval = "60s"
                    timeout  = "4s"
                    check_restart {
                        grace = "20s"
                    }
                }
            }
        }
    }

    group "api" {
        count = "${var.api_count}"
        spread {
            attribute = "${node.datacenter}"
        }
        update {
            max_parallel     = 1
            canary           = 1
            min_healthy_time = "40s"
            auto_revert      = true
            auto_promote     = false
        }
        network {
            port "http" {
                to = "${var.api_http_port}"
            }
        }

        task "api" {
            driver = "${var.api_task_driver}"
            config {
                image = "${var.api_artifact_repository}/${var.api_artifact_name}:${var.api_artifact_version}"
                auth {
                    username = "${var.registry_auth_username}"
                    password = "${var.registry_auth_password}"
                }
                ports = [
                    "http"
                ]
                dns_servers = [
                    "1.1.1.1",
                    "1.0.0.1"
                ]
            }
            resources {
                memory = 512
            }
            env {
                SPRING_JPA_GENERATE_DDL = true
            }
            template {
                destination="application.env"
                env = true
                data = <<EOH
                SERVER_PORT             = "${NOMAD_PORT_http}"
                MANAGEMENT_SERVER_PORT  = "${NOMAD_PORT_http}"
                SPRING_PROFILES_ACTIVE  = "production"
                SPRING_DATASOURCE_URL=jdbc:postgresql://{{ range service "continuousdelivery-db-${var.environment}" }}{{ .Address }}:{{ .Port }}{{ end }}/app
                SPRING_DATASOURCE_USERNAME="spring"
                SPRING_DATASOURCE_PASSWORD="boot"
                EOH
            }
            service {
                name        = "continuousdelivery-api-${var.environment}"
                provider    = "consul"
                port        = "http"

                tags        = [
                    "traefik.enable=true",
                    "traefik.tags=service",
                    "traefik.http.routers.continuousdelivery-api-${var.environment}.entrypoints=http",
                    "traefik.http.routers.continuousdelivery-api-${var.environment}.rule=Host(`continuousdelivery-api-${var.environment}.${var.api_service_domain}`)",
                ]
                canary_tags = [
                    "traefik.nomad.canary=true",
                    "traefik.enable=true",
                    "traefik.http.routers.continuousdelivery-api-${var.environment}.entrypoints=http",
                    "traefik.http.routers.continuousdelivery-api-${var.environment}.rule=Host(`canary.continuousdelivery-api-${var.environment}.${var.api_service_domain}`)",
                ]
                check {
                    name     = "continuousdelivery-api-check"
                    type     = "http"
                    port     = "http"
                    path     = "/actuator/health"
                    interval = "60s"
                    timeout  = "10s"
                    check_restart {
                        grace = "60s"
                    }
                }
            }
        }
    }
}
