package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import database.PriceCompSystem;
import database.PriceCompSystemImpl;
import queries.ApiResult;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;

public class LoginHandler implements HttpHandler {
    private static DatabaseConnector connector = null;
    private static PriceCompSystem library = null;
    private static ConnectConfig connectConfig = null;

    static {
        try {
            // parse connection config from "resources/application.yaml"
            connectConfig = new ConnectConfig();
            // 调用libraryTest方法
            LibraryTest();
            // 调用prepareTest方法
            prepareTest();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void LibraryTest() {
        try {
            // connect to database
            connector = new DatabaseConnector(connectConfig);
            library = new PriceCompSystemImpl(connector);
            System.out.println("Successfully init class ProductTest.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void prepareTest() {
        boolean connStatus = connector.connect();
        if (!connStatus) {
            System.out.println("Failed to connect database.");
            System.exit(1);
        }
        System.out.println("Successfully connect to database.");
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 允许所有域的请求，cors处理
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        // 解析请求的方法，看GET还是POST
        String requestMethod = exchange.getRequestMethod();
        // 注意判断要用equals方法而不是==啊，java的小坑（
        if (requestMethod.equals("POST")) {
            // 处理POST
            handlePostRequest(exchange);
        }else if (requestMethod.equals("OPTIONS")){
            handleOptionsRequest(exchange);
        }

        else {
            // 其他请求返回405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private static void handlePostRequest(HttpExchange exchange) throws IOException {
        // 读取POST请求体
        System.out.println("POST request received");
        InputStream requestBody = exchange.getRequestBody();
        // 用这个请求体（输入流）构造个buffered reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
        // 拼字符串的
        StringBuilder requestBodyBuilder = new StringBuilder();
        // 用来读的
        String line;

        // 没读完，一直读，拼到string builder里
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
        }
        System.out.println("Received POST request data: " + requestBodyBuilder.toString());
        //调用数据库操作：
        // 1. 解析请求体，获取卡片信息
        String query = requestBodyBuilder.toString();
        // 解析 username
        int usernameStartIndex = query.indexOf("name") + 7; // 获取 "username" 后的索引位置
        int usernameEndIndex = query.indexOf(",", usernameStartIndex)-1; // 获取第一个逗号的位置
        String username = query.substring(usernameStartIndex, usernameEndIndex);
        // 解析 password
        int passwordStartIndex = query.indexOf("password") + 11; // 获取 "password" 后的索引位置
        int passwordEndIndex = query.length()-3; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
        String password = query.substring(passwordStartIndex, passwordEndIndex);
        //调用register函数
        System.out.printf("username:%s,password:%s\n", username
                , password);
        //调用showCards函数： 返回的return new ApiResult(true, "success",Results)，处理results：
        ApiResult result = LoginHandler.library.login(username,password);
        // 流一定要close！！！小心泄漏
        if (result.ok) {
            System.out.println("login successful!");
            exchange.sendResponseHeaders(200, 0);
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("login successful".getBytes());
            outputStream.close();
        } else {
            System.out.println("login failed!");
            exchange.sendResponseHeaders(400, 0);
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("login failed".getBytes());
            outputStream.close();
        }
    }
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
}