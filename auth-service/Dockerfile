# ---------- Giai đoạn 1: build file jar bằng Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml trước để Docker cache lại bước tải thư viện
# (chỉ tải lại khi pom.xml thay đổi)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy source code và đóng gói
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Giai đoạn 2: image chạy ứng dụng (chỉ cần JRE, nhẹ hơn) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/auth-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]