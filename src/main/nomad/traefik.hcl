variable "consul_http_token" {
    type=string
    default=""
}

job "traefik" {
    region      = "global"
    datacenters = ["dc1"]

    type        = "system" # job will be deployed to each node

    group "traefik" {
        network {
            port "http" {
                static = 80
            }
            port "api" {
                static = 81
            }
        }

        task "traefik" {
            driver = "docker"
            config {
                image        = "traefik:2.9"
                volumes = [
                    "local/traefik.yml:/etc/traefik/traefik.yml",
                ]
              ports = [
                  "api", "http"
              ]
            }
            env {
                CONSUL_SOCKET_ADDRESS = "[[ .task.traefik.consul.socketaddress ]]"
                CONSUL_HTTP_TOKEN = "${var.consul_http_token}"
            }
            template {
                 data = <<EOF
[[ fileContents "traefik.yml" ]]
                EOF
                destination = "local/traefik.yml"
            }

            resources {
                cpu    = 100
                memory = 256
            }
        }

        service {
            name = "traefik"

            check {
                name     = "alive"
                type     = "http"
                port     = "api"
                interval = "10s"
                timeout  = "2s"
                path     = "/ping"
            }
        }
    }
}
