import com.google.gson.Gson;
import crawler.HistoryCrawler;
import crawler.JDCrawler;
import crawler.TBCrwaler;
import crawler.PriceCrwaler;
import database.LibraryManagementSystem;
import database.LibraryManagementSystemImpl;
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
import java.nio.charset.StandardCharsets;

import java.util.List;

import java.util.Map;

import java.util.HashMap;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import handlers.LoginHandler;
import handlers.RegisterHandler;
import handlers.SearchHandeler;
import handlers.PriceUpdateHandler;
import handlers.CardHandler;
import handlers.BorrowHandler;
import handlers.ProductHandler;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
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
            PriceUpdateHandler priceUpdateHandler = new PriceUpdateHandler();
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/home/card", cardHandler);
            server.createContext("/home/borrow", borrowHandler);
            server.createContext("/home/product", productHandler);
            server.createContext("/login", loginHandler);
            server.createContext("/register", registerHandler);
            server.createContext("/search", searchHandeler);
            server.createContext("/update", priceUpdateHandler);
            server.setExecutor(null);
            server.start();
            log.info("Server is listening on port 8000.");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line = reader.readLine(); // 读取一行文本
            if (line != null) {
                System.out.println("Read line: " + line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
