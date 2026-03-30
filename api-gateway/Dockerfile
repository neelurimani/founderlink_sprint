FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY . .
RUN mvn -pl api-gateway -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/api-gateway/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
