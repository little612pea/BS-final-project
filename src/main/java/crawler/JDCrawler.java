package crawler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.json.TypeToken;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JDCrawler {
    private static OutputStreamWriter writer;
    private static WebDriver driver;
    private static WebDriverWait wait;
    static List<CrawProduct> crawProductList = new ArrayList<>();

    public JDCrawler(OutputStreamWriter writer) {
        this.writer = writer;  // 通过构造函数传入 writer
    }


    public static int CheckMode(String keyword){
        System.out.println("keyword"+keyword);
        if(keyword.endsWith(".png")){
            return 1;
        }
        return 0;
    }
    public static <IOException extends Throwable> void searchGoods(int startPage, int totalPages, String keyword) {
        try {
            driver.get("https://www.jd.com/");
            driver.manage().deleteAllCookies();
            System.out.println("searchGoods--loading cookies");
//            File file = new File("D:\\home\\BS\\BS-final-project\\src\\crawler\\jd\\cookies_jd.txt");
            File file = new File("/app/cookies_jd.txt");

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();
            String jsonStr = content.toString();
            JSONArray jsonArray = new JSONArray(jsonStr); // 如果 JSON 是数组格式
            int mode = CheckMode(keyword);
            System.out.println("mode:"+mode);
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
            try{
                // 模拟人类行为绕过检测
                ((JavascriptExecutor) driver).executeScript(
                        "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
                );
                WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#key")));
                WebElement submit = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#search > div > div.form > button")));
                if (mode==0){
//                    input.sendKeys(keyword);
//                    submit.click();
                    //改成拼接url的方式：
                    driver.get("https://search.jd.com/Search?keyword="+keyword);
                }
                else if(mode==1){
                    driver.get("https://search.jd.com/Search?keyword=iphone");
                    randomSleep(2, 3);
                    driver.get("https://www.jd.com/");
                    input = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"search-img-upload\"]/input")));
                    input.sendKeys(keyword);
                }
            }
            catch (Exception e) {
                System.out.println("searchGoods: error");
                e.printStackTrace();
            }

            // 等待搜索完成
            randomSleep(2, 8);

            if (startPage != 1) {
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                randomSleep(2, 4);

                WebElement pageInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("/html/body/div[5]/div[2]/div[2]/div[1]/div/div[3]/div/span[2]/input")));
                pageInput.sendKeys(String.valueOf(startPage));

                WebElement admit = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("/html/body/div[5]/div[2]/div[2]/div[1]/div/div[3]/div/span[2]/a")));
                admit.click();
            }


            if(mode==0){
                getGoods();
                for (int i = startPage + 1; i < startPage + totalPages; i++) {
                    pageTurning(i);
                }
            } else if (mode==1) {
                GetGoodsPhoto();
            }


        } catch (TimeoutException e) {
            System.out.println("searchGoods: error");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        catch (Exception e) {
            System.out.println("searchGoods: error");
            System.out.println(e.getMessage());
            e.printStackTrace();
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

    public static void getGoods() {
        // Wait for 2-4 seconds before extracting the page content
        randomSleep(2, 4);
        try {
            // 随机等待 1-3 秒
            Thread.sleep((int) (Math.random() * 2000) + 1000);

            // 滚动页面 5 次，每次滚动 1000 像素
            for (int i = 0; i < 5; i++) {
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 1000);");
                Thread.sleep(200);
            }
            System.out.println("getGoods");
            String html = driver.getPageSource();
            Document doc = Jsoup.parse(html);
            // Extract all products
            List<WebElement> items = driver.findElements(By.cssSelector(".gl-item"));
            for (WebElement item : items) {
                //打印item里面的内容：
                String outerHtml = item.getAttribute("outerHTML");
                System.out.println(outerHtml);
                // Get product title
                // Get price string and clean it
                String priceString = item.findElement(By.cssSelector(".p-price")).getText();
                String cleanPriceString = priceString.replaceAll("[^\\d.]", "");

                // Get price element and clean it
                WebElement priceElement = item.findElement(By.cssSelector("i[data-price]"));
                cleanPriceString = priceElement.getText().trim();
                cleanPriceString.replaceAll("[^\\d.]", "");
                if (cleanPriceString.isEmpty()) {
                    continue;
                }

                // Convert cleaned price to float
                float raw_price = Float.parseFloat(cleanPriceString);
                double price = Math.round(raw_price * 10) / 10.0;

                // Get comment count
                String comment = item.findElement(By.cssSelector(".p-commit")).getText();
                String title = item.findElement(By.cssSelector(".p-name a")).getText();

                // Get shop name
                String shop = item.findElement(By.cssSelector(".p-shop")).getText();

                // Get image URL
                String img = item.findElement(By.cssSelector(".p-img")).getAttribute("outerHTML");
                String img_url = extractImageUrl(img);
                img_url = img_url.replaceAll("^[^\"]*\".*?//", "");
                // Get product link

                if (img_url.startsWith("//")) {
                    img_url = "http:" + img_url;
                } else if (!img_url.startsWith("http://") && !img_url.startsWith("https://")) {
                    // 如果不以 http:// 或 https:// 开头，则添加 http://
                    img_url = "http://" + img_url;
                }
                String href = extractProductLink(img);
                // Create product object and add to the list
                CrawProduct crawProduct = new CrawProduct(title, price, comment,null, shop, img_url, href,0);
                String productJson = new Gson().toJson(crawProduct);
                // 将爬取到的产品信息发送给SSE服务
                try {
                    writer.write("data: " + productJson + "\n\n");
                    writer.flush();  // 刷新输出流
                } catch (IOException e) {
                    System.err.println("Error while flushing: " + e.getMessage()); // 打印异常
                }
        }
    } catch (Exception e) {
        System.out.println("Error while scraping goods: " + e.getMessage());
    }
    }

    private static void GetGoodsPhoto(){
        // Wait for 2-4 seconds before extracting the page content
        randomSleep(2, 4);
        try {
            // 随机等待 1-3 秒
            Thread.sleep((int) (Math.random() * 2000) + 1000);

            // 滚动页面 5 次，每次滚动 1000 像素
            for (int i = 0; i < 5; i++) {
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 1000);");
                Thread.sleep(200);
            }
            System.out.println("getGoods");
            String html = driver.getPageSource();
            Document doc = Jsoup.parse(html);

            // Extract all products
            List<WebElement> items = driver.findElements(By.cssSelector(".gl-item"));
            for (WebElement item : items) {
                try {
                    String outerHtml = item.getAttribute("outerHTML");
                    System.out.println(outerHtml);
                    String title = item.findElement(By.cssSelector(".p-name a")).getText().trim();
                    System.out.println("Title: " + title);
                    // 获取商品价格
                    WebElement priceElement = item.findElement(By.cssSelector(".p-price strong"));
                    String priceString = priceElement.getText().replaceAll("[^\\d.]", "").trim();
                    if (priceString.isEmpty()) priceString="0.0";
                    float raw_price = Float.parseFloat(priceString);
                    double price = Math.round(raw_price * 10) / 10.0;
                    System.out.println("Price: " + price);
                    // 获取评论数量
                    WebElement commentElement = item.findElement(By.cssSelector(".p-commit a"));
                    String comment = commentElement.getText().replaceAll("[^\\d]", "").trim();
                    System.out.println("Comment: " + comment);
                    // 获取商店名称
                    String shop = "";
                    try {
                        shop = item.findElement(By.cssSelector(".p-shop a")).getText().trim();
                    } catch (Exception e) {
                        shop = "自营"; // 自营商品时，店铺名称可能不存在
                    }
                    System.out.println("Shop: " + shop);

                    // 获取图片URL
                    WebElement imgElement = item.findElement(By.cssSelector(".p-img img"));
                    String img_url = imgElement.getAttribute("src");
                    if (img_url == null || img_url.isEmpty()) {
                        img_url = imgElement.getAttribute("data-lazy-img"); // 备用图片属性
                    }
                    if (img_url.startsWith("//")) {
                        img_url = "http:" + img_url;
                    }
                    System.out.println("Image URL: " + img_url);

                    // 获取商品链接
                    WebElement linkElement = item.findElement(By.cssSelector(".p-name a"));
                    String href = linkElement.getAttribute("href");
                    if (href.startsWith("//")) {
                        href = "http:" + href;
                    }

                    // 创建商品对象
                    CrawProduct crawProduct = new CrawProduct(title, price, comment, null, shop, img_url, href, 0);

                    // 转为JSON字符串
                    String productJson = new Gson().toJson(crawProduct);

                    // 将爬取到的产品信息发送给SSE服务
                    writer.write("data: " + productJson + "\n\n");
                    System.out.println("data: " + productJson + "\n\n");
                    writer.flush();

                } catch (Exception e) {
                    System.err.println("Error parsing item: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error while scraping goods: " + e.getMessage());
        }
    }

    // Helper function to extract the image URL
    private static String extractImageUrl(String imgHtml) {
//        System.out.println(imgHtml);
        Pattern pattern = Pattern.compile("data-lazy-img=\"(.*?)\" source-data-lazy-img=\"\"");
        Matcher matcher = pattern.matcher(imgHtml);
        if (matcher.find() && !matcher.group(1).equals("done")){
            return matcher.group(1);
        } else {
            pattern = Pattern.compile("src=\"(.*?)\"");
            matcher = pattern.matcher(imgHtml);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    // Helper function to extract the product link
    private static String extractProductLink(String imgHtml) {
        Pattern pattern = Pattern.compile("href=\"(.*?)\"");
        Matcher matcher = pattern.matcher(imgHtml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public static void pageTurning(int pageNumber) {
        try {
            // 获取当前页面 URL
            String currentUrl = driver.getCurrentUrl();
            System.out.println("Current URL: " + currentUrl);
            URI uri = new URI(currentUrl);
            String query = uri.getQuery();
            Map<String, String> queryParams = splitQuery(query);

            // 更新 'page' 参数
            if (queryParams.containsKey("page")) {
                int newPage;
                try {
                    newPage = Integer.parseInt(queryParams.get("page")) + 2;
                } catch (NumberFormatException e) {
                    newPage = 1;
                }
                queryParams.put("page", String.valueOf(newPage));
            } else {
                queryParams.put("page", "crawler");
            }

            // 更新 's' 参数
            if (queryParams.containsKey("s")) {
                int newS;
                try {
                    newS = Integer.parseInt(queryParams.get("s")) + 60;
                } catch (NumberFormatException e) {
                    newS = 1;
                }
                queryParams.put("s", String.valueOf(newS));
            } else {
                queryParams.put("s", "crawler");
            }

            // 构建新的查询字符串和 URL
            String newQuery = queryParams.entrySet().stream()
                    .map(entry -> {
                        return entry.getKey() + "=" + entry.getValue();
                    })
                    .collect(Collectors.joining("&"));
            URI newUri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment());
            String newUrl = newUri.toString();
            System.out.println("Navigating to: " + newUrl);
            // 导航到新的 URL
            try {
                driver.get(newUrl);
            } catch (Exception e) {
                System.out.println("Error navigating to " + newUrl + ": " + e.getMessage());
            }

            getGoods(); // 调用抓取商品的方法

        } catch (URISyntaxException | UnsupportedEncodingException e) {
            System.out.println("Error parsing URL: " + e.getMessage());
        }
    }

    // 辅助方法：解析查询参数为 Map
    private static Map<String, String> splitQuery(String query) throws URISyntaxException, UnsupportedEncodingException {
        Map<String, String> queryPairs = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
            String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
            queryPairs.put(key, value);
        }
        return queryPairs;
    }

    public static void craw(String keyword) {
        try{
            keyword = URLDecoder.decode(keyword, StandardCharsets.UTF_8.name());
            System.out.println("Crawling JD for keyword: " + keyword);
//            System.setProperty("webdriver.chrome.driver", "C:\\Users\\23828\\.cache\\selenium\\chromedriver\\win64\\130.0.6723.116\\chromedriver.exe");
            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");

            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
//            options.setBinary("C:\\Users\\23828\\.cache\\selenium\\chrome\\win64\\130.0.6723.116\\chrome.exe");
            options.setBinary("/usr/bin/chromium-browser");
            options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--headless");
            options.addArguments("excludeSwitches", "enable-automation");
            driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            System.out.println("searchGoods");
            searchGoods(1, 3, keyword);
        }
        catch (Exception e){
            System.out.println("Error: " + e);
        } finally {
        // 确保无论如何都会关闭浏览器
        if (driver != null) {
            driver.quit();
        }
    }

    }
    static class CrawProduct {
        private String title;
        private double price;
        private String comment; // 新增的字段
        private String deal;
        private String shop;
        private String img_url;
        private String source;
        private int favorite;


        public CrawProduct(String title, double price, String comment, String deal, String shop, String img_url, String source , int favorite) {
            this.title = title;
            this.price = price;
            this.comment = comment; // 初始化 comment 字段
            this.deal = deal;
            this.shop = shop;
            this.img_url = img_url;
            this.source = source;
            this.favorite = 0;

        }

        // Getters for each field
        public String getTitle() { return title; }
        public double getPrice() { return price; }
        public String getDeal() { return deal; }
        public String getShop() { return shop; }
        public String getImgUrl() { return img_url; }
        public String getSource() { return source; }
        public String getComment() { return comment; } // 新增的 getter 方法
    }
}





