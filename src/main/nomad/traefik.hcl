job "traefik" {
    region      = "global"
    datacenters = ["dc1"]

    type        = "system" # job will be deployed to each node

    group "traefik" {
        network {
            port "http" {
                static = 8080
            }
            port "api" {
                static = 8081
            }
        }

        task "traefik" {
            driver = "docker"
            config {
                image        = "traefik:2.7"
                volumes = [
                    "local/traefik.yml:/etc/traefik/traefik.yml",
                ]
              ports = [
                  "api", "http"
              ]
            }
            env {
                CONSUL_SOCKET_ADDRESS = "[[ .task.traefik.consul.socketaddress ]]"
            }
            template {
                # traefik configuration is loaded from external file
                # https://learn.hashicorp.com/tutorials/nomad/dry-jobs-levant?in=nomad/templates
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
