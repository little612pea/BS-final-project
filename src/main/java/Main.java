import com.mysql.cj.log.Log;
import entities.Product;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;


import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // 这里是8000，建议不要80端口，容易和其他的撞
        try {
            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            DatabaseConnector connector = new DatabaseConnector(conf);
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }
            /* do somethings */
            CardHandler cardHandler = new CardHandler();
            BorrowHandler borrowHandler = new BorrowHandler();
            ProductHandler productHandler = new ProductHandler();
            LoginHandler loginHandler = new LoginHandler();
            RegisterHandler registerHandler = new RegisterHandler();
            SearchHandeler searchHandeler = new SearchHandeler();
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/home/card", cardHandler);
            server.createContext("/home/borrow", borrowHandler);
            server.createContext("/home/product", productHandler);
            server.createContext("/login", loginHandler);
            server.createContext("/register", registerHandler);
            server.createContext("/search", searchHandeler);
            server.setExecutor(null);
            server.start();
            log.info("Server is listening on port 8000.");
            // block the main thread
            //int read = System.in.read();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line = reader.readLine(); // 读取一行文本
            if (line != null) {
                System.out.println("Read line: " + line);
            }
//            server.stop(0);
            // release database connection handler
//            if (connector.release()) {
//                log.info("Success to release connection.");
//            } else {
//                log.warning("Failed to release connection.");
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static class LoginHandler implements HttpHandler {
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
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
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
    }

    static class RegisterHandler implements HttpHandler {
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
            // 解析 username
            int usernameStartIndex = register_Info.indexOf("name") + 7; // 获取 "username" 后的索引位置
            int usernameEndIndex = register_Info.indexOf(",", usernameStartIndex)-1; // 获取第一个逗号的位置
            String username = register_Info.substring(usernameStartIndex, usernameEndIndex);
            // 解析 password
            int passwordStartIndex = register_Info.indexOf("password") + 11; // 获取 "password" 后的索引位置
            int passwordEndIndex = register_Info.indexOf(",", passwordStartIndex)-1; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
            String password = register_Info.substring(passwordStartIndex, passwordEndIndex);
            // 解析 email
            int emailStartIndex = register_Info.indexOf("email") + 8;
            int emailEndIndex = register_Info.length() - 3;
            String email = register_Info.substring(emailStartIndex, emailEndIndex);
            //调用register函数
            System.out.printf("username:%s,password:%s\n", username
                    , password, email);

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
    }

    static class CardHandler implements HttpHandler {
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

    static class BorrowHandler implements HttpHandler {
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

    static class ProductHandler implements HttpHandler {
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
            if(query==null){
                ApiResult result = ProductHandler.library.queryProduct(new ProductQueryConditions());
                ProductQueryResults resProductList = (ProductQueryResults) result.payload;
                System.out.println(resProductList.getCount());
                for (int i = 0; i < resProductList.getCount(); i++) {
                    Product o2 = resProductList.getResults().get(i);
                    response += "{\"id\": " + o2.getProductId()+ ", \"comment\": \"" +o2.getComment()+ "\", \"title\": \""+o2.getTitle()+ "\", \"shop\": \""+o2.getShop()+ "\", \"deal\": \""+o2.getDeal()+ "\", \"img_url\": \""+o2.getImg()+ "\", \"price\": "+o2.getPrice()+ ", \"source\": \""+o2.getSource()+"\"}";
                    if (i != resProductList.getCount() - 1)
                        response += ",";
                    //{\"id\": " + o2.getProductId() + ", \"name\": \"" + o2.getName() + "\", \"department\": \"" + o2.getDepar response.append("{\"id\": ").append(o2.getProductId()).append(", \"name\": \"").append(o2.getName()).append("\", \"department\": \"").append(o2.getDepartment()).append("\", \"type\": \"").append(o2.getType()).append("\"}");
                }
                response += "]";
            }
            else if(query.contains("title") && query.contains("comment")){
                System.out.println("multi condition query:");
                // 格式为：category=shelbyHU&title=shelbyHU&shop=shelbyHU&minPublishYear=1145&maxPublishYear=14&img_url=shelbyHU&minPrice=1&maxPrice=919
                ProductQueryConditions productQueryConditions = new ProductQueryConditions();
                //将query中可能的中文编码转化为utf-8格式:
                query = URLDecoder.decode(query, "UTF-8");
                // 输出解码后的字符串
                System.out.println(query); // 输出： 原码
                // 解析 comment
                int categoryStartIndex = query.indexOf("comment") + 9; // 获取 "comment" 后的索引位置
                int categoryEndIndex = query.indexOf("&", categoryStartIndex); // 获取第一个逗号的位置
                String comment;
                if(categoryStartIndex!=categoryEndIndex){
                    comment = query.substring(categoryStartIndex, categoryEndIndex);
                    System.out.println(comment);
                    productQueryConditions.setComment(comment);
                }
                // 解析 title
                int titleStartIndex = query.indexOf("title") + 6; // 获取 "title" 后的索引位置
                int titleEndIndex = query.indexOf("&", titleStartIndex); // 获取第一个逗号的位置
                String title;
                if(titleStartIndex!=titleEndIndex){
                    title = query.substring(titleStartIndex, titleEndIndex);
                    System.out.println(title);
                    productQueryConditions.setTitle(title);
                }
                // 解析 shop
                int pressStartIndex = query.indexOf("shop") + 6; // 获取 "shop" 后的索引位置
                int pressEndIndex = query.indexOf("&", pressStartIndex); // 获取第一个逗号的位置
                String shop;
                if(pressStartIndex!=pressEndIndex){
                    shop = query.substring(pressStartIndex, pressEndIndex);
                    System.out.println(shop);
                    productQueryConditions.setShop(shop);
                }
                // 解析 minPublishYear
                int minPublishYearStartIndex = query.indexOf("minPublishYear") + 15; // 获取 "minPublishYear" 后的索引位置
                int minPublishYearEndIndex = query.indexOf("&", minPublishYearStartIndex); // 获取第一个逗号的位置
                int minPublishYear;
                if(minPublishYearStartIndex!=minPublishYearEndIndex){
                    minPublishYear = Integer.parseInt(query.substring(minPublishYearStartIndex, minPublishYearEndIndex));
                    System.out.println(minPublishYear);
                    productQueryConditions.setMinPublishYear(minPublishYear);
                }
                // 解析 maxPublishYear
                int maxPublishYearStartIndex = query.indexOf("maxPublishYear") + 15; // 获取 "maxPublishYear" 后的索引位置
                int maxPublishYearEndIndex = query.indexOf("&", maxPublishYearStartIndex); // 获取第一个逗号的位置
                int maxPublishYear;
                if(maxPublishYearStartIndex!=maxPublishYearEndIndex){
                    maxPublishYear = Integer.parseInt(query.substring(maxPublishYearStartIndex, maxPublishYearEndIndex));
                    System.out.println(maxPublishYear);
                    productQueryConditions.setMaxPublishYear(maxPublishYear);
                }
                // 解析 img_url
                int authorStartIndex = query.indexOf("img_url") + 7; // 获取 "img_url" 后的索引位置
                int authorEndIndex = query.indexOf("&", authorStartIndex); // 获取第一个逗号的位置
                String img_url;
                if(authorStartIndex!=authorEndIndex){
                    img_url = query.substring(authorStartIndex, authorEndIndex);
                    System.out.println(img_url);
                    productQueryConditions.setImg(img_url);
                }
                // 解析 minPrice
                int minPriceStartIndex = query.indexOf("minPrice") + 9; // 获取 "minPrice" 后的索引位置
                int minPriceEndIndex = query.indexOf("&", minPriceStartIndex); // 获取第一个逗号的位置
                double minPrice;
                if(minPriceStartIndex!=minPriceEndIndex){
                    minPrice = Integer.parseInt(query.substring(minPriceStartIndex, minPriceEndIndex));
                    System.out.println(minPrice);
                    productQueryConditions.setMinPrice(minPrice);
                }
                // 解析 maxPrice
                int maxPriceStartIndex = query.indexOf("maxPrice") + 9; // 获取 "maxPrice" 后的索引位置
                int maxPriceEndIndex = query.length(); // 获取最后一个字符前的索引位置（排除末尾的 `}`)
                double maxPrice;
                if(maxPriceStartIndex!=maxPriceEndIndex){
                    maxPrice = Integer.parseInt(query.substring(maxPriceStartIndex, maxPriceEndIndex));
                    System.out.println(maxPrice);
                    productQueryConditions.setMaxPrice(maxPrice);
                }
                //打印productQueryConditions:
                System.out.printf("comment:%s,title:%s,shop:%s,minPublishYear:%d,maxPublishYear:%d,img_url:%s,minPrice:%f,maxPrice:%f\n", productQueryConditions.getComment(), productQueryConditions.getTitle(), productQueryConditions.getShop(), productQueryConditions.getMinPublishYear(), productQueryConditions.getMaxPublishYear(), productQueryConditions.getImg(), productQueryConditions.getMinPrice(), productQueryConditions.getMaxPrice());
                ApiResult result = ProductHandler.library.queryProduct(productQueryConditions);
                ProductQueryResults resProductList = (ProductQueryResults) result.payload;
                System.out.println(resProductList.getCount());
                for (int i = 0; i < resProductList.getCount(); i++) {
                    Product o2 = resProductList.getResults().get(i);
                    response += "{\"id\": " + o2.getProductId()+ ", \"comment\": \"" +o2.getComment()+ "\", \"title\": \""+o2.getTitle()+ "\", \"shop\": \""+o2.getShop()+ "\", \"deal\": \""+o2.getDeal()+ "\", \"img_url\": \""+o2.getImg()+ "\", \"price\": "+o2.getPrice()+ ", \"source\": "+o2.getSource()+"}";
                    if (i != resProductList.getCount() - 1)
                        response += ",";
                }
                response += "]";

            }
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
            //如果product_Info有id字段，调用updateProduct函数，否则调用createProduct函数：
            // 解析 product_Info：格式为 text/plain:[{"comment":"cate","title":"productname","shop":"pub","deal":"2024","img_url":"img_url","price":"123","source":"321"},{"comment":"cate","title":"productname","shop":"pub","deal":"2024","img_url":"img_url","price":"123","source":"321"}]
            int productInfoStartIndex = product_Info.indexOf("[{") + 1; // 获取第一个 `[` 的索引位置
            int productInfoEndIndex = product_Info.indexOf("]"); // 获取最后一个 `]` 的索引位置
            String productInfo = product_Info.substring(productInfoStartIndex, productInfoEndIndex);
            String[] productInfos = productInfo.split("},");
            //计算productInfos的长度，一共有多少本书：
            int product_num = productInfos.length;
            int product_count = 0;
            List<Product> new_products = new ArrayList<>();
            for (String productInfo1 : productInfos) {
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
                int stockEndIndex = productInfo1.length() - 1; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
                if(product_count==product_num-1){
                    stockEndIndex = productInfo1.length() - 2;
                }
                String source = productInfo1.substring(stockStartIndex, stockEndIndex);
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
                new_products.add(product_to_create);
                product_count++;
            }
            ApiResult result = ProductHandler.library.storeProduct(new_products);
            System.out.println("store multi product result: " + result.toString());
            if (result.ok) {
                exchange.sendResponseHeaders(200, 0);
                System.out.println("Store all products successfully");
            } else {
                exchange.sendResponseHeaders(400, 0);
                System.out.println("Store product failed");
            }

//            else if(product_Info.contains("id") && product_Info.contains("comment")&& product_Info.contains("inc")){
//
//                //调用modifyProductInfo函数
//                System.out.println("1");
//                Product product_to_modify = new Product();
//                int idStartIndex = product_Info.indexOf("id") + 4; // 获取冒号后的索引位置
//                int idEndIndex = product_Info.indexOf(",", idStartIndex); // 获取第一个逗号的位置
//                int id = Integer.parseInt(product_Info.substring(idStartIndex, idEndIndex));
//                System.out.println(id);
//                product_to_modify.setProductId(id);
//                // 解析 comment
//                int categoryStartIndex = product_Info.indexOf("comment") + 11; // 获取 "comment" 后的索引位置
//                int categoryEndIndex = product_Info.indexOf(",", categoryStartIndex); // 获取第一个逗号的位置
//                String comment = product_Info.substring(categoryStartIndex, categoryEndIndex-1);
//                System.out.println(comment);
//                product_to_modify.setComment(comment);
//                // 解析 title
//                int titleStartIndex = product_Info.indexOf("title") + 8; // 获取 "title" 后的索引位置
//                int titleEndIndex = product_Info.indexOf(",", titleStartIndex); // 获取第一个逗号的位置
//                String title = product_Info.substring(titleStartIndex, titleEndIndex-1);
//                System.out.println(title);
//                product_to_modify.setTitle(title);
//                // 解析 shop
//                int pressStartIndex = product_Info.indexOf("shop") + 8; // 获取 "shop" 后的索引位置
//                int pressEndIndex = product_Info.indexOf(",", pressStartIndex-1); // 获取第一个逗号的位置
//                String shop = product_Info.substring(pressStartIndex, pressEndIndex-1);
//                System.out.println(shop);
//                product_to_modify.setShop(shop);
//
//                // 解析 deal
//                int publishYearStartIndex = product_Info.indexOf("deal") + 13; // 获取 "deal" 后的索引位置
//                int publishYearEndIndex = product_Info.indexOf(",", publishYearStartIndex); // 获取第一个逗号的位置
//                String publishYearStr = product_Info.substring(publishYearStartIndex, publishYearEndIndex);
//                System.out.println(publishYearStr);
//                // 去除前后的引号：
//                if(publishYearStr.charAt(0)=='"'){
//                    publishYearStr = publishYearStr.substring(1, publishYearStr.length()-1);
//                }
//                // 转换为int类型：
//                String deal = publishYearStr;
//                System.out.println(deal);
//                product_to_modify.setDeal(deal);
//
//                // 解析 img_url
//                int authorStartIndex = product_Info.indexOf("img_url") + 9; // 获取 "img_url" 后的索引位置
//                int authorEndIndex = product_Info.indexOf(",", authorStartIndex); // 获取第一个逗号的位置
//                String img_url = product_Info.substring(authorStartIndex, authorEndIndex-1);
//                System.out.println(img_url);
//                product_to_modify.setImg(img_url);
//
//                // 解析 price
//                int priceStartIndex = product_Info.indexOf("price") + 7; // 获取 "price" 后的索引位置
//                int priceEndIndex = product_Info.indexOf(",", priceStartIndex); // 获取第一个逗号的位置
//                String priceStr = product_Info.substring(priceStartIndex, priceEndIndex);
//                System.out.println(priceStr);
//                // 去除前后的引号：
//                if(priceStr.charAt(0)=='"'){
//                    priceStr = priceStr.substring(1, priceStr.length()-1);
//                }
//                // 转换为int类型：
//                double price = Double.parseDouble(priceStr);
//                System.out.println(price);
//                product_to_modify.setPrice(price);
//
//                // 解析 source
//                int stockStartIndex = product_Info.indexOf("source") + 7; // 获取 "source" 后的索引位置
//                int stockEndIndex = product_Info.indexOf(",", stockStartIndex); // 获取最后一个字符前的索引位置（排除末尾的 `}`）
//                String stockStr = product_Info.substring(stockStartIndex, stockEndIndex);
//                System.out.println(stockStr);
//                // 去除前后的引号：
//                if(stockStr.charAt(0)=='"'){
//                    stockStr = stockStr.substring(1, stockStr.length()-1);
//                }
//                // 转换为int类型：
//                int source = Integer.parseInt(stockStr);
//                System.out.println(source);
////                product_to_modify.setSource(source);
//
//                //解析stock_inc
//                int stock_incStartIndex = product_Info.indexOf("inc") + 5; // 获取 "stock_inc" 后的索引位置
//                int stock_incEndIndex = product_Info.length() - 1; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
//                String stock_incStr = product_Info.substring(stock_incStartIndex, stock_incEndIndex);
//                System.out.println("here1"+stock_incStr);
//                // 去除前后的引号：
//                if(stock_incStr.charAt(0)=='"'){
//                    stock_incStr = stock_incStr.substring(1, stock_incStr.length()-1);
//                }
//                // 转换为int类型：
//                int stock_inc = Integer.parseInt(stock_incStr);
//                System.out.println("here2 "+stock_inc);
//                ApiResult inc_result =  ProductHandler.library.incProductStock(product_to_modify.getProductId(), stock_inc);
//                if(inc_result.ok){
//                    System.out.println("Increase source successfully");
//                    System.out.printf("id:%d,comment:%s,title:%s,shop:%s,deal:%d,img_url:%s,price:%f,source:%d\n", product_to_modify.getProductId(), product_to_modify.getComment(), product_to_modify.getTitle(), product_to_modify.getShop(), product_to_modify.getDeal(), product_to_modify.getImg(), product_to_modify.getPrice(), product_to_modify.getSource());
//                    ApiResult result = ProductHandler.library.modifyProductInfo(product_to_modify);
//                    if(result.ok){
//                        System.out.println("Update product successfully");
//                        exchange.sendResponseHeaders(200, 0);
//                    }
//                    else{
//                        System.out.println("Update product failed");
//                        exchange.sendResponseHeaders(400, 0);
//                    }
//                }
//                else{
//                    System.out.println("Increase source failed");
//                    exchange.sendResponseHeaders(400, 0);
//                }
//
//            }
//            else if (product_Info.contains("id")&&product_Info.contains("card_id")) {
//                System.out.println("2 here");
//                int idStartIndex = product_Info.indexOf("id") + 4; // 获取冒号后的索引位置
//                int idEndIndex = product_Info.indexOf(",", idStartIndex); // 获取第一个逗号的位置
//                int id = Integer.parseInt(product_Info.substring(idStartIndex, idEndIndex));
//                int card_idStartIndex = product_Info.indexOf("card_id") + 10; // 获取 "card_id" 后的索引位置
//                int card_idEndIndex = product_Info.length() - 1; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
//                int card_id = Integer.parseInt(product_Info.substring(card_idStartIndex, card_idEndIndex-1));
//                System.out.printf("id:%d,card_id:%d\n", id, card_id);
//                Borrow product_borrow = new Borrow();
//                product_borrow.setProductId(id);
//                product_borrow.setCardId(card_id);
//                product_borrow.setBorrowTime(new Date().getTime());
//                product_borrow.setReturnTime(0);
//                ApiResult result = ProductHandler.library.borrowProduct(product_borrow);
//                if(result.ok){
//                    System.out.println("Borrow product successfully");
//                    exchange.sendResponseHeaders(200, 0);
//                }else{
//                    System.out.println("Borrow product failed");
//                    exchange.sendResponseHeaders(400, 0);
//                }
//
//            }
//            else if(product_Info.contains("id")){
//                //调用removeProduct函数:
//                // 解析 id
//                int idStartIndex = product_Info.indexOf(":") + 1; // 获取冒号后的索引位置
//                int idEndIndex = product_Info.indexOf("}", idStartIndex); // 获取第一个逗号的位置
//                int id = Integer.parseInt(product_Info.substring(idStartIndex, idEndIndex));
//                //调用removeProduct函数
//                System.out.printf("id:%d\n", id);
//                ApiResult result = ProductHandler.library.removeProduct(id);
//                System.out.println("Remove product result: " + result.toString());
//                if (result.ok) {
//                    exchange.sendResponseHeaders(200, 0);
//                    System.out.println("Remove product successfully");
//                } else {
//                    exchange.sendResponseHeaders(400, 0);
//                    System.out.println("Remove product failed");
//                }
//            }
//            else {
//                Product product_to_create = new Product();
//                product_to_create.setProductId(0);
//                //格式为：{"comment":"cate","title":"productname","shop":"pub","deal":"2024","img_url":"img_url","price":"123","source":"321"}
//                // 解析 comment
//                int categoryStartIndex = product_Info.indexOf("comment") + 11; // 获取 "comment" 后的索引位置
//                int categoryEndIndex = product_Info.indexOf(",", categoryStartIndex); // 获取第一个逗号的位置
//                String comment = product_Info.substring(categoryStartIndex, categoryEndIndex-1);
//                System.out.println(comment);
//                product_to_create.setComment(comment);
//                // 解析 title
//                int titleStartIndex = product_Info.indexOf("title") + 8; // 获取 "title" 后的索引位置
//                int titleEndIndex = product_Info.indexOf(",", titleStartIndex); // 获取第一个逗号的位置
//                String title = product_Info.substring(titleStartIndex, titleEndIndex-1);
//                System.out.println(title);
//                product_to_create.setTitle(title);
//                // 解析 shop
//                int pressStartIndex = product_Info.indexOf("shop") + 8; // 获取 "shop" 后的索引位置
//                int pressEndIndex = product_Info.indexOf(",", pressStartIndex-1); // 获取第一个逗号的位置
//                String shop = product_Info.substring(pressStartIndex, pressEndIndex-1);
//                System.out.println(shop);
//                product_to_create.setShop(shop);
//                // 解析 deal
//                int publishYearStartIndex = product_Info.indexOf("deal") + 14; // 获取 "deal" 后的索引位置
//                int publishYearEndIndex = product_Info.indexOf(",", publishYearStartIndex); // 获取第一个逗号的位置
//                String deal = product_Info.substring(publishYearStartIndex, publishYearEndIndex-1);
//                System.out.println(deal);
//                product_to_create.setDeal(deal);
//                // 解析 img_url
//                int authorStartIndex = product_Info.indexOf("img_url") + 9; // 获取 "img_url" 后的索引位置
//                int authorEndIndex = product_Info.indexOf(",", authorStartIndex); // 获取第一个逗号的位置
//                String img_url = product_Info.substring(authorStartIndex, authorEndIndex-1);
//                System.out.println(img_url);
//                product_to_create.setImg(img_url);
//                // 解析 price
//                int priceStartIndex = product_Info.indexOf("price") + 8; // 获取 "price" 后的索引位置
//                int priceEndIndex = product_Info.indexOf(",", priceStartIndex); // 获取第一个逗号的位置
//                double price = Double.parseDouble(product_Info.substring(priceStartIndex, priceEndIndex-1));
//                System.out.println(price);
//                product_to_create.setPrice(price);
//                // 解析 source
//                int stockStartIndex = product_Info.indexOf("source") + 8; // 获取 "source" 后的索引位置
//                int stockEndIndex = product_Info.length() - 1; // 获取最后一个字符前的索引位置（排除末尾的 `}`）
//                int source = Integer.parseInt(product_Info.substring(stockStartIndex, stockEndIndex-1));
//                System.out.println(source);
////                product_to_create.setSource(source);
//
//
//                //调用createProduct函数
//                System.out.printf("comment:%s,title:%s,shop:%s,deal:%d,img_url:%s,price:%f,source:%d\n", comment, title, shop, deal, img_url, price, source);
//                ApiResult result = ProductHandler.library.storeProduct(product_to_create);
//                System.out.println("Create product result: " + result.toString());
//                exchange.sendResponseHeaders(200, 0);
//            }


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

    static class SearchHandeler implements HttpHandler{
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
            //System.out.println("GET request received");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            URI requestedUri = exchange.getRequestURI();
            // 解析查询字符串
            String query = requestedUri.getRawQuery();

            String[] parts = query.split("=", 2);
            String search = parts[1];
            if (search!=null) {
                String jd = getSearchResult("\"C:\\\\Users\\\\23828\\\\anaconda3\\\\python.exe\"","D:\\home\\BS\\BS-final-project\\src\\crawler\\jd\\crawler_jd.py",search);
                String tb = getSearchResult("\"C:\\\\Users\\\\23828\\\\anaconda3\\\\python.exe\"","D:\\home\\BS\\BS-final-project\\src\\crawler\\tb\\crawler_tb.py",search);
                System.out.println(tb);
                System.out.println(jd);
            // 定义正则表达式模式
                String combinedJson = tb + jd;
                String result = combinedJson.replace("}][{", "},{");
                System.out.println(result);
                outputStream.write(result.getBytes());
                // 流一定要close！！！小心泄漏
                outputStream.close();
            }
            else{
                System.out.println("No search content");
                //返回数据库的内容：
                ApiResult result = SearchHandeler.library.queryProduct(new ProductQueryConditions());
            }
        }
        static String getSearchResult(String python_path, String exe_path, String search) {
            String jsonOutput = null;
            Process proc = null;
            try {
                String decodedSearch = URLDecoder.decode(search, "UTF-8");
                System.out.println("Query to search: " + decodedSearch);

                // 设置执行Python脚本的命令
                String[] args1 = new String[]{python_path, exe_path, decodedSearch};

                // 执行Python脚本
                proc = Runtime.getRuntime().exec(args1);

                // 读取标准输出
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream(), "gb2312"));
                // 读取错误输出
                BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream(), "gb2312"));

                // 使用StringBuilder来存储标准输出结果
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = stdInput.readLine()) != null) {
                    output.append(line);
                }
                stdInput.close();

                // 如果有错误信息，也将其输出，方便调试
                StringBuilder errorOutput = new StringBuilder();
                while ((line = stdError.readLine()) != null) {
                    errorOutput.append(line);
                }
                stdError.close();

//                if (errorOutput.length() > 0) {
//                    System.out.println("Python Error Output: " + errorOutput.toString());
//                }

                // 获取标准输出内容作为JSON结果
                jsonOutput = output.toString();

                // 确保Python进程已经完成
                proc.waitFor();

            } catch (Exception e) {
                System.out.println("Exception: " + e);
            } finally {
                if (proc != null) {
                    proc.destroy(); // 确保进程被关闭
                }
            }
            return jsonOutput;
        }
        private static void handlePostRequest(HttpExchange exchange) throws IOException {
            Process proc = null;
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
            String product_url = requestBodyBuilder.toString();
            int titleStartIndex = product_url.indexOf("url") + 6; // 获取 "title" 后的索引位置
            int titleEndIndex = product_url.indexOf("}", titleStartIndex); // 获取第一个逗号的位置
            String title = product_url.substring(titleStartIndex, titleEndIndex - 1);
            System.out.println(title);
            try {
                String[] args1 = new String[]{"\"C:\\\\Users\\\\23828\\\\anaconda3\\\\python.exe\"", "D:\\home\\BS\\BS-final-project\\src\\crawler\\history\\craw_history.py", title};
                System.out.println("args1: " + args1);
                proc = Runtime.getRuntime().exec(args1);
                proc.waitFor();

            } catch (Exception e) {
            System.out.println("Exception: " + e);
            } finally {
                if (proc != null) {
                    proc.destroy(); // 确保进程被关闭
                }
             }
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Product history get successfully".getBytes());
            outputStream.close();
        }
    }
}
