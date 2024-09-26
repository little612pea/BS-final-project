import com.mysql.cj.log.Log;
import entities.Book;
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
            BookHandler bookHandler = new BookHandler();
            LoginHandler loginHandler = new LoginHandler();
            RegisterHandler registerHandler = new RegisterHandler();
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/home/card", cardHandler);
            server.createContext("/home/borrow", borrowHandler);
            server.createContext("/home/book", bookHandler);
            server.createContext("/login", loginHandler);
            server.createContext("/register", registerHandler);
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
            server.stop(0);
            // release database connection handler
            if (connector.release()) {
                log.info("Success to release connection.");
            } else {
                log.warning("Failed to release connection.");
            }
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
                System.out.println("Successfully init class BookTest.");
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
                System.out.println("Successfully init class BookTest.");
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
                System.out.println("Successfully init class BookTest.");
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
        //System.out.println("GET request received");
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
        System.out.printf("Response: %s\n", response);
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
                System.out.println("Successfully init class BookTest.");
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
                String iso_borrowTime = new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm").format(new java.util.Date(o2.getBorrowTime()));
                String iso_returnTime = new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm").format(new java.util.Date(o2.getReturnTime()));
                if (o2.getReturnTime() == 0)
                    iso_returnTime = "book unreturned";
                response += "{\"cardID\": " + o2.getCardId() + ", \"bookID\": " + o2.getBookId() + ", \"borrowTime\": \"" + iso_borrowTime + "\", \"unix_borrowTime\": " + o2.getBorrowTime() + ", \"returnTime\": \"" + iso_returnTime + "\", \"unix_returnTime\": " + o2.getReturnTime() + "}";
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
            String return_book_info = requestBodyBuilder.toString();
            Borrow book_to_return = new Borrow();
            // 格式为Received POST request data: {"cardID":1,"bookID":1,"borrowTime":"2024.04.11 14:28"}
            // 解析 cardID
            int cardIDStartIndex = return_book_info.indexOf("cardID") + 8; // 获取 "cardID" 后的索引位置
            int cardIDEndIndex = return_book_info.indexOf(",", cardIDStartIndex); // 获取第一个逗号的位置
            int cardID = Integer.parseInt(return_book_info.substring(cardIDStartIndex, cardIDEndIndex));
            System.out.println("Received cardID: " + cardID);
            book_to_return.setCardId(cardID);

            // 解析 bookID
            int bookIDStartIndex = return_book_info.indexOf("bookID") + 8; // 获取 "bookID" 后的索引位置
            int bookIDEndIndex = return_book_info.indexOf(",", bookIDStartIndex); // 获取第一个逗号的位置
            int bookID = Integer.parseInt(return_book_info.substring(bookIDStartIndex, bookIDEndIndex));
            System.out.println("Received bookID: " + bookID);
            book_to_return.setBookId(bookID);

            // 解析 borrowTime
            int borrowTimeStartIndex = return_book_info.indexOf("borrowTime") + 12; // 获取 "borrowTime" 后的索引位置'
            int borrowTimeEndIndex = return_book_info.length()-1; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
            long borrowTime = Long.parseLong(return_book_info.substring(borrowTimeStartIndex, borrowTimeEndIndex));
            //将String转换为long类型:
            System.out.println("Received borrowTime: " + borrowTime);
            book_to_return.setBorrowTime(borrowTime);
            book_to_return.setReturnTime(0);
            //打印book_to_return:
            System.out.printf("cardID:%d,bookID:%d,borrowTime:%s\n", book_to_return.getCardId(), book_to_return.getBookId(), borrowTime);
            //调用returnBook函数
            ApiResult result = BorrowHandler.library.returnBook(book_to_return);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            if(result.ok){
                System.out.println("Return book successfully");
                exchange.sendResponseHeaders(200, 0);
                // 剩下三个和GET一样
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write("Book returned successfully".getBytes());
                outputStream.close();
            }else{
                System.out.println("Return book failed");
                exchange.sendResponseHeaders(400, 0);
                // 剩下三个和GET一样
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write("Book return failed".getBytes());
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

    static class BookHandler implements HttpHandler {
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
                System.out.println("Successfully init class BookTest.");
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
            System.out.println("query: " + query);
            String response = "[";
            if(query==null){
                ApiResult result = BookHandler.library.queryBook(new BookQueryConditions());
                BookQueryResults resBookList = (BookQueryResults) result.payload;
                System.out.println(resBookList.getCount());
                for (int i = 0; i < resBookList.getCount(); i++) {
                    Book o2 = resBookList.getResults().get(i);
                    response += "{\"id\": " + o2.getBookId()+ ", \"category\": \"" +o2.getCategory()+ "\", \"title\": \""+o2.getTitle()+ "\", \"press\": \""+o2.getPress()+ "\", \"publishYear\": "+o2.getPublishYear()+ ", \"author\": \""+o2.getAuthor()+ "\", \"price\": "+o2.getPrice()+ ", \"stock\": "+o2.getStock()+"}";
                    if (i != resBookList.getCount() - 1)
                        response += ",";
                    //{\"id\": " + o2.getBookId() + ", \"name\": \"" + o2.getName() + "\", \"department\": \"" + o2.getDepar response.append("{\"id\": ").append(o2.getBookId()).append(", \"name\": \"").append(o2.getName()).append("\", \"department\": \"").append(o2.getDepartment()).append("\", \"type\": \"").append(o2.getType()).append("\"}");
                }
                response += "]";
            }
            else if(query.contains("title") && query.contains("category")){
                System.out.println("multi condition query:");
                // 格式为：category=shelbyHU&title=shelbyHU&press=shelbyHU&minPublishYear=1145&maxPublishYear=14&author=shelbyHU&minPrice=1&maxPrice=919
                BookQueryConditions bookQueryConditions = new BookQueryConditions();
                //将query中可能的中文编码转化为utf-8格式:
                query = URLDecoder.decode(query, "UTF-8");
                // 输出解码后的字符串
                System.out.println(query); // 输出： 原码
                // 解析 category
                int categoryStartIndex = query.indexOf("category") + 9; // 获取 "category" 后的索引位置
                int categoryEndIndex = query.indexOf("&", categoryStartIndex); // 获取第一个逗号的位置
                String category;
                if(categoryStartIndex!=categoryEndIndex){
                    category = query.substring(categoryStartIndex, categoryEndIndex);
                    System.out.println(category);
                    bookQueryConditions.setCategory(category);
                }
                // 解析 title
                int titleStartIndex = query.indexOf("title") + 6; // 获取 "title" 后的索引位置
                int titleEndIndex = query.indexOf("&", titleStartIndex); // 获取第一个逗号的位置
                String title;
                if(titleStartIndex!=titleEndIndex){
                    title = query.substring(titleStartIndex, titleEndIndex);
                    System.out.println(title);
                    bookQueryConditions.setTitle(title);
                }
                // 解析 press
                int pressStartIndex = query.indexOf("press") + 6; // 获取 "press" 后的索引位置
                int pressEndIndex = query.indexOf("&", pressStartIndex); // 获取第一个逗号的位置
                String press;
                if(pressStartIndex!=pressEndIndex){
                    press = query.substring(pressStartIndex, pressEndIndex);
                    System.out.println(press);
                    bookQueryConditions.setPress(press);
                }
                // 解析 minPublishYear
                int minPublishYearStartIndex = query.indexOf("minPublishYear") + 15; // 获取 "minPublishYear" 后的索引位置
                int minPublishYearEndIndex = query.indexOf("&", minPublishYearStartIndex); // 获取第一个逗号的位置
                int minPublishYear;
                if(minPublishYearStartIndex!=minPublishYearEndIndex){
                    minPublishYear = Integer.parseInt(query.substring(minPublishYearStartIndex, minPublishYearEndIndex));
                    System.out.println(minPublishYear);
                    bookQueryConditions.setMinPublishYear(minPublishYear);
                }
                // 解析 maxPublishYear
                int maxPublishYearStartIndex = query.indexOf("maxPublishYear") + 15; // 获取 "maxPublishYear" 后的索引位置
                int maxPublishYearEndIndex = query.indexOf("&", maxPublishYearStartIndex); // 获取第一个逗号的位置
                int maxPublishYear;
                if(maxPublishYearStartIndex!=maxPublishYearEndIndex){
                    maxPublishYear = Integer.parseInt(query.substring(maxPublishYearStartIndex, maxPublishYearEndIndex));
                    System.out.println(maxPublishYear);
                    bookQueryConditions.setMaxPublishYear(maxPublishYear);
                }
                // 解析 author
                int authorStartIndex = query.indexOf("author") + 7; // 获取 "author" 后的索引位置
                int authorEndIndex = query.indexOf("&", authorStartIndex); // 获取第一个逗号的位置
                String author;
                if(authorStartIndex!=authorEndIndex){
                    author = query.substring(authorStartIndex, authorEndIndex);
                    System.out.println(author);
                    bookQueryConditions.setAuthor(author);
                }
                // 解析 minPrice
                int minPriceStartIndex = query.indexOf("minPrice") + 9; // 获取 "minPrice" 后的索引位置
                int minPriceEndIndex = query.indexOf("&", minPriceStartIndex); // 获取第一个逗号的位置
                double minPrice;
                if(minPriceStartIndex!=minPriceEndIndex){
                    minPrice = Integer.parseInt(query.substring(minPriceStartIndex, minPriceEndIndex));
                    System.out.println(minPrice);
                    bookQueryConditions.setMinPrice(minPrice);
                }
                // 解析 maxPrice
                int maxPriceStartIndex = query.indexOf("maxPrice") + 9; // 获取 "maxPrice" 后的索引位置
                int maxPriceEndIndex = query.length(); // 获取最后一个字符前的索引位置（排除末尾的 `}`)
                double maxPrice;
                if(maxPriceStartIndex!=maxPriceEndIndex){
                    maxPrice = Integer.parseInt(query.substring(maxPriceStartIndex, maxPriceEndIndex));
                    System.out.println(maxPrice);
                    bookQueryConditions.setMaxPrice(maxPrice);
                }
                //打印bookQueryConditions:
                System.out.printf("category:%s,title:%s,press:%s,minPublishYear:%d,maxPublishYear:%d,author:%s,minPrice:%f,maxPrice:%f\n", bookQueryConditions.getCategory(), bookQueryConditions.getTitle(), bookQueryConditions.getPress(), bookQueryConditions.getMinPublishYear(), bookQueryConditions.getMaxPublishYear(), bookQueryConditions.getAuthor(), bookQueryConditions.getMinPrice(), bookQueryConditions.getMaxPrice());
                ApiResult result = BookHandler.library.queryBook(bookQueryConditions);
                BookQueryResults resBookList = (BookQueryResults) result.payload;
                System.out.println(resBookList.getCount());
                for (int i = 0; i < resBookList.getCount(); i++) {
                    Book o2 = resBookList.getResults().get(i);
                    response += "{\"id\": " + o2.getBookId()+ ", \"category\": \"" +o2.getCategory()+ "\", \"title\": \""+o2.getTitle()+ "\", \"press\": \""+o2.getPress()+ "\", \"publishYear\": "+o2.getPublishYear()+ ", \"author\": \""+o2.getAuthor()+ "\", \"price\": "+o2.getPrice()+ ", \"stock\": "+o2.getStock()+"}";
                    if (i != resBookList.getCount() - 1)
                        response += ",";
                }
                response += "]";

            }
            // 写
            System.out.printf("Response: %s\n", response);
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
            System.out.println("Received POST request data here here~~~: " + requestBodyBuilder.toString());
            //调用数据库操作：
            // 1. 解析请求体，获取卡片信息
            String book_Info = requestBodyBuilder.toString();
            //如果book_Info有id字段，调用updateBook函数，否则调用createBook函数：
            if(book_Info.contains("text/plain")){
                System.out.println("3 here");
                // 解析 book_Info：格式为 text/plain:[{"category":"cate","title":"bookname","press":"pub","publishYear":"2024","author":"author","price":"123","stock":"321"},{"category":"cate","title":"bookname","press":"pub","publishYear":"2024","author":"author","price":"123","stock":"321"}]
                int bookInfoStartIndex = book_Info.indexOf("[{") + 1; // 获取第一个 `[` 的索引位置
                int bookInfoEndIndex = book_Info.indexOf("]"); // 获取最后一个 `]` 的索引位置
                String bookInfo = book_Info.substring(bookInfoStartIndex, bookInfoEndIndex);
                System.out.println(bookInfo);
                String[] bookInfos = bookInfo.split("},");
                //计算bookInfos的长度，一共有多少本书：
                int book_num = bookInfos.length;
                System.out.println(book_num);
                int book_count = 0;
                List<Book> new_books = new ArrayList<>();
                for (String bookInfo1 : bookInfos) {
                    Book book_to_create = new Book();
                    //格式为：{"category":"cate","title":"bookname","press":"pub","publishYear":"2024","author":"author","price":"123","stock":"321"}
                    // 解析 category
                    int categoryStartIndex = bookInfo1.indexOf("category") + 11; // 获取 "category" 后的索引位置
                    int categoryEndIndex = bookInfo1.indexOf(",", categoryStartIndex); // 获取第一个逗号的位置
                    String category = bookInfo1.substring(categoryStartIndex, categoryEndIndex - 1);
                    System.out.println(category);
                    book_to_create.setCategory(category);
                    // 解析 title
                    int titleStartIndex = bookInfo1.indexOf("title") + 8; // 获取 "title" 后的索引位置
                    int titleEndIndex = bookInfo1.indexOf(",", titleStartIndex); // 获取第一个逗号的位置
                    String title = bookInfo1.substring(titleStartIndex, titleEndIndex - 1);
                    System.out.println(title);
                    book_to_create.setTitle(title);
                    // 解析 press
                    int pressStartIndex = bookInfo1.indexOf("press") + 8; // 获取 "press" 后的索引位置
                    int pressEndIndex = bookInfo1.indexOf(",", pressStartIndex - 1); // 获取第一个逗号的位置
                    String press = bookInfo1.substring(pressStartIndex, pressEndIndex - 1);
                    System.out.println(press);
                    book_to_create.setPress(press);
                    // 解析 publishYear
                    int publishYearStartIndex = bookInfo1.indexOf("publishYear") + 14; // 获取 "publishYear" 后的索引位置
                    int publishYearEndIndex = bookInfo1.indexOf(",", publishYearStartIndex); // 获取第一个逗号的位置
                    int publishYear = Integer.parseInt(bookInfo1.substring(publishYearStartIndex, publishYearEndIndex - 1));
                    System.out.println(publishYear);
                    book_to_create.setPublishYear(publishYear);
                    // 解析 author
                    int authorStartIndex = bookInfo1.indexOf("author") + 9; // 获取 "author" 后的索引位置
                    int authorEndIndex = bookInfo1.indexOf(",", authorStartIndex); // 获取第一个逗号的位置
                    String author = bookInfo1.substring(authorStartIndex, authorEndIndex - 1);
                    System.out.println(author);
                    book_to_create.setAuthor(author);
                    // 解析 price
                    int priceStartIndex = bookInfo1.indexOf("price") + 8; // 获取 "price" 后的索引位置
                    int priceEndIndex = bookInfo1.indexOf(",", priceStartIndex); // 获取第一个逗号的位置
                    double price = Double.parseDouble(bookInfo1.substring(priceStartIndex, priceEndIndex - 1));
                    System.out.println(price);
                    book_to_create.setPrice(price);
                    // 解析 stock
                    int stockStartIndex = bookInfo1.indexOf("stock") + 8; // 获取 "stock" 后的索引位置
                    int stockEndIndex = bookInfo1.length() - 1; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
                    if(book_count==book_num-1){
                        stockEndIndex = bookInfo1.length() - 2;
                    }
                    int stock = Integer.parseInt(bookInfo1.substring(stockStartIndex, stockEndIndex));
                    System.out.println(stock);
                    book_to_create.setStock(stock);
                    //调用createBook函数
                    System.out.printf("category:%s,title:%s,press:%s,publishYear:%d,author:%s,price:%f,stock:%d\n", category, title, press, publishYear, author, price, stock);
                    //ApiResult result = BookHandler.library.storeBook(book_to_create);
                    //System.out.println("Create book result: " + result.toString());
                    new_books.add(book_to_create);
                    book_count++;
                }
                //打印new_books:
                System.out.println("new_books:");
                for (Book new_book : new_books) {
                    System.out.printf("category:%s,title:%s,press:%s,publishYear:%d,author:%s,price:%f,stock:%d\n", new_book.getCategory(), new_book.getTitle(), new_book.getPress(), new_book.getPublishYear(), new_book.getAuthor(), new_book.getPrice(), new_book.getStock());
                }
                ApiResult result = BookHandler.library.storeBook(new_books);
                System.out.println("store multi book result: " + result.toString());
                if (result.ok) {
                    exchange.sendResponseHeaders(200, 0);
                    System.out.println("Store all books successfully");
                } else {
                    exchange.sendResponseHeaders(400, 0);
                    System.out.println("Store book failed");
                }
            }
            else if(book_Info.contains("id") && book_Info.contains("category")&& book_Info.contains("inc")){

                //调用modifyBookInfo函数
                System.out.println("1");
                Book book_to_modify = new Book();
                int idStartIndex = book_Info.indexOf("id") + 4; // 获取冒号后的索引位置
                int idEndIndex = book_Info.indexOf(",", idStartIndex); // 获取第一个逗号的位置
                int id = Integer.parseInt(book_Info.substring(idStartIndex, idEndIndex));
                System.out.println(id);
                book_to_modify.setBookId(id);
                // 解析 category
                int categoryStartIndex = book_Info.indexOf("category") + 11; // 获取 "category" 后的索引位置
                int categoryEndIndex = book_Info.indexOf(",", categoryStartIndex); // 获取第一个逗号的位置
                String category = book_Info.substring(categoryStartIndex, categoryEndIndex-1);
                System.out.println(category);
                book_to_modify.setCategory(category);
                // 解析 title
                int titleStartIndex = book_Info.indexOf("title") + 8; // 获取 "title" 后的索引位置
                int titleEndIndex = book_Info.indexOf(",", titleStartIndex); // 获取第一个逗号的位置
                String title = book_Info.substring(titleStartIndex, titleEndIndex-1);
                System.out.println(title);
                book_to_modify.setTitle(title);
                // 解析 press
                int pressStartIndex = book_Info.indexOf("press") + 8; // 获取 "press" 后的索引位置
                int pressEndIndex = book_Info.indexOf(",", pressStartIndex-1); // 获取第一个逗号的位置
                String press = book_Info.substring(pressStartIndex, pressEndIndex-1);
                System.out.println(press);
                book_to_modify.setPress(press);

                // 解析 publishYear
                int publishYearStartIndex = book_Info.indexOf("publishYear") + 13; // 获取 "publishYear" 后的索引位置
                int publishYearEndIndex = book_Info.indexOf(",", publishYearStartIndex); // 获取第一个逗号的位置
                String publishYearStr = book_Info.substring(publishYearStartIndex, publishYearEndIndex);
                System.out.println(publishYearStr);
                // 去除前后的引号：
                if(publishYearStr.charAt(0)=='"'){
                    publishYearStr = publishYearStr.substring(1, publishYearStr.length()-1);
                }
                // 转换为int类型：
                int publishYear = Integer.parseInt(publishYearStr);
                System.out.println(publishYear);
                book_to_modify.setPublishYear(publishYear);

                // 解析 author
                int authorStartIndex = book_Info.indexOf("author") + 9; // 获取 "author" 后的索引位置
                int authorEndIndex = book_Info.indexOf(",", authorStartIndex); // 获取第一个逗号的位置
                String author = book_Info.substring(authorStartIndex, authorEndIndex-1);
                System.out.println(author);
                book_to_modify.setAuthor(author);

                // 解析 price
                int priceStartIndex = book_Info.indexOf("price") + 7; // 获取 "price" 后的索引位置
                int priceEndIndex = book_Info.indexOf(",", priceStartIndex); // 获取第一个逗号的位置
                String priceStr = book_Info.substring(priceStartIndex, priceEndIndex);
                System.out.println(priceStr);
                // 去除前后的引号：
                if(priceStr.charAt(0)=='"'){
                    priceStr = priceStr.substring(1, priceStr.length()-1);
                }
                // 转换为int类型：
                double price = Double.parseDouble(priceStr);
                System.out.println(price);
                book_to_modify.setPrice(price);

                // 解析 stock
                int stockStartIndex = book_Info.indexOf("stock") + 7; // 获取 "stock" 后的索引位置
                int stockEndIndex = book_Info.indexOf(",", stockStartIndex); // 获取最后一个字符前的索引位置（排除末尾的 `}`）
                String stockStr = book_Info.substring(stockStartIndex, stockEndIndex);
                System.out.println(stockStr);
                // 去除前后的引号：
                if(stockStr.charAt(0)=='"'){
                    stockStr = stockStr.substring(1, stockStr.length()-1);
                }
                // 转换为int类型：
                int stock = Integer.parseInt(stockStr);
                System.out.println(stock);
                book_to_modify.setStock(stock);

                //解析stock_inc
                int stock_incStartIndex = book_Info.indexOf("inc") + 5; // 获取 "stock_inc" 后的索引位置
                int stock_incEndIndex = book_Info.length() - 1; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
                String stock_incStr = book_Info.substring(stock_incStartIndex, stock_incEndIndex);
                System.out.println("here1"+stock_incStr);
                // 去除前后的引号：
                if(stock_incStr.charAt(0)=='"'){
                    stock_incStr = stock_incStr.substring(1, stock_incStr.length()-1);
                }
                // 转换为int类型：
                int stock_inc = Integer.parseInt(stock_incStr);
                System.out.println("here2 "+stock_inc);
                ApiResult inc_result =  BookHandler.library.incBookStock(book_to_modify.getBookId(), stock_inc);
                if(inc_result.ok){
                    System.out.println("Increase stock successfully");
                    System.out.printf("id:%d,category:%s,title:%s,press:%s,publishYear:%d,author:%s,price:%f,stock:%d\n", book_to_modify.getBookId(), book_to_modify.getCategory(), book_to_modify.getTitle(), book_to_modify.getPress(), book_to_modify.getPublishYear(), book_to_modify.getAuthor(), book_to_modify.getPrice(), book_to_modify.getStock());
                    ApiResult result = BookHandler.library.modifyBookInfo(book_to_modify);
                    if(result.ok){
                        System.out.println("Update book successfully");
                        exchange.sendResponseHeaders(200, 0);
                    }
                    else{
                        System.out.println("Update book failed");
                        exchange.sendResponseHeaders(400, 0);
                    }
                }
                else{
                    System.out.println("Increase stock failed");
                    exchange.sendResponseHeaders(400, 0);
                }

            }
            else if (book_Info.contains("id")&&book_Info.contains("card_id")) {
                System.out.println("2 here");
                int idStartIndex = book_Info.indexOf("id") + 4; // 获取冒号后的索引位置
                int idEndIndex = book_Info.indexOf(",", idStartIndex); // 获取第一个逗号的位置
                int id = Integer.parseInt(book_Info.substring(idStartIndex, idEndIndex));
                int card_idStartIndex = book_Info.indexOf("card_id") + 10; // 获取 "card_id" 后的索引位置
                int card_idEndIndex = book_Info.length() - 1; // 获取最后一个字符前的索引位置（排除末尾的 `}`)
                int card_id = Integer.parseInt(book_Info.substring(card_idStartIndex, card_idEndIndex-1));
                System.out.printf("id:%d,card_id:%d\n", id, card_id);
                Borrow book_borrow = new Borrow();
                book_borrow.setBookId(id);
                book_borrow.setCardId(card_id);
                book_borrow.setBorrowTime(new Date().getTime());
                book_borrow.setReturnTime(0);
                ApiResult result = BookHandler.library.borrowBook(book_borrow);
                if(result.ok){
                    System.out.println("Borrow book successfully");
                    exchange.sendResponseHeaders(200, 0);
                }else{
                    System.out.println("Borrow book failed");
                    exchange.sendResponseHeaders(400, 0);
                }

            }
            else if(book_Info.contains("id")){
                //调用removeBook函数:
                // 解析 id
                int idStartIndex = book_Info.indexOf(":") + 1; // 获取冒号后的索引位置
                int idEndIndex = book_Info.indexOf("}", idStartIndex); // 获取第一个逗号的位置
                int id = Integer.parseInt(book_Info.substring(idStartIndex, idEndIndex));
                //调用removeBook函数
                System.out.printf("id:%d\n", id);
                ApiResult result = BookHandler.library.removeBook(id);
                System.out.println("Remove book result: " + result.toString());
                if (result.ok) {
                    exchange.sendResponseHeaders(200, 0);
                    System.out.println("Remove book successfully");
                } else {
                    exchange.sendResponseHeaders(400, 0);
                    System.out.println("Remove book failed");
                }
            }
            else {
                Book book_to_create = new Book();
                book_to_create.setBookId(0);
                //格式为：{"category":"cate","title":"bookname","press":"pub","publishYear":"2024","author":"author","price":"123","stock":"321"}
                // 解析 category
                int categoryStartIndex = book_Info.indexOf("category") + 11; // 获取 "category" 后的索引位置
                int categoryEndIndex = book_Info.indexOf(",", categoryStartIndex); // 获取第一个逗号的位置
                String category = book_Info.substring(categoryStartIndex, categoryEndIndex-1);
                System.out.println(category);
                book_to_create.setCategory(category);
                // 解析 title
                int titleStartIndex = book_Info.indexOf("title") + 8; // 获取 "title" 后的索引位置
                int titleEndIndex = book_Info.indexOf(",", titleStartIndex); // 获取第一个逗号的位置
                String title = book_Info.substring(titleStartIndex, titleEndIndex-1);
                System.out.println(title);
                book_to_create.setTitle(title);
                // 解析 press
                int pressStartIndex = book_Info.indexOf("press") + 8; // 获取 "press" 后的索引位置
                int pressEndIndex = book_Info.indexOf(",", pressStartIndex-1); // 获取第一个逗号的位置
                String press = book_Info.substring(pressStartIndex, pressEndIndex-1);
                System.out.println(press);
                book_to_create.setPress(press);
                // 解析 publishYear
                int publishYearStartIndex = book_Info.indexOf("publishYear") + 14; // 获取 "publishYear" 后的索引位置
                int publishYearEndIndex = book_Info.indexOf(",", publishYearStartIndex); // 获取第一个逗号的位置
                int publishYear = Integer.parseInt(book_Info.substring(publishYearStartIndex, publishYearEndIndex-1));
                System.out.println(publishYear);
                book_to_create.setPublishYear(publishYear);
                // 解析 author
                int authorStartIndex = book_Info.indexOf("author") + 9; // 获取 "author" 后的索引位置
                int authorEndIndex = book_Info.indexOf(",", authorStartIndex); // 获取第一个逗号的位置
                String author = book_Info.substring(authorStartIndex, authorEndIndex-1);
                System.out.println(author);
                book_to_create.setAuthor(author);
                // 解析 price
                int priceStartIndex = book_Info.indexOf("price") + 8; // 获取 "price" 后的索引位置
                int priceEndIndex = book_Info.indexOf(",", priceStartIndex); // 获取第一个逗号的位置
                double price = Double.parseDouble(book_Info.substring(priceStartIndex, priceEndIndex-1));
                System.out.println(price);
                book_to_create.setPrice(price);
                // 解析 stock
                int stockStartIndex = book_Info.indexOf("stock") + 8; // 获取 "stock" 后的索引位置
                int stockEndIndex = book_Info.length() - 1; // 获取最后一个字符前的索引位置（排除末尾的 `}`）
                int stock = Integer.parseInt(book_Info.substring(stockStartIndex, stockEndIndex-1));
                System.out.println(stock);
                book_to_create.setStock(stock);


                //调用createBook函数
                System.out.printf("category:%s,title:%s,press:%s,publishYear:%d,author:%s,price:%f,stock:%d\n", category, title, press, publishYear, author, price, stock);
                ApiResult result = BookHandler.library.storeBook(book_to_create);
                System.out.println("Create book result: " + result.toString());
                exchange.sendResponseHeaders(200, 0);
            }


            // 2. 插入数据库
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200


            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Book created successfully".getBytes());
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
}
