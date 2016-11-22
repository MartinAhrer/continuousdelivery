FROM openjdk:8-jdk
MAINTAINER Martin Ahrer <this@martinahrer.at>

RUN mkdir /opt/app

ADD continuousdelivery-@version@-exec.jar /opt/app/app.jar

EXPOSE 8080

CMD "bash -c \"java -jar /opt/app/app.jar\""