FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .
COPY checkstyle.xml .
COPY checkstyle-suppressions.xml .

COPY cerps-common cerps-common
COPY currency-service currency-service
COPY user-service user-service
COPY analytics-service analytics-service
COPY mock-service-1 mock-service-1
COPY mock-service-2 mock-service-2

RUN mvn clean package -pl analytics-service -am -Dmaven.test.skip=true -Dcheckstyle.skip=true -Dpmd.skip=true

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/analytics-service/target/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
