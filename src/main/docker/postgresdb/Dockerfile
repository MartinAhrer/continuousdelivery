FROM postgres:9.6
MAINTAINER Martin Ahrer <this@martinahrer.at>

ADD create-database.sh /docker-entrypoint-initdb.d/create-database.sh
RUN chmod +x /docker-entrypoint-initdb.d/create-database.sh
