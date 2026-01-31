# Simple Dockerfile using pre-built JAR
FROM amazoncorretto:17

# Install curl for health check
RUN yum install -y curl && yum clean all

# 在宿主机有个环境把jar先打包好直接拷贝进docker镜像里面，不是每次启动的时候在docker compose环境里做build再生成镜像
WORKDIR /app
COPY target/task-async-service-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
