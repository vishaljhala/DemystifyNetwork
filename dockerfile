FROM eclipse-temurin:17
RUN addgroup spring && adduser spring --ingroup spring
USER spring:spring
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} demystify_network_backend.jar
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=production", "/demystify_network_backend.jar"]

