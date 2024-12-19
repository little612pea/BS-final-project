package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import database.PriceCompSystem;
import database.PriceCompSystemImpl;
import queries.ApiResult;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import static utils.JsonUtils.extractValue;

public class RegisterHandler implements HttpHandler {
    private Map<String, String> verificationCodes = new HashMap<>();
    private static DatabaseConnector connector = null;
    public static PriceCompSystem library = null;
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
        }else if(requestMethod.equals("OPTIONS")){
            // options请求不做处理
            handleOptionsRequest(exchange);
        }
        else {
            // 其他请求返回405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        System.out.println("GET send veri_code request received");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // 状态码为200，也就是status ok
        exchange.sendResponseHeaders(200, 0);
        // 获取输出流，java用流对象来进行io操作
        OutputStream outputStream = exchange.getResponseBody();
        URI requestedUri = exchange.getRequestURI();
        // 解析查询字符串
        String query = requestedUri.getRawQuery();
        System.out.println("query: " + query);
        // 解析查询字符串,假设就只有一个参数：card_id，格式为cardID=xxx
        int emailStartIndex = query.indexOf("email") + 6;
        int emailEndIndex = query.length();
        String email = query.substring(emailStartIndex, emailEndIndex);
        email = URLDecoder.decode(email, StandardCharsets.UTF_8.toString());
        System.out.println("email: " + email);
        String verificationCode = generateVerificationCode(6); // Generate a 6-digit code
        boolean mailSent = sendMail(email, "Your verification code is: " + verificationCode, "Verification Code");

        if (mailSent) {
            verificationCodes.put(email, verificationCode); // Store the code associated with the email
            System.out.println("Verification code sent to: " + email);
        }
        String response = verificationCode;
        // 写
        System.out.printf("Response: %s\n", response);
        outputStream.write(response.getBytes());
        // 流一定要close！！！小心泄漏
        outputStream.close();
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
        String register_Info = requestBodyBuilder.toString();
        //解析请求体，获取卡片信息
        String username = extractValue(register_Info, "name", String::toString);
        String password = extractValue(register_Info, "password", String::toString);
        String email = extractValue(register_Info, "email", String::toString);
        System.out.printf("username:%s,password:%s\n", username, password, email);

        ApiResult result = RegisterHandler.library.register(username, password, email);
        System.out.println("Register result: " + result.toString());
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        if (result.ok) {
            System.out.println("Register successfully");
            exchange.sendResponseHeaders(200, 0);
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Register successfully".getBytes());
            outputStream.close();
        } else {
            System.out.println("Register failed,user exists!");
            exchange.sendResponseHeaders(400, 0);
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Register failed".getBytes());
            outputStream.close();

        }
    }
    private static final String USER = "2382825693@qq.com"; // 发件人称号，同邮箱地址※
    private static final String PASSWORD = "vnqjkjevngeceafb"; // 授权码，开启SMTP时显示※
    public String generateVerificationCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10)); // Generate digits
        }
        return code.toString();
    }
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