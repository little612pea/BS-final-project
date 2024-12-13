package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import database.LibraryManagementSystem;
import database.LibraryManagementSystemImpl;
import entities.Product;
import queries.ApiResult;
import queries.ProductQueryConditions;
import queries.ProductQueryResults;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductHandler implements HttpHandler {
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
                //{\"id\": " + o2.getProductId() + ", \"name\": \"" + o2.getName() + "\", \"department\": \"" + o2.getDepar response.append("{\"id\": ").append(o2.getProductId()).append(", \"name\": \"").append(o2.getName()).append("\", \"department\": \"").append(o2.getDepartment()).append("\", \"type\": \"").append(o2.getType()).append("\"}");
            }
            response += "]";
        // 写
        System.out.printf("Response: %s\n", response);
        try {
            outputStream.write(response.getBytes());
            // 流一定要close！！！小心泄漏
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
            // 解析 productId
            int IdStartIndex = productInfo1.indexOf("id") + 4; // 获取 "productId" 后的索引位置
            int IdEndIndex = productInfo1.indexOf(",", IdStartIndex); // 获取第一个逗号的位置
            int productId = Integer.parseInt(productInfo1.substring(IdStartIndex, IdEndIndex));
//                System.out.println(productId);
            product_to_create.setProductId(productId);
//                System.out.println(productId);

            int titleStartIndex = productInfo1.indexOf("title") + 8; // 获取 "title" 后的索引位置
            int titleEndIndex = productInfo1.indexOf(",", titleStartIndex); // 获取第一个逗号的位置
            String title = productInfo1.substring(titleStartIndex, titleEndIndex - 1);
            product_to_create.setTitle(title);
//                System.out.println(title);
            // 解析 shop
            int pressStartIndex = productInfo1.indexOf("shop") + 7; // 获取 "shop" 后的索引位置
            int pressEndIndex = productInfo1.indexOf(",", pressStartIndex - 1); // 获取第一个逗号的位置
            String shop = productInfo1.substring(pressStartIndex, pressEndIndex - 1);
            product_to_create.setShop(shop);
//                System.out.println(shop);
            // 解析 img_url
            int authorStartIndex = productInfo1.indexOf("img_url") + 10; // 获取 "img_url" 后的索引位置
            int authorEndIndex = productInfo1.indexOf(",", authorStartIndex); // 获取第一个逗号的位置
            String img_url = productInfo1.substring(authorStartIndex, authorEndIndex-1 );
            product_to_create.setImg(img_url);
//                System.out.println(img_url);
            // 解析 price
            int priceStartIndex = productInfo1.indexOf("price") + 7; // 获取 "price" 后的索引位置
            int priceEndIndex = productInfo1.indexOf(",", priceStartIndex); // 获取第一个逗号的位置
            double price = Double.parseDouble(productInfo1.substring(priceStartIndex, priceEndIndex));
            product_to_create.setPrice(price);
//                System.out.println(price);
            // 解析 source
            int stockStartIndex = productInfo1.indexOf("source") + 9; // 获取 "source" 后的索引位置
            int stockEndIndex = productInfo1.indexOf(",", stockStartIndex); // 获取最后一个字符前的索引位置（排除末尾的 `}`)
            String source = productInfo1.substring(stockStartIndex, stockEndIndex);
            product_to_create.setSource(source);
//                System.out.println(source);
            if(productInfo1.contains("comment")){
                // 解析 comment
                int categoryStartIndex = productInfo1.indexOf("comment") + 10; // 获取 "comment" 后的索引位置
                int categoryEndIndex = productInfo1.indexOf(",", categoryStartIndex); // 获取第一个逗号的位置
                String comment = productInfo1.substring(categoryStartIndex, categoryEndIndex - 1);
                product_to_create.setComment(comment);
//                    System.out.printf("comment:%s,title:%s,shop:%s,img_url:%s,price:%f,source:%s\n", comment, title, shop, img_url, price, source);
            }
            else if (productInfo1.contains("deal")){
                // 解析 deal
                int publishYearStartIndex = productInfo1.indexOf("deal") + 7; // 获取 "deal" 后的索引位置
                int publishYearEndIndex = productInfo1.indexOf(",", publishYearStartIndex); // 获取第一个逗号的位置
                String deal = productInfo1.substring(publishYearStartIndex, publishYearEndIndex - 1);
                product_to_create.setDeal(deal);
//                    System.out.printf("title:%s,shop:%s,deal:%s,img_url:%s,price:%f,source:%s\n", title, shop, deal, img_url, price, source);
            }
            int favStartIndex = productInfo1.indexOf("favorite") + 10; // 获取 "price" 后的索引位置
            int favEndIndex = productInfo1.length() - 3; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
            int favorite = Integer.parseInt(productInfo1.substring(favStartIndex, favEndIndex));
            System.out.println("parsed favorite result: "+favorite);
            product_to_create.setFavorite(favorite);
//                System.out.println(price);
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
                    //格式为：{"comment":"cate","title":"productname","shop":"pub","deal":"2024","img_url":"img_url","price":"123","source":"321"}
                    // 解析 title,如果包含comment/deal字段分别处理
                    int titleStartIndex = productInfo1.indexOf("title") + 8; // 获取 "title" 后的索引位置
                    int titleEndIndex = productInfo1.indexOf(",", titleStartIndex); // 获取第一个逗号的位置
                    String title = productInfo1.substring(titleStartIndex, titleEndIndex - 1);
                    product_to_create.setTitle(title);
                    System.out.println(title);
                    // 解析 shop
                    int pressStartIndex = productInfo1.indexOf("shop") + 7; // 获取 "shop" 后的索引位置
                    int pressEndIndex = productInfo1.indexOf(",", pressStartIndex - 1); // 获取第一个逗号的位置
                    String shop = productInfo1.substring(pressStartIndex, pressEndIndex - 1);
                    product_to_create.setShop(shop);
                    System.out.println(shop);
                    // 解析 img_url
                    int authorStartIndex = productInfo1.indexOf("img_url") + 10; // 获取 "img_url" 后的索引位置
                    int authorEndIndex = productInfo1.indexOf(",", authorStartIndex); // 获取第一个逗号的位置
                    String img_url = productInfo1.substring(authorStartIndex, authorEndIndex-1 );
                    product_to_create.setImg(img_url);
                    System.out.println(img_url);
                    // 解析 price
                    int priceStartIndex = productInfo1.indexOf("price") + 7; // 获取 "price" 后的索引位置
                    int priceEndIndex = productInfo1.indexOf(",", priceStartIndex); // 获取第一个逗号的位置
                    double price = Double.parseDouble(productInfo1.substring(priceStartIndex, priceEndIndex));
                    product_to_create.setPrice(price);
                    System.out.println(price);
                    // 解析 source
                    int stockStartIndex = productInfo1.indexOf("source") + 9; // 获取 "source" 后的索引位置
                    int stockEndIndex = productInfo1.indexOf(",", stockStartIndex); // 获取最后一个字符前的索引位置（排除末尾的 `}`)
                    String source = productInfo1.substring(stockStartIndex, stockEndIndex);
                    if(source.endsWith("\"")) {
                        source = source.substring(0, source.length() - 1); // 去除首尾的双引号
                    }
                    product_to_create.setSource(source);
                    System.out.println(source);
                    if(productInfo1.contains("comment")){
                        // 解析 comment
                        int categoryStartIndex = productInfo1.indexOf("comment") + 10; // 获取 "comment" 后的索引位置
                        int categoryEndIndex = productInfo1.indexOf(",", categoryStartIndex); // 获取第一个逗号的位置
                        String comment = productInfo1.substring(categoryStartIndex, categoryEndIndex - 1);
                        product_to_create.setComment(comment);
                        System.out.printf("comment:%s,title:%s,shop:%s,img_url:%s,price:%f,source:%s\n", comment, title, shop, img_url, price, source);
                    }
                    else if (productInfo1.contains("deal")){
                        // 解析 deal
                        int publishYearStartIndex = productInfo1.indexOf("deal") + 7; // 获取 "deal" 后的索引位置
                        int publishYearEndIndex = productInfo1.indexOf(",", publishYearStartIndex); // 获取第一个逗号的位置
                        String deal = productInfo1.substring(publishYearStartIndex, publishYearEndIndex - 1);
                        product_to_create.setDeal(deal);
                        System.out.printf("title:%s,shop:%s,deal:%s,img_url:%s,price:%f,source:%s\n", title, shop, deal, img_url, price, source);
                    }
                    int favStartIndex = productInfo1.indexOf("favorite") + 10; // 获取 "price" 后的索引位置
                    int favEndIndex = productInfo1.length() - 1; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
                    if(product_count==product_num-1){
                        favEndIndex = productInfo1.length() - 2;
                    }
                    String favoriteStr = productInfo1.substring(favStartIndex, favEndIndex);
                    int favorite = "true".equalsIgnoreCase(favoriteStr) ? 1 : 0;
                    product_to_create.setFavorite(favorite);
                    System.out.println(price);
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
        // 2. 插入数据库
        // 响应头
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        // 响应状态码200


        // 剩下三个和GET一样
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