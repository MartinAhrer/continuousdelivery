FROM openjdk:8-jre
MAINTAINER Martin Ahrer <this@martinahrer.at>

RUN mkdir /opt/app

ADD continuousdelivery-@version@-exec.jar /opt/app/app.jar

EXPOSE 8080

CMD java -jar /opt/app/app.jar