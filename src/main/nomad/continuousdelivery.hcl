job "continuousdelivery" {
    datacenters = ["dc1"]
    type = "service"

    spread {
        attribute = "${node.datacenter}"
    }

    group "continuousdelivery-api" {
        count = 3
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
                image       = "registry.gitlab.com/martinahrer/continuousdelivery:[[ .task.api.image.tag ]]"
                force_pull = true
                auth {
                    username = "[[ .registry.auth.username ]]"
                    password = "[[ .registry.auth.password ]]"
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

            service {
                name        = "continuousdelivery-api"
                port        = "http"
                tags        = [
                    "traefik.enable=true",
                    "traefik.tags=service",
                    "traefik.http.routers.api.entrypoints=http",
                    "traefik.http.routers.api.rule=PathPrefix(`/continuousdelivery/api/`)",
                    "traefik.http.routers.api.middlewares=api-stripprefix",
                    "traefik.http.middlewares.api-stripprefix.stripprefix.prefixes=/continuousdelivery",
                    "traefik.http.middlewares.api-stripprefix.stripprefix.forceSlash=true"
                ]

                canary_tags = [
                    "traefik.enable=false"
                ]
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
