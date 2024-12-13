package handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import crawler.HistoryCrawler;
import crawler.JDCrawler;
import crawler.TBCrwaler;
import database.LibraryManagementSystem;
import database.LibraryManagementSystemImpl;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SearchHandeler implements HttpHandler {
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
        } else {
            // 其他请求返回405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }
    private static void handleGetRequest(HttpExchange exchange) throws IOException {
        System.out.println("GET request received in SearchHandler crawler");
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0); // 发送 200 响应码，表示请求成功

        // 获取输出流
        OutputStream outputStream = exchange.getResponseBody();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        // 获取查询参数（例如商品关键词）
        URI requestedUri = exchange.getRequestURI();
        String query = requestedUri.getRawQuery();
        String[] parts = query.split("=", 2);
        String search = parts[1];

        // 创建爬虫实例
        JDCrawler jdScraper = new JDCrawler(writer);
        TBCrwaler tbScraper = new TBCrwaler(writer); // 将writer传入TBCrwaler，逐个发送商品
        // 创建线程来并行执行爬虫操作
        Thread jdThread = new Thread(() -> {
            try {
                jdScraper.craw(search); // 逐个发送京东商品
            } catch (Exception e) {
                try {
                    writer.write("data: Error in JD Scraper: " + e.getMessage() + "\n\n");
                    writer.flush();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        Thread tbThread = new Thread(() -> {
            try {
                tbScraper.craw(search); // 逐个发送淘宝商品
            } catch (Exception e) {
                try {
                    writer.write("data: Error in TB Scraper: " + e.getMessage() + "\n\n");
                    writer.flush();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        // 启动线程
        jdThread.start();
        tbThread.start();
        System.out.println("Threads started");
        // 等待两个线程完成
        try {
            jdThread.join();
            tbThread.join();
            System.out.println("All threads finished");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("All threads finished--2");
        // 完成后关闭输出流

        try {
            writer.close();
            // 流一定要close！！！小心泄漏
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("All threads finished--3");
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

        // 获取请求体
        InputStream requestBody = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
        StringBuilder requestBodyBuilder = new StringBuilder();
        exchange.getResponseHeaders().set("Content-Type", "text/plain"); // 设置响应类型为纯文本

        String line;

        // 读取请求体
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
        }
        System.out.println("Received POST request data: " + requestBodyBuilder.toString());

        // 提取 URL
        String product_url = requestBodyBuilder.toString();
        int titleStartIndex = product_url.indexOf("url") + 6;  // 获取 "url" 后的索引位置
        int titleEndIndex = product_url.indexOf("}", titleStartIndex); // 获取第一个 '}' 的位置
        String title = product_url.substring(titleStartIndex, titleEndIndex - 1); // 截取 URL

        System.out.println("Product URL extracted: " + title);

        // 创建爬虫实例
        HistoryCrawler historyCrawler = new HistoryCrawler();

        // 创建线程来并行执行爬虫操作
        Thread historyThread = new Thread(() -> {
            try {
                historyCrawler.getData(title); // 执行爬虫任务
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 启动爬虫线程
        historyThread.start();
        try {
            // 等待爬虫线程完成
            historyThread.join(); // 等待线程执行完毕

            // 检查是否页面已关闭（客户端已断开）
            if (!exchange.getResponseHeaders().isEmpty()) {
                // 发送成功响应头
                exchange.sendResponseHeaders(200, 0); // 200 OK，响应体长度为0（没有返回内容）
                System.out.println("History image stored successfully");

                // 发送响应体
                String response = "Crawling completed for product: " + title;
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());  // 写入响应内容
                os.close();  // 关闭输出流
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            // 发送错误响应头
            exchange.sendResponseHeaders(400, 0); // 400 Bad Request
            System.out.println("History image store failed");

            // 发送错误响应体
            String errorResponse = "Error occurred while processing the request.";
            OutputStream os = exchange.getResponseBody();
            os.write(errorResponse.getBytes());  // 写入错误信息
            os.close();  // 关闭输出流
        } finally {
            // 线程中断检查：如果请求已被中断（例如页面刷新）
            if (Thread.interrupted()) {
                historyThread.interrupt(); // 中断爬虫线程
                System.out.println("Thread interrupted due to page refresh.");
            }
        }
    }

}