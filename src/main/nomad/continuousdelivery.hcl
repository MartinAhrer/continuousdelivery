variable "api_image_tag" {
    type = string
}

variable "registry_auth_username" {
    type = string
}

variable "registry_auth_password" {
    type = string
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
                name = "continuousdelivery-db"
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
                to = "8080"
            }
            port "management_http" {
                to = "8081"
            }
        }

        task "api" {
            driver = "docker"
            config {
                image = "registry.gitlab.com/martinahrer/continuousdelivery:${var.api_image_tag}"
                auth {
                    username = "${var.registry_auth_username}"
                    password = "${var.registry_auth_password}"
                }
                ports = [
                    "http",
                    "management_http"
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
                SERVER_PORT             = "8080"
                MANAGEMENT_SERVER_PORT  = "8081"
                SPRING_JPA_GENERATE_DDL = true
            }
            template {
                destination="application.env"
                env = true
                data = <<EOH
                SPRING_DATASOURCE_URL=jdbc:postgresql://{{ range service "continuousdelivery-db" }}{{ .Address }}:{{ .Port }}{{ end }}/app
                SPRING_DATASOURCE_USERNAME="spring"
                SPRING_DATASOURCE_PASSWORD="boot"
                EOH
            }
            service {
                name        = "continuousdelivery-api"
                provider    = "consul"
                port        = "http"
                tags        = [ "api" ]
                check {
                    name     = "continuousdelivery-api-check"
                    type     = "http"
                    port     = "management_http"
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
