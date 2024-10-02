# 第一阶段：使用 Maven 编译项目
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 第二阶段：设置 Python 环境并安装依赖
FROM python:3.9 AS python-env
WORKDIR /app
# 将 Python 脚本复制到 Python 环境中
COPY src/crawler/ ./python-scripts/

# 第三阶段：合并 Java 和 Python 环境，并在 Alpine 中安装 Python
FROM openjdk:17-alpine
WORKDIR /app
# 安装 Python 和 pip
RUN apk add --no-cache \
    gcc \
    musl-dev \
    python3-dev \
    libffi-dev \
    libxml2-dev \
    libxslt-dev \
    openssl-dev \
# 更新并安装必要的依赖库和工具
RUN apk update && apk upgrade && \
    apk add --no-cache \
    bash \
    chromium \
    chromium-chromedriver \
    curl \
    build-base \
    python3-dev \
    libffi-dev \
    gcc \
    musl-dev \
    libgcc \
    linux-headers \
    g++ \
    make \
    xvfb

# 设置环境变量，使 Selenium 可以找到 Chrome 和 ChromeDriver
ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_DRIVER=/usr/bin/chromedriver
RUN apk add --no-cache python3 py3-pip
COPY src/crawler/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# 复制 Python 脚本和 Java 项目到最终镜像
COPY --from=python-env /app/python-scripts/ ./python-scripts/
COPY --from=build /app/target/bs-final-project-1.0.jar /app/backend.jar

# 设置 CMD 以运行 Java 应用程序
CMD ["java", "-jar", "/app/backend.jar"]
