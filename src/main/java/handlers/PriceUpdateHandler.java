package handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import crawler.PriceCrwaler;
import database.PriceCompSystem;
import database.PriceCompSystemImpl;
import entities.Product;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static utils.JsonUtils.extractValue;

public class PriceUpdateHandler implements HttpHandler {
    private Map<String, String> verificationCodes = new HashMap<>();
    private static DatabaseConnector connector = null;
    private static PriceCompSystem library = null;
    private static ConnectConfig connectConfig = null;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> currentTask = null;
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
        }else if (requestMethod.equals("GET")) {
            // 处理GET
            handleGetRequest(exchange);
        }
        else if (requestMethod.equals("OPTIONS")) {
            handleOptionsRequest(exchange);
        }

        else {
            // 其他请求返回405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private String getQueryParam(String query, String paramName) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
    private void handlePostRequest(HttpExchange exchange) throws IOException {
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
        String product_Info = requestBodyBuilder.toString();
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
        List<Integer> ids = new ArrayList<>();
        List<Double> prices = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        for (String productInfo1 : productInfos) {
            try {
                ids.add(extractValue(productInfo1, "id", Integer::parseInt));
                prices.add(extractValue(productInfo1, "price", Double::parseDouble));
                titles.add(extractValue(productInfo1, "title", String::valueOf));
                sources.add(extractValue(productInfo1, "source", String::valueOf));
            } catch (Exception e) {
                System.out.println("error in parsing productInfo1");
                e.printStackTrace();
            }
        }
        //传入三个list，调用爬虫函数
        PriceCrwaler priceCrwaler = new PriceCrwaler();
        Map<Integer, Double> price_updates = priceCrwaler.craw(ids, prices,titles, sources,user_name);
        //设置邮箱内容
        String emailContent = "亲爱的 " + user_name + ",\n\n";
        emailContent += "下列商品已经降价啦~点击链接查看详情:\n";
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        String email = RegisterHandler.library.searchEmail(user_name).message.toString();
        System.out.println("email: " + email);
        //如果price_updates中的price比原来的price低，发送邮件
        boolean need_to_send = false;
        for (int i = 0; i < ids.size(); i++) {
            int productId = ids.get(i);
            double originalPrice = prices.get(i);  // 获取原始价格
            double newPrice = price_updates.getOrDefault(productId, originalPrice);  // 获取新的价格

            // 如果新的价格比原始价格低，则发送邮件
            if (newPrice < originalPrice && newPrice != -1.0) {
                emailContent += String.format(
                        "商品ID: %d\n原价格: ￥%.2f\n现价格: ￥%.2f\n商品链接: %s",
                        productId, originalPrice, newPrice, sources.get(i)
                );
                need_to_send = true;
            }
        }
        boolean mailSent = false;
        if(ids.size()>0 && need_to_send){
            mailSent = sendMail(email, emailContent, "商品价格更新通知");
            System.out.println("emailContent: " + emailContent);
        }
        if (mailSent | ids.size() == 0 | !need_to_send) {
            System.out.println("updated successfully");

            // Convert price_updates map to JSON
            Gson gson = new Gson();
            String jsonResponse = gson.toJson(price_updates);

            // Set the response header for JSON content type
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            // Send response with status 200
            exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);

            // Write JSON response body
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(jsonResponse.getBytes());
            outputStream.close();
        } else {
            System.out.println("updated failed!");
            exchange.sendResponseHeaders(400, 0);
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("updated failed".getBytes());
            outputStream.close();
        }
    }
    private static final String USER = "238*****693@qq.com"; // 发件人称号，同邮箱地址※
    private static final String PASSWORD = "vn*************afb"; // 授权码，开启SMTP时显示※
    /**
     *
     * @param to 收件人邮箱
     * @param text 邮件正文
     * @param title 标题
     */
    /* 发送验证信息的邮件 */
    public static boolean sendMail(String to, String text, String title){
        try {
            final Properties props = new Properties();
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.auth", "true");
//            注意发送邮件的方法中，发送给谁的，发送给对应的app，※
//            要改成对应的app。扣扣的改成qq的，网易的要改成网易的。※
            props.put("mail.smtp.host", "smtp.qq.com");
//                props.put("mail.smtp.host", "smtp.163.com");

            // 发件人的账号
            props.put("mail.user", USER);
            //发件人的密码
            props.put("mail.password", PASSWORD);

            // 构建授权信息，用于进行SMTP进行身份验证
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    // 用户名、密码
                    String userName = props.getProperty("mail.user");
                    String password = props.getProperty("mail.password");
                    return new PasswordAuthentication(userName, password);
                }
            };
            // 使用环境属性和授权信息，创建邮件会话
            Session mailSession = Session.getInstance(props, authenticator);
            // 创建邮件消息
            MimeMessage message = new MimeMessage(mailSession);
            // 设置发件人
            String username = props.getProperty("mail.user");
            InternetAddress form = new InternetAddress(username);
            message.setFrom(form);

            // 设置收件人
            InternetAddress toAddress = new InternetAddress(to);
            message.setRecipient(Message.RecipientType.TO, toAddress);

            // 设置邮件标题
            message.setSubject(title);

            // 设置邮件的内容体
            message.setContent(text, "text/html;charset=UTF-8");
            // 发送邮件
            Transport.send(message);
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    //执行定时任务
    public static void startScheduledTask(int time_interval, String userName) {
        // 如果已有任务正在运行，先取消它
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false); // 取消当前任务（不打断正在执行的任务）
        }

        Runnable task = () -> {
            try {
                System.out.println("Executing scheduled task to check price updates...");
                handlePriceUpdateForUser(userName);
            } catch (Exception e) {
                System.err.println("Error occurred while executing scheduled task: " + e.getMessage());
                e.printStackTrace();
            }
        };

        // 启动新的定时任务，并保存返回的 `ScheduledFuture`
        currentTask = scheduler.scheduleAtFixedRate(task, 0, time_interval, TimeUnit.SECONDS);
    }
    private static void handlePriceUpdateForUser(String userName) {
        try {
            // 获取用户收藏的商品信息
            List<Integer> ids = new ArrayList<>();
            List<Double> prices = new ArrayList<>();
            List<String> sources = new ArrayList<>();
            List<String> titles = new ArrayList<>();

            // 从数据库或其他服务中加载用户的收藏数据
            List<Product> favoriteProducts = (List<Product>) RegisterHandler.library.getUserFavoriteProducts(userName).payload;
            for (Product product : favoriteProducts) {
                ids.add(product.getProductId());
                prices.add(product.getPrice());
                titles.add(product.getTitle());
                sources.add(product.getSource());
            }

            // 调用爬虫函数获取价格更新
            PriceCrwaler priceCrwaler = new PriceCrwaler();
            Map<Integer, Double> priceUpdates = priceCrwaler.craw(ids, prices, titles, sources, userName);

            // 准备邮件内容
            StringBuilder emailContent = new StringBuilder();
            emailContent.append("亲爱的 ").append(userName).append(",\n\n");
            emailContent.append("下列商品已经降价啦~点击链接查看详情:\n");

            // 获取用户邮箱
            String email = RegisterHandler.library.searchEmail(userName).message.toString();

            for (int i = 0; i < ids.size(); i++) {
                int productId = ids.get(i);
                double originalPrice = prices.get(i);
                double newPrice = priceUpdates.getOrDefault(productId, originalPrice);

                if (newPrice < originalPrice) {
                    emailContent.append(String.format(
                            "商品ID: %d\n原价格: ￥%.2f\n现价格: ￥%.2f\n商品链接: %s\n\n",
                            productId, originalPrice, newPrice, sources.get(i)
                    ));
                }
            }

            // 发送邮件
            if (!ids.isEmpty()) {
                boolean mailSent = sendMail(email, emailContent.toString(), "商品价格更新通知");
                if (mailSent) {
                    System.out.println("Price update email sent to: " + email);
                } else {
                    System.err.println("Failed to send email to: " + email);
                }
            }
        } catch (Exception e) {
            System.err.println("Error while handling price update for user " + userName + ": " + e.getMessage());
            e.printStackTrace();
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
        String response = "ok";
        //parse user_name
        int nameStartIndex = query.indexOf("user_name=") + 10; // 获取 "user_name=" 后的索引位置
        int nameEndIndex = query.indexOf("&", nameStartIndex); // 获取第一个逗号的位置
        String user_name = query.substring(nameStartIndex, nameEndIndex); // 提取 user_name 的值
        int intervalStartIndex = query.indexOf("interval=") + 9; // 获取 "interval=" 后的索引位置
        int intervalEndIndex = query.length(); // 获取最后一个字符前的索引位置（排除末尾的 `}`)
        String interval = query.substring(intervalStartIndex, intervalEndIndex); // 提取 interval 的值
        System.out.println("user_name: " + user_name);
        System.out.println("interval: " + interval);
        startScheduledTask(Integer.parseInt(interval), user_name);
        try {
            outputStream.write(response.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
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