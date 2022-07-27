FROM openjdk:17-slim-buster

WORKDIR /app

# Copy Registration Service client jar
COPY ./registration-service-cli.jar .
COPY ./entrypoint.sh .

ENTRYPOINT "/app/entrypoint.sh"
