import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;
import java.util.logging.Logger;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;


import handlers.LoginHandler;
import handlers.RegisterHandler;
import handlers.SearchHandeler;
import handlers.PriceUpdateHandler;
import handlers.ProductHandler;
import handlers.PicHandler;

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
            ProductHandler productHandler = new ProductHandler();
            LoginHandler loginHandler = new LoginHandler();
            RegisterHandler registerHandler = new RegisterHandler();
            SearchHandeler searchHandeler = new SearchHandeler();
            PriceUpdateHandler priceUpdateHandler = new PriceUpdateHandler();
            PicHandler picHandler = new PicHandler();
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/product", productHandler);
            server.createContext("/login", loginHandler);
            server.createContext("/register", registerHandler);
            server.createContext("/search", searchHandeler);
            server.createContext("/update", priceUpdateHandler);
            server.createContext("/pic",picHandler);
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
