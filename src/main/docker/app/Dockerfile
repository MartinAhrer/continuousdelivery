FROM openjdk:8-jre-alpine
LABEL maintainer='Martin Ahrer <this@martinahrer.at>'
RUN mkdir /opt/app
EXPOSE 8080
CMD java -jar /opt/app/app.jar
COPY continuousdelivery.jar /opt/app/app.jar