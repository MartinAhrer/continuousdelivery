variable "api_image_tag" {
    type = string
    default = "latest"
}

variable "api_image_repository" {
    type = string
    default = "registry.gitlab.com/martinahrer/continuousdelivery"
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
        count = 3
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
            driver = "docker"
            config {
                image = "${var.api_image_repository}:${var.api_image_tag}"
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
                memory = 1024
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
                tags        = [ "api" ]
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
