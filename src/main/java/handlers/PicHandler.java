package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.Base64;

import static utils.JsonUtils.extractValue;

public class PicHandler implements HttpHandler {

    private static final String BASE_PATH = "upload";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 允许跨域请求
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");

        // 解析请求方法
        String requestMethod = exchange.getRequestMethod();

        if (requestMethod.equalsIgnoreCase("POST")) {
            handleFileUpload(exchange);
        } else if (requestMethod.equalsIgnoreCase("OPTIONS")) {
            handleOptionsRequest(exchange);
        } else {
            // 返回 405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }

    /**
     * 处理文件下载请求（POST 方法）
     */
    private void handleFileUpload(HttpExchange exchange) throws IOException {
        // 读取请求体内容
        InputStream requestBody = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8));
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
        }
        String requestData = requestBodyBuilder.toString();

        // 假设客户端发送的是表单数据：image=<Base64Data>&name=<FileName>

        String base64Data = null;
        String fileName = null;
        base64Data = extractValue(requestData,"image",String::valueOf);
        fileName = extractValue(requestData,"name",String::valueOf);

        if (base64Data == null || fileName == null) {
            sendErrorResponse(exchange, "Missing or invalid image data or file name", 400);
            return;
        }

        // 解码Base64数据
        byte[] fileData = Base64.getDecoder().decode(base64Data);

        // 生成唯一的文件名
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        String newFileName = "PhotoSearch.png";

        // 保存文件
        File dir = new File(BASE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream( newFileName)) {
            fileOutputStream.write(fileData);
        } catch (IOException e) {
            sendErrorResponse(exchange, "Failed to save the image", 500);
            return;
        }

        // 设置响应头
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, 0);

        // 发送成功响应
        String response = "{\"message\": \"Image uploaded successfully\", \"file\": \"" + newFileName + "\"}";
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("File uploaded successfully: " + newFileName);
    }

    /**
     * 处理OPTIONS请求（CORS预检请求）
     */
    private static void handleOptionsRequest(HttpExchange exchange) throws IOException {
        // 设置响应头
        exchange.getResponseHeaders().set("Allow", "POST");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
        exchange.getResponseHeaders().set("Content-Length", "0");

        // 发送响应状态码 204 (No Content)
        exchange.sendResponseHeaders(204, -1);

        // 结束请求
        exchange.close();
    }

    /**
     * 从查询参数中提取文件名
     */
    private String getFileNameFromQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase("file")) {
                return keyValue[1];
            }
        }
        return null;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
        System.out.println("Error: " + message);
    }
}
