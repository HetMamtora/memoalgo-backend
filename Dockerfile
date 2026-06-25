# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first and resolve dependencies before copying source --
# Docker layer caching means dependency resolution only re-runs when
# pom.xml actually changes, not on every source-code edit.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Runtime stage ----
# JRE (not JDK) -- no compiler needed at runtime, smaller image.
# Alpine -- smaller still, matters more on a constrained free-tier disk.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Render's free tier gives 512MB RAM total. Left to its own defaults,
# the JVM can misjudge how much of that it actually has inside a
# container and get OOM-killed by the OS -- which looks exactly like a
# random crash, not a clean Java error. Setting explicit bounds:
#   -Xmx192m              heap ceiling
#   -XX:MaxMetaspaceSize  class-metadata ceiling (Hibernate/Spring use
#                         a fair amount of this loading entity/proxy
#                         classes)
#   -XX:+UseSerialGC      far less native bookkeeping overhead than the
#                         default G1GC -- the right tradeoff for a small
#                         heap and low traffic, where GC throughput
#                         doesn't matter but memory headroom does
# 192 (heap) + 128 (metaspace) + thread stacks/JIT/native overhead
# leaves real margin under 512MB total.
ENTRYPOINT ["java", "-Xmx192m", "-XX:MaxMetaspaceSize=128m", "-XX:+UseSerialGC", "-jar", "app.jar"]
