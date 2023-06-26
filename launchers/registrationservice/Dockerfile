# -buster is required to have apt available
FROM openjdk:17-slim-buster

# Optional JVM arguments, such as memory settings
ARG JVM_ARGS=""

# Install curl, then delete apt indexes to save image space
RUN apt update \
    && apt install -y curl \
    && rm -rf /var/cache/apt/archives /var/lib/apt/lists

WORKDIR /app

COPY ./build/libs/registrationservice.jar /app

EXPOSE 8182

# health status is determined by the availability of the /health endpoint
HEALTHCHECK --interval=5s --timeout=5s --retries=10 CMD curl --fail http://localhost:8181/api/check/health

ENV WEB_HTTP_PORT="8181"
ENV WEB_HTTP_PATH="/api"
ENV WEB_HTTP_IDENTITY_PORT="7171"
ENV WEB_HTTP_IDENTITY_PATH="/api/identity"
ENV WEB_HTTP_AUTHORITY_PORT="8184"
ENV WEB_HTTP_AUTHORITY_PATH="/api/authority"

# Use "exec" for graceful termination (SIGINT) to reach JVM.
# ARG can not be used in ENTRYPOINT so storing value in an ENV variable
ENV ENV_JVM_ARGS=$JVM_ARGS
ENTRYPOINT [ "sh", "-c", \
    "exec java $ENV_JVM_ARGS -jar registrationservice.jar"]
