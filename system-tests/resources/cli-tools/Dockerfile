FROM openjdk:17-slim-buster

# Install uuidgen and jq then delete apt indexes to save image space
RUN apt update \
    && apt install -y uuid-runtime jq \
    && rm -rf /var/cache/apt/archives /var/lib/apt/lists

WORKDIR /app

# Copy Registration Service client jar
COPY ./registration-service-cli.jar .

# Copy Identity Hub client jar
COPY ./identity-hub-cli.jar .

COPY ./entrypoint.sh .
COPY ./validate_onboarding.sh .

# health status is determined by the success of the entrypoint script
HEALTHCHECK --start-period=60s --interval=5s --retries=3 CMD test -f finished.flag

ENTRYPOINT "/app/entrypoint.sh"
