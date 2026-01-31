# Simple Dockerfile using pre-built JAR
FROM amazoncorretto:17

# Install curl for health check
RUN yum install -y curl && yum clean all

WORKDIR /app
COPY target/task-async-service-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
