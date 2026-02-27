FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY checkstyle.xml .
COPY checkstyle-suppressions.xml .
COPY src ./src

RUN mvn clean package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache ca-certificates openssl

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
COPY certificates/*.crt /tmp/certs/

RUN keytool -import -trustcacerts -noprompt \
    -alias cloud-services-root -file "/tmp/certs/Cloud Services Root CA.crt" \
    -keystore /opt/java/openjdk/lib/security/cacerts \
    -storepass changeit && \
    rm -rf /tmp/certs

EXPOSE 8080

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
