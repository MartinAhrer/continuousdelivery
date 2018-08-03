FROM openjdk:8-jdk-slim
MAINTAINER Martin Ahrer <this@martinahrer.at>

RUN mkdir /opt/app

ADD continuousdelivery-@version@.jar /opt/app/app.jar

EXPOSE 8080

CMD java -jar /opt/app/app.jar