package handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import crawler.PriceCrwaler;
import database.LibraryManagementSystem;
import database.LibraryManagementSystemImpl;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceUpdateHandler implements HttpHandler {
    private Map<String, String> verificationCodes = new HashMap<>();
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
        if (requestMethod.equals("POST")) {
            // 处理POST
            handlePostRequest(exchange);
        } else {
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
                int idStartIndex = productInfo1.indexOf("id") + 4; // 获取 "title" 后的索引位置
                int idEndIndex = productInfo1.indexOf(",", idStartIndex); // 获取第一个逗号的位置
                int id = Integer.parseInt(productInfo1.substring(idStartIndex, idEndIndex));
                System.out.println(id);
                ids.add(id); // 存储id
                // 解析 price
                int priceStartIndex = productInfo1.indexOf("price") + 7; // 获取 "price" 后的索引位置
                int priceEndIndex = productInfo1.indexOf(",", priceStartIndex); // 获取第一个逗号的位置
                double price = Double.parseDouble(productInfo1.substring(priceStartIndex, priceEndIndex));
                System.out.println(price);
                prices.add(price); // 存储price
                // 解析 title
                int titleStartIndex = productInfo1.indexOf("title") + 8; // 获取 "title" 后的索引位置
                int titleEndIndex = productInfo1.indexOf(",", titleStartIndex); // 获取第一个逗号的位置
                String title = productInfo1.substring(titleStartIndex, titleEndIndex - 1);
                titles.add(title); // 存储title
                // 解析 source
                int stockStartIndex = productInfo1.indexOf("source") + 9; // 获取 "source" 后的索引位置
                int stockEndIndex = productInfo1.indexOf(",", stockStartIndex); // 获取最后一个字符前的索引位置（排除末尾的 `}`)
                String source = productInfo1.substring(stockStartIndex, stockEndIndex);
                if(source.endsWith("\"")) {
                    source = source.substring(0, source.length() - 1); // 去除首尾的双引号
                }
                System.out.println(source);
                sources.add(source); // 存储source

            } catch (Exception e) {
                System.out.println("error in parsing productInfo1");
                e.printStackTrace();
            }
        }
        //传入三个list，调用爬虫函数
        PriceCrwaler priceCrwaler = new PriceCrwaler();
        Map<Integer, Double> price_updates = priceCrwaler.craw(ids, prices,titles, sources);
        //设置邮箱内容
        String emailContent = "亲爱的 " + user_name + ",\n\n";
        emailContent += "下列商品已经降价啦~点击链接查看详情:\n";
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        String email = RegisterHandler.library.searchEmail(user_name).message.toString();
        System.out.println("email: " + email);
        //如果price_updates中的price比原来的price低，发送邮件
        for (int i = 0; i < ids.size(); i++) {
            int productId = ids.get(i);
            double originalPrice = prices.get(i);  // 获取原始价格
            double newPrice = price_updates.getOrDefault(productId, originalPrice);  // 获取新的价格

            // 如果新的价格比原始价格低，则发送邮件
            if (newPrice < originalPrice) {
                emailContent += String.format(
                        "商品ID: %d\n原价格: ￥%.2f\n现价格: ￥%.2f\n商品链接: %s",
                        productId, originalPrice, newPrice, sources.get(i)
                );
            }
        }
        boolean mailSent = false;
        if(ids.size()>0){
            mailSent = sendMail(email, emailContent, "商品价格更新通知");
            System.out.println("emailContent: " + emailContent);
        }
        if (mailSent | ids.size() == 0) {
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
    private static final String USER = "2382825693@qq.com"; // 发件人称号，同邮箱地址※
    private static final String PASSWORD = "vnqjkjevngeceafb"; // 授权码，开启SMTP时显示※
    /**
     *
     * @param to 收件人邮箱
     * @param text 邮件正文
     * @param title 标题
     */
    /* 发送验证信息的邮件 */
    public boolean sendMail(String to, String text, String title){
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
}