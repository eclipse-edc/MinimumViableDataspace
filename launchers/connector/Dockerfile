# -buster is required to have apt available
FROM openjdk:17-slim-buster

# Optional JVM arguments, such as memory settings
ARG JVM_ARGS=""

# Install curl, then delete apt indexes to save image space
RUN apt update \
    && apt install -y curl \
    && rm -rf /var/cache/apt/archives /var/lib/apt/lists

WORKDIR /app

COPY ./build/libs/connector.jar /app

EXPOSE 8181
EXPOSE 9191
EXPOSE 8282
EXPOSE 8383

# health status is determined by the availability of the /health endpoint
HEALTHCHECK --interval=5s --timeout=5s --retries=10 CMD curl -H "X-Api-Key: $EDC_API_AUTH_KEY" --fail http://localhost:8181/api/check/health

ENV WEB_HTTP_PORT="8181"
ENV WEB_HTTP_PATH="/api"
ENV WEB_HTTP_CONTROL_PORT="8383"
ENV WEB_HTTP_CONTROL_PATH="/api/control"
ENV WEB_HTTP_MANAGEMENT_PORT="9191"
ENV WEB_HTTP_MANAGEMENT_PATH="/api/management"
ENV WEB_HTTP_PROTOCOL_PORT="8282"
ENV WEB_HTTP_PROTOCOL_PATH="/api/dsp"
ENV WEB_HTTP_IDENTITY_PORT="7171"
ENV WEB_HTTP_IDENTITY_PATH="/api/identity"

# Use "exec" for graceful termination (SIGINT) to reach JVM.
# ARG can not be used in ENTRYPOINT so storing values in ENV variables
ENV JVM_ARGS=$JVM_ARGS
ENTRYPOINT [ "sh", "-c", \
    "exec java $JVM_ARGS -jar connector.jar"]
