FROM openjdk:8-jdk-slim
MAINTAINER Martin Ahrer <this@martinahrer.at>
RUN mkdir /opt/app
EXPOSE 8080
CMD java -jar /opt/app/continuousdelivery.jar
COPY continuousdelivery.jar /opt/app/continuousdelivery.jar