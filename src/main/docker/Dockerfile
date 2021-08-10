FROM adoptopenjdk/openjdk11:alpine-jre
LABEL maintainer='Martin Ahrer <this@martinahrer.at>'
EXPOSE 8080
CMD ["java", "-jar", "/opt/app/app.jar"]
RUN apk update && apk --no-cache add curl && \
    rm -r /var/cache/apk/*
HEALTHCHECK  --interval=5m --timeout=3s --start-period=10s CMD curl -f http://localhost:8080/actuator/health
RUN mkdir /opt/app
COPY continuousdelivery.jar /opt/app/app.jar
