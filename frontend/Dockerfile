# 使用 Node.js 构建镜像
FROM node:alpine AS build

# 设置工作目录
WORKDIR /app

# 将 package.json 和 package-lock.json 复制到容器中
COPY package*.json ./
RUN ls -l
# 安装依赖
RUN npm install

# 复制源代码
COPY . .

# 构建 Vue 应用
RUN npm run build
RUN ls -l
# 使用 Nginx 来提供静态文件
FROM nginx:alpine

# 复制构建的文件到 Nginx 的 HTML 目录中
COPY --from=build /app/dist /usr/share/nginx/html

# 暴露 Nginx 端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]