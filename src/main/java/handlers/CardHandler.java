package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import database.LibraryManagementSystem;
import database.LibraryManagementSystemImpl;
import entities.Card;
import queries.ApiResult;
import queries.CardList;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;

public class CardHandler implements HttpHandler {
    // 关键重写handle方法
    private static DatabaseConnector connector = null;
    private static LibraryManagementSystem library = null;
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
            library = new LibraryManagementSystemImpl(connector);
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
        if (requestMethod.equals("GET")) {
            // 处理GET
            handleGetRequest(exchange);
        } else if (requestMethod.equals("POST")) {
            // 处理POST
            handlePostRequest(exchange);
        } else if (requestMethod.equals("OPTIONS")) {
            // 处理OPTIONS
            handleOptionsRequest(exchange);
        } else {
            // 其他请求返回405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }


    private static void handleGetRequest(HttpExchange exchange) throws IOException {
        System.out.println("GET request received in card handler");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // 状态码为200，也就是status ok
        exchange.sendResponseHeaders(200, 0);
        // 获取输出流，java用流对象来进行io操作
        OutputStream outputStream = exchange.getResponseBody();
        // 构建JSON响应数据，这里简化为字符串
        //调用showCards函数： 返回的return new ApiResult(true, "success",Results)，处理results：
        ApiResult result = CardHandler.library.showCards();
        CardList resCardList = (CardList) result.payload;
        String response = "[";
        System.out.println(resCardList.getCount());
        for (int i = 0; i < resCardList.getCount(); i++) {
            Card o2 = resCardList.getCards().get(i);
            response += "{\"id\": " + o2.getCardId() + ", \"name\": \"" + o2.getName() + "\", \"department\": \"" + o2.getDepartment() + "\", \"type\": \"" + o2.getType() + "\"}";
            if (i != resCardList.getCount() - 1)
                response += ",";
            //response.append("{\"id\": ").append(o2.getCardId()).append(", \"name\": \"").append(o2.getName()).append("\", \"department\": \"").append(o2.getDepartment()).append("\", \"type\": \"").append(o2.getType()).append("\"}");
        }
        response += "]";
        // 写
//        System.out.printf("Response: %s\n", response);
        outputStream.write(response.getBytes());
        // 流一定要close！！！小心泄漏
        outputStream.close();
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
        String card_Info = requestBodyBuilder.toString();
        //如果card_Info有id字段，调用updateCard函数，否则调用createCard函数：
        if (card_Info.contains("id") && card_Info.contains("name") && card_Info.contains("department") && card_Info.contains("type")) {
            //调用updateCard函数
            //解析请求体，获取卡片信息
            // 解析 id
            int idStartIndex = card_Info.indexOf(":") + 1; // 获取冒号后的索引位置
            int idEndIndex = card_Info.indexOf(",", idStartIndex); // 获取第一个逗号的位置
            int id = Integer.parseInt(card_Info.substring(idStartIndex, idEndIndex));
            // 解析 name
            int nameStartIndex = card_Info.indexOf("name") + 7; // 获取 "name" 后的索引位置
            int nameEndIndex = card_Info.indexOf("department") - 3; // 获取 "department" 前的索引位置
            String name = card_Info.substring(nameStartIndex, nameEndIndex);
            // 解析 department
            int departmentStartIndex = card_Info.indexOf("department") + 13; // 获取 "department" 后的索引位置
            int departmentEndIndex = card_Info.indexOf("type") - 3; // 获取 "type" 前的索引位置
            String department = card_Info.substring(departmentStartIndex, departmentEndIndex);
            // 解析 type
            int typeStartIndex = card_Info.indexOf("type") + 7; // 获取 "type" 后的索引位置
            int typeEndIndex = card_Info.length() - 2; // 获取最后一个字符前的索引位置（排除末尾的 `}`）
            String type = card_Info.substring(typeStartIndex, typeEndIndex);
            if (type.equals("Student"))
                type = "S";
            else if (type.equals("Teacher"))
                type = "T";
            //调用updateCard函数
            System.out.printf("id:%d,name:%s,dept:%s,type:%s\n", id, name, department, type);
            ApiResult result = CardHandler.library.ModifyCard(id, name, department, type);
            System.out.println("Update card result: " + result.toString());
            exchange.sendResponseHeaders(200, 0);
        } else if (card_Info.contains("id")) {
            //调用removeCard函数:
            // 解析 id
            int idStartIndex = card_Info.indexOf(":") + 1; // 获取冒号后的索引位置
            int idEndIndex = card_Info.indexOf("}", idStartIndex); // 获取第一个逗号的位置
            int id = Integer.parseInt(card_Info.substring(idStartIndex, idEndIndex));
            //调用removeCard函数
            System.out.printf("id:%d\n", id);
            ApiResult result = CardHandler.library.removeCard(id);
            System.out.println("Remove card result: " + result.toString());
            if (result.ok) {
                exchange.sendResponseHeaders(200, 0);
                System.out.println("Remove card successfully");
            } else {
                exchange.sendResponseHeaders(400, 0);
                System.out.println("Remove card failed");
            }


        } else {
            //调用createCard函数：
            // 解析 name
            int nameStartIndex = card_Info.indexOf("name") + 7; // 获取 "name" 后的索引位置
            int nameEndIndex = card_Info.indexOf("department") - 3; // 获取 "department" 前的索引位置
            String name = card_Info.substring(nameStartIndex, nameEndIndex);
            // 解析 department
            int departmentStartIndex = card_Info.indexOf("department") + 13; // 获取 "department" 后的索引位置
            int departmentEndIndex = card_Info.indexOf("type") - 3; // 获取 "type" 前的索引位置
            String department = card_Info.substring(departmentStartIndex, departmentEndIndex);
            // 解析 type
            int typeStartIndex = card_Info.indexOf("type") + 7; // 获取 "type" 后的索引位置
            int typeEndIndex = card_Info.length() - 2; // 获取最后一个字符前的索引位置（排除末尾的 `}`）
            String type = card_Info.substring(typeStartIndex, typeEndIndex);
            if (type.equals("Student"))
                type = "S";
            else if (type.equals("Teacher"))
                type = "T";
            //调用createCard函数
            System.out.printf("name:%s,dept:%s,type:%s\n", name, department, type);
            Card.CardType cardType = Card.CardType.values(type);
            Card card = new Card(0, name, department, cardType);
            ApiResult result = CardHandler.library.registerCard(card);
            System.out.println("Create card result: " + result.toString());
            exchange.sendResponseHeaders(200, 0);
        }


        // 2. 插入数据库
        // 响应头
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        // 响应状态码200


        // 剩下三个和GET一样
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write("Card created successfully".getBytes());
        outputStream.close();
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