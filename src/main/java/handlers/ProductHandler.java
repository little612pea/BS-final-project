package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import database.PriceCompSystem;
import database.PriceCompSystemImpl;
import entities.Product;
import queries.ApiResult;
import queries.ProductQueryConditions;
import queries.ProductQueryResults;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static utils.JsonUtils.extractValue;

public class ProductHandler implements HttpHandler {
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
        System.out.println("GET request received");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // 状态码为200，也就是status ok
        exchange.sendResponseHeaders(200, 0);
        // 获取输出流，java用流对象来进行io操作
        OutputStream outputStream = exchange.getResponseBody();
        // 构建JSON响应数据，这里简化为字符串
        URI requestedUri = exchange.getRequestURI();
        // 解析查询字符串
        String query = requestedUri.getRawQuery();
        System.out.println("query to fetch from db: " + query);
        String response = "[";
        //parse user_name
        int nameStartIndex = query.indexOf("user_name=") + 10; // 获取 "user_name=" 后的索引位置
        int nameEndIndex = query.length();
        String user_name = query.substring(nameStartIndex, nameEndIndex); // 提取 user_name 的值
        ApiResult result = ProductHandler.library.queryProduct(user_name,new ProductQueryConditions());
        ProductQueryResults resProductList = (ProductQueryResults) result.payload;
        System.out.println(resProductList.getCount());
        for (int i = 0; i < resProductList.getCount(); i++) {
            Product o2 = resProductList.getResults().get(i);
            response += "{\"id\": " + o2.getProductId()+ ", \"comment\": \"" +o2.getComment()+ "\", \"title\": \""+o2.getTitle()+ "\", \"shop\": \""+o2.getShop()+ "\", \"deal\": \""+o2.getDeal()+ "\", \"img_url\": \""+o2.getImg()+ "\", \"price\": "+o2.getPrice()+ ", \"source\": \""+o2.getSource()+"\", \"favorite\": "+o2.getFavorite()+"}";
            if (i != resProductList.getCount() - 1)
                response += ",";
        }
        response += "]";

        System.out.printf("Response: %s\n", response);
        try {
            outputStream.write(response.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
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
        System.out.println("Received POST request data here here~~~: " + requestBodyBuilder.toString());
        //调用数据库操作：
        // 1. 解析请求体，获取卡片信息

        String product_Info = requestBodyBuilder.toString();
        if( product_Info.contains("like")){
            Product product_to_create = new Product();
            String productInfo1 = product_Info;
            int nameStartIndex = product_Info.indexOf("user_name:") + 12; // 获取 "title" 后的索引位置
            int nameEndIndex = product_Info.indexOf(",", nameStartIndex); // 获取第一个逗号的位置
            String user_name = product_Info.substring(nameStartIndex, nameEndIndex);
            Pattern pattern = Pattern.compile("\"user_name\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(user_name);
            if (matcher.find()) {
                user_name = matcher.group(1);  // 获取匹配的用户名部分
                System.out.println("Extracted username: " + user_name);
            } else {
                System.out.println("No username found.");
            }

            product_to_create.setProductId(extractValue(productInfo1, "\"id", Integer::parseInt));
            product_to_create.setTitle(extractValue(productInfo1, "title", String::valueOf));
            product_to_create.setShop(extractValue(productInfo1, "shop", String::valueOf));
            product_to_create.setImg(extractValue(productInfo1, "img_url", String::valueOf));
            product_to_create.setPrice(extractValue(productInfo1, "price", Double::parseDouble));
            product_to_create.setSource(extractValue(productInfo1, "source", String::valueOf));
            if(productInfo1.contains("comment")){
                product_to_create.setComment(extractValue(productInfo1, "comment", String::valueOf));
            }
            else if (productInfo1.contains("deal")){
                product_to_create.setDeal(extractValue(productInfo1, "deal", String::valueOf));
            }
            product_to_create.setFavorite(extractValue(productInfo1, "favorite", Integer::parseInt));
            ApiResult result = ProductHandler.library.modifyLikeStatus(user_name,product_to_create);
            if (result.ok) {
                exchange.sendResponseHeaders(200, 0);
                System.out.println("Store all products successfully");
            } else {
                exchange.sendResponseHeaders(400, 0);
                System.out.println("Store product failed");
            }
        }
        else if (product_Info.contains("product")){
            //如果product_Info有id字段，调用updateProduct函数，否则调用createProduct函数：
            // 解析 product_Info：格式为 text/plain:[{"comment":"cate","title":"productname","shop":"pub","deal":"2024","img_url":"img_url","price":"123","source":"321"},{"comment":"cate","title":"productname","shop":"pub","deal":"2024","img_url":"img_url","price":"123","source":"321"}]
            int nameStartIndex = product_Info.indexOf("user_name:") + 12; // 获取 "title" 后的索引位置
            int nameEndIndex = product_Info.indexOf(",", nameStartIndex); // 获取第一个逗号的位置
            String user_name = product_Info.substring(nameStartIndex, nameEndIndex);
            Pattern pattern = Pattern.compile("\"user_name\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(user_name);
            if (matcher.find()) {
                user_name = matcher.group(1);  // 获取匹配的用户名部分
                System.out.println("Extracted username: " + user_name);
            } else {
                System.out.println("No username found.");
            }
            int productInfoStartIndex = product_Info.indexOf("[{") + 1; // 获取第一个 `[` 的索引位置
            int productInfoEndIndex = product_Info.indexOf("]"); // 获取最后一个 `]` 的索引位置
            String productInfo = product_Info.substring(productInfoStartIndex, productInfoEndIndex);
            String[] productInfos = productInfo.split("},");
            //计算productInfos的长度，一共有多少本书：
            int product_num = productInfos.length;
            int product_count = 0;
            List<Product> new_products = new ArrayList<>();
            for (String productInfo1 : productInfos) {
                try {
                    Product product_to_create = new Product();
                    product_to_create.setTitle(extractValue(productInfo1, "title", String::valueOf));
                    product_to_create.setShop(extractValue(productInfo1, "shop", String::valueOf));
                    product_to_create.setImg(extractValue(productInfo1, "img_url", String::valueOf));
                    product_to_create.setPrice(extractValue(productInfo1, "price", Double::parseDouble));
                    product_to_create.setSource(extractValue(productInfo1, "source", String::valueOf));
                    if(productInfo1.contains("comment")){
                        product_to_create.setComment(extractValue(productInfo1, "comment", String::valueOf));
                    }
                    else if (productInfo1.contains("deal")){
                        product_to_create.setDeal(extractValue(productInfo1, "deal", String::valueOf));
                    }
                    product_to_create.setFavorite(extractValue(productInfo1, "favorite", Integer::parseInt));
                    new_products.add(product_to_create);
                    product_count++;
                } catch (Exception e) {
                    System.out.println("error in parsing productInfo1");
                    e.printStackTrace();
                }
            }
            System.out.println("new_products: " + new_products);
            ApiResult result = ProductHandler.library.storeProduct(user_name,new_products);
            System.out.println("store multi product result: " + result.toString());
            if (result.ok) {
                exchange.sendResponseHeaders(200, 0);
                System.out.println("Store all products successfully");
            } else {
                exchange.sendResponseHeaders(400, 0);
                System.out.println("Store product failed");
            }
        }
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write("Product created successfully".getBytes());
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