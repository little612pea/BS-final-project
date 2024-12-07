# 第一阶段：使用 Maven 编译项目
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 第二阶段：设置运行环境并安装依赖
FROM openjdk:17-alpine
WORKDIR /app

# 安装 Selenium 所需的依赖库和 Chrome 环境
RUN apk update && apk add --no-cache \
    bash \
    chromium \
    chromium-chromedriver

# 设置环境变量，使 Selenium 可以找到 Chrome 和 ChromeDriver
ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_DRIVER=/usr/bin/chromedriver

# 复制构建好的 JAR 文件到最终镜像
COPY --from=build /app/target/bs-final-project-1.0.jar /app/backend.jar

# 设置 CMD 以运行 Java 应用程序
CMD ["java", "-jar", "/app/backend.jar"]
