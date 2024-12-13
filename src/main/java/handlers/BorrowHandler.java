package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import database.LibraryManagementSystem;
import database.LibraryManagementSystemImpl;
import entities.Borrow;
import queries.ApiResult;
import queries.BorrowHistories;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BorrowHandler implements HttpHandler {
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

    static void handleGetRequest(HttpExchange exchange) throws IOException {
        System.out.println("GET borrow query request received");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // 状态码为200，也就是status ok
        exchange.sendResponseHeaders(200, 0);
        // 获取输出流，java用流对象来进行io操作
        OutputStream outputStream = exchange.getResponseBody();
        // 获取请求的URI
        URI requestedUri = exchange.getRequestURI();
        // 解析查询字符串
        String query = requestedUri.getRawQuery();
        System.out.println("query: " + query);
        // 解析查询字符串,假设就只有一个参数：card_id，格式为cardID=xxx
        int card_id = Integer.parseInt(query.split("=")[1]);
        System.out.println("Received card_id: " + card_id);
        ApiResult result = BorrowHandler.library.showBorrowHistory(card_id);
        BorrowHistories resBorrowHistories = (BorrowHistories) result.payload;
        List<BorrowHistories.Item> borrow_items = resBorrowHistories.getItems();
        String response = "[";
        System.out.println(resBorrowHistories.getCount());
        for (int i = 0; i < resBorrowHistories.getCount(); i++) {
            BorrowHistories.Item o2 = borrow_items.get(i);
            String iso_borrowTime = new SimpleDateFormat("yyyy.MM.dd HH:mm").format(new Date(o2.getBorrowTime()));
            String iso_returnTime = new SimpleDateFormat("yyyy.MM.dd HH:mm").format(new Date(o2.getReturnTime()));
            if (o2.getReturnTime() == 0)
                iso_returnTime = "product unreturned";
            response += "{\"cardID\": " + o2.getCardId() + ", \"productId\": " + o2.getProductId() + ", \"borrowTime\": \"" + iso_borrowTime + "\", \"unix_borrowTime\": " + o2.getBorrowTime() + ", \"returnTime\": \"" + iso_returnTime + "\", \"unix_returnTime\": " + o2.getReturnTime() + "}";
            if (i != resBorrowHistories.getCount() - 1)
                response += ",";
            //将unix时间戳转换为2024.03.04 21:48格式：
            //System.out.println("borrowTime: \n"+iso_borrowTime);
            //System.out.println("returnTime: \n"+iso_returnTime);
        }
        response += "]";
        // 写

// 转换为ISO 8601格式的字符串

        System.out.printf("borrow query Response: %s\n", response);
        outputStream.write(response.getBytes());
        // 流一定要close！！！小心泄漏
        outputStream.close();
    }

    static void handlePostRequest(HttpExchange exchange) throws IOException {
        //还书操作：
        // 读取POST请求体
        System.out.println("POST request received");
        InputStream requestBody = exchange.getRequestBody();
        // 用这个请求体（输入流）构造个buffered reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
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
        String return_product_info = requestBodyBuilder.toString();
        Borrow product_to_return = new Borrow();
        // 格式为Received POST request data: {"cardID":1,"productId":1,"borrowTime":"2024.04.11 14:28"}
        // 解析 cardID
        int cardIDStartIndex = return_product_info.indexOf("cardID") + 8; // 获取 "cardID" 后的索引位置
        int cardIDEndIndex = return_product_info.indexOf(",", cardIDStartIndex); // 获取第一个逗号的位置
        int cardID = Integer.parseInt(return_product_info.substring(cardIDStartIndex, cardIDEndIndex));
        System.out.println("Received cardID: " + cardID);
        product_to_return.setCardId(cardID);

        // 解析 productId
        int productIDStartIndex = return_product_info.indexOf("productId") + 8; // 获取 "productId" 后的索引位置
        int productIDEndIndex = return_product_info.indexOf(",", productIDStartIndex); // 获取第一个逗号的位置
        int productId = Integer.parseInt(return_product_info.substring(productIDStartIndex, productIDEndIndex));
        System.out.println("Received productId: " + productId);
        product_to_return.setProductId(productId);

        // 解析 borrowTime
        int borrowTimeStartIndex = return_product_info.indexOf("borrowTime") + 12; // 获取 "borrowTime" 后的索引位置'
        int borrowTimeEndIndex = return_product_info.length()-1; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
        long borrowTime = Long.parseLong(return_product_info.substring(borrowTimeStartIndex, borrowTimeEndIndex));
        //将String转换为long类型:
        System.out.println("Received borrowTime: " + borrowTime);
        product_to_return.setBorrowTime(borrowTime);
        product_to_return.setReturnTime(0);
        //打印product_to_return:
        System.out.printf("cardID:%d,productId:%d,borrowTime:%s\n", product_to_return.getCardId(), product_to_return.getProductId(), borrowTime);
        //调用returnProduct函数
        ApiResult result = BorrowHandler.library.returnProduct(product_to_return);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        if(result.ok){
            System.out.println("Return product successfully");
            exchange.sendResponseHeaders(200, 0);
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Product returned successfully".getBytes());
            outputStream.close();
        }else{
            System.out.println("Return product failed");
            exchange.sendResponseHeaders(400, 0);
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Product return failed".getBytes());
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
