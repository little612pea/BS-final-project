package crawler;

import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import entities.Product;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import queries.ApiResult;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import database.LibraryManagementSystem;
import database.LibraryManagementSystemImpl;

public class PriceCrwaler {
    private static WebDriver driver;
    private static WebDriverWait wait;
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
    public PriceCrwaler() {
    }


    public static int add_cookie(String url) throws IOException {
        File file = null;
        int type = 0;
        if (url.contains("tmall") || url.contains("taobao")) {
            driver.get("https://www.taobao.com/");
            driver.manage().deleteAllCookies();
            System.out.println("pricecrawler--loading cookies for tb");
            file = new File("D:\\home\\BS\\BS-final-project\\src\\crawler\\tb\\cookies_tb.txt");
//            File file = new File("/app/cookies_jd.txt");
            type = 0;
        }
        else if (url.contains("jd")) {
            driver.get("https://www.jd.com/");
            driver.manage().deleteAllCookies();
            System.out.println("pricecrawler--loading cookies for jd");
            file = new File("D:\\home\\BS\\BS-final-project\\src\\crawler\\jd\\cookies_jd.txt");
            type = 1;
        }


        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();
        String jsonStr = content.toString();
        JSONArray jsonArray = new JSONArray(jsonStr); // 如果 JSON 是数组格式

        // 遍历 JSON 数组并将每个 JSON 对象转换为 Selenium 的 Cookie
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            // 提取 cookie 的必要属性
            String name = jsonObject.getString("name");
            String value = jsonObject.getString("value");
            String domain = jsonObject.optString("domain", null);
            String path = jsonObject.optString("path", "/");
            boolean isSecure = jsonObject.optBoolean("secure", false);
            boolean isHttpOnly = jsonObject.optBoolean("httpOnly", false);

            // 设置 cookie 的到期时间（如果有）
            Cookie cookie = new Cookie.Builder(name, value)
                    .domain(domain)
                    .path(path)
                    .isSecure(isSecure)
                    .isHttpOnly(isHttpOnly)
                    .build();

            driver.manage().addCookie(cookie);
        }
        randomSleep(2, 4);
        return type;
    }
    public static Map<Integer, Double> searchGoods(List<Integer> ids, List<Double> prices,List<String> title, List<String> sources) throws IOException {
        Map<Integer, Double> priceUpdates = new HashMap<>();
        try {
            for (int i = 0; i < ids.size(); i++) {
                int id = ids.get(i);
                String url = sources.get(i);
                int type = add_cookie(url);
                // 绕过检测
                String title_cur = title.get(i);
                try{
                    // 模拟人类行为绕过检测
                    ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
                    WebElement input = null;
                    WebElement submit = null;
                    if(type==0){
                        input = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#q")));
                        submit = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#J_TSearchForm > div.search-button > button")));
                    }
                    else{
                        input = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#key")));
                        submit = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#search > div > div.form > button")));
                    }
                    input.sendKeys(title_cur);
                    submit.click();}
                catch (Exception e) {
                    System.out.println("searchGoods: error");
                    e.printStackTrace();
                }
                // 等待搜索完成
                TimeUnit.SECONDS.sleep(10);
                Double price = prices.get(i);
                String old_title = title.get(i);
                randomSleep(2, 4);
                // 调用爬虫方法，爬取商品价格
                getGoods(id, price, type, old_title, priceUpdates);
                // 将爬取到的价格存入 prices 数组中
            }

        } catch (TimeoutException e) {
            System.out.println("searchGoods: error");
            e.printStackTrace();
        } catch(Exception e) {
            System.out.println("searchGoods: error");
            e.printStackTrace();
        }
        return priceUpdates;
    }

    public static void getGoods(int id, double price, int type,String old_title, Map priceUpdates) {
        System.out.println("getGoods--price_crawler");
        Product product_modify_price = new Product();
        try {
            // 随机等待 1-3 秒
            randomSleep(2, 4);
            // 获取页面源代码
            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);
            if (pageSource == null || pageSource.trim().isEmpty()) {
                System.out.println("Page source is empty!");
                return;
            }
            double new_price = -1;
            if(type==0){
                // 淘宝
                Elements items = doc.select(".doubleCardWrapperAdapt--mEcC7olq");
                System.out.println("items--" + items.size());
                for (Element item : items) {
                    // 定位商品标题
                    if(item.select(".title--qJ7Xg_90").text().equals(old_title))
                    {
                        String title = item.select(".title--qJ7Xg_90").text();
                        // 定位价格
                        String priceInt = item.select(".priceInt--yqqZMJ5a").text();
                        String priceFloat = item.select(".priceFloat--XpixvyQ1").text();
                        double raw_price = (priceInt.isEmpty() || priceFloat.isEmpty()) ? 0.0 : Double.parseDouble(priceInt + priceFloat);
                        new_price = Math.round(raw_price * 10) / 10.0;
                        System.out.println("new_price tb--" + new_price);
                    }
                }
            }
            else{
                List<WebElement> items = driver.findElements(By.cssSelector(".gl-item"));
                WebElement item= items.get(0);
                // Get product title
                String title = item.findElement(By.cssSelector(".p-name.p-name-type-2")).getText();
                // Get price string and clean it
                String priceString = item.findElement(By.cssSelector(".p-price")).getText();
                String cleanPriceString = priceString.replaceAll("[^\\d.]", "");

                // Get price element and clean it
                WebElement priceElement = item.findElement(By.cssSelector("i[data-price]"));
                cleanPriceString = priceElement.getText().trim();

                // Convert cleaned price to float
                float raw_price = Float.parseFloat(cleanPriceString);
                new_price = Math.round(raw_price * 10) / 10.0;
                System.out.println("new_price jd--" + new_price);
            }
            product_modify_price.setPrice(new_price);
            product_modify_price.setProductId(id);
            priceUpdates.put(id, new_price);
            // 调用数据库
            ApiResult result = null;
            if(new_price!=-1){
                result = library.modifyPrice(product_modify_price);
            }

            if (result.ok) {
                System.out.println("Successfully modify price.");
            } else {
                System.out.println("Failed to modify price.");
            }

        } catch (Exception e) {
            System.out.println("Error while scraping goods: " + e.getMessage());
        }
    }

    private static void randomSleep(int minSeconds, int maxSeconds) {
        try {
            // 生成 minSeconds 到 maxSeconds 范围内的随机等待时间（单位为毫秒）
            int sleepTime = (minSeconds * 1000) + (int) (Math.random() * ((maxSeconds - minSeconds) * 1000));
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static Map<Integer, Double> craw(List<Integer> ids, List<Double> prices,List<String> titles, List<String> sources) {
        try{
            System.setProperty("webdriver.chrome.driver", "C:\\Users\\23828\\.cache\\selenium\\chromedriver\\win64\\130.0.6723.116\\chromedriver.exe");
//            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            options.setBinary("C:\\Users\\23828\\.cache\\selenium\\chrome\\win64\\130.0.6723.116\\chrome.exe");
//            options.setBinary("/usr/bin/chromium-browser");
            options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "blink-settings=imagesEnabled=false");
//            options.addArguments("--headless");
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("excludeSwitches", "enable-automation");
            driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            System.out.println("searchGoods");
            Map new_product_map =  searchGoods(ids, prices,titles, sources);
            //返回价格更低的商品id和新价格：
            return new_product_map;
        }
        catch (Exception e){
            System.out.println("Error: " + e);
        }finally {
            // 确保无论如何都会关闭浏览器
            if (driver != null) {
                driver.quit();
            }
        }

        return null;
    }
}
