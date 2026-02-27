FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .
COPY checkstyle.xml .
COPY checkstyle-suppressions.xml .

COPY cerps-common cerps-common
COPY currency-service currency-service
COPY analytics-service analytics-service

RUN mvn clean package -pl analytics-service -am -Dmaven.test.skip=true -Dcheckstyle.skip=true -Dpmd.skip=true

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/analytics-service/target/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS \
  -XX:+UseSerialGC \
  -XX:MaxRAM=128m \
  -Xms32m \
  -Xmx64m \
  -XX:MaxMetaspaceSize=64m \
  -XX:+UseCompressedOops \
  -XX:+UseCompressedClassPointers \
  -XX:CompressedClassSpaceSize=16m \
  -XX:ReservedCodeCacheSize=16m \
  -XX:MaxDirectMemorySize=8m \
  -Xss256k \
  -XX:+ExitOnOutOfMemoryError \
  -Dspring.jmx.enabled=false \
  -Dspring.config.additional-location=optional:file:/app/config/ \
  -jar app.jar"]
