FROM openjdk:17-slim-buster

# Install uuidgen, then delete apt indexes to save image space
RUN apt update \
    && apt install -y uuid-runtime \
    && rm -rf /var/cache/apt/archives /var/lib/apt/lists

WORKDIR /app

# Copy Registration Service client jar
COPY ./registration-service-cli.jar .

# Copy Identity Hub client jar
COPY ./identity-hub-cli.jar .

COPY ./entrypoint.sh .

ENTRYPOINT "/app/entrypoint.sh"
