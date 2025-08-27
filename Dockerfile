FROM openjdk:17-jdk-slim
WORKDIR /usr/app
ADD target/*.jar app.jar
# --- Datadog defaults (can be overridden at runtime) ---
ENV DD_SERVICE="healthcare-service" \
    DD_ENV="dev" \
    DD_VERSION="0.0.1" \
    DD_LOGS_INJECTION="true" \
    DD_DYNAMIC_INSTRUMENTATION_ENABLED="true"
# Optional: source-code integration (set from CI)
ARG COMMIT_SHA=""
ENV DD_GIT_COMMIT_SHA="${COMMIT_SHA}"
ENV DD_GIT_REPOSITORY_URL="https://github.com/Sumanth17-git/healthcare-service.git"

EXPOSE 8081
ENTRYPOINT exec java  -jar app.jar

