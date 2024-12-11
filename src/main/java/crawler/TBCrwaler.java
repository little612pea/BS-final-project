package crawler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TBCrwaler {
    private static OutputStreamWriter writer;
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static WebDriver.Options manage;
    private static JavascriptExecutor executor;
    static List<CrawProduct> crawProductList = new ArrayList<>();

    public TBCrwaler(OutputStreamWriter writer) {
        this.writer = writer;  // 通过构造函数传入 writer
    }
    public static String searchGoods(int startPage, int totalPages, String keyword) {
        try {
            driver.get("https://www.taobao.com");
            driver.manage().deleteAllCookies();
            System.out.println("searchGoods--loading cookies");
            File file = new File("D:\\home\\BS\\BS-final-project\\src\\crawler\\tb\\cookies_tb.txt");
//            File file = new File("/app/cookies_tb.txt");

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

            // 绕过检测
            ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#q")));
            WebElement submit = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#J_TSearchForm > div.search-button > button")));
            input.sendKeys(keyword);
            submit.click();

            // 等待搜索完成
            TimeUnit.SECONDS.sleep(10);

            // 如果不是从第一页开始爬取，滑动到底部并跳转
            if (startPage != 1) {
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                TimeUnit.SECONDS.sleep((int) (Math.random() * 3) + 1);

                WebElement pageInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[5]/div[2]/div[2]/div[1]/div/div[3]/div/span[2]/input")));
                pageInput.sendKeys(String.valueOf(startPage));

                WebElement admit = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[5]/div[2]/div[2]/div[1]/div/div[3]/div/span[2]/a")));
                admit.click();
            }

            getGoods();  // 调用商品提取方法
            for (int i = startPage + 1; i < startPage + totalPages; i++) {
                pageTurning(i);
            }

        } catch (TimeoutException | InterruptedException e) {
            System.out.println("searchGoods: error");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch(Exception e) {
            System.out.println("searchGoods: error");
            e.printStackTrace();
        }
        return keyword;
    }

    public static void getGoods() {
        System.out.println("getGoods--taobao");
        List<Product> productList = new ArrayList<>();
        try {
            // 随机等待 1-3 秒
            Thread.sleep((int) (Math.random() * 2000) + 1000);

            // 滚动页面 5 次，每次滚动 1000 像素
            for (int i = 0; i < 5; i++) {
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 1000);");
                Thread.sleep(200);
            }

            // 获取页面源代码
            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);

            // 提取所有商品的共同父元素
            Elements items = doc.select(".doubleCardWrapperAdapt--mEcC7olq");
            System.out.println("items--" + items.size());
            for (Element item : items) {
                // 定位商品标题
                String title = item.select(".title--qJ7Xg_90").text();
                String href = item.attr("href");
                href = extractIdFromUrl(href);

                // 定位价格
                String priceInt = item.select(".priceInt--yqqZMJ5a").text();
                String priceFloat = item.select(".priceFloat--XpixvyQ1").text();
                double raw_price = (priceInt.isEmpty() || priceFloat.isEmpty()) ? 0.0 : Double.parseDouble(priceInt + priceFloat);
                double price = Math.round(raw_price * 10) / 10.0;
                // 定位交易量
                String deal = item.select(".realSales--XZJiepmt").text();

                // 定位店名
                String shop = item.select(".shopNameText--DmtlsDKm").text();

                // 定位 img_url
                String img_url = item.select(".mainPic--Ds3X7I8z").attr("src");
                if (img_url.isEmpty()) {
                    img_url = item.select(".mainPic--ZzRJ1jkn").attr("src");
                }

                // 创建商品对象
                CrawProduct crawProduct = new CrawProduct(title, price, null, deal, shop, img_url, href, 0);
                if (title.isEmpty() || price == 0.0) {
                    continue;
                }
                String productJson = new Gson().toJson(crawProduct);
                // 将爬取到的产品信息发送给SSE服务
                try {
                    System.out.println("Sending product: " + productJson);
                    writer.write("data: " + productJson + "\n\n");
                    writer.flush();  // 刷新输出流
                } catch (IOException e) {
                    System.err.println("Error while flushing: " + e.getMessage()); // 打印异常
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Thread sleep interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error while scraping goods: " + e.getMessage());
        }
    }

    public static String extractIdFromUrl(String url) {
        // 使用正则表达式查找 id 参数
        Pattern pattern = Pattern.compile("id=(\\d+)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            // 提取出 id 的值
            String idValue = matcher.group(1);
            String newUrl;

            // 根据 URL 判断生成新的 URL
            if (url.contains("detail.tmall.com")) {
                newUrl = "https://detail.tmall.com/item.htm?id=" + idValue;
            } else {
                newUrl = "https://item.taobao.com/item.htm?id=" + idValue;
            }

            return newUrl;
        } else {
            return "ID not found in URL";
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
    public static void pageTurning(int pageNumber) {
        // 滑动到页面底端
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");

        // 等待页面加载完成
        randomSleep(1, 3);

        try {
            // 找到“下一页”按钮并滚动到其可见区域
            WebElement nextButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[@id=\"sortBarWrap\"]/div[1]/div[2]/div[2]/div[8]/div/button[2]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextButton);

            // 再次滑动确保完全显示
            randomSleep(1, 2);

            // 点击“下一页”按钮
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);

            // 确认翻页是否成功，等待页码变为指定页数
            WebElement pageIndicator = driver.findElement(By.xpath("//*[@id=\"sortBarWrap\"]/div[1]/div[2]/div[2]/div[8]/div/span/em"));
            wait.until(ExpectedConditions.textToBePresentInElement(pageIndicator, String.valueOf(pageNumber)));

            // 调用获取商品信息的方法
            getGoods();
        } catch (TimeoutException e) {
            // 如果超时，递归调用，重试翻页
            pageTurning(pageNumber);
        }
    }

    public static void craw(String keyword) {
        try{
            keyword = URLDecoder.decode(keyword, StandardCharsets.UTF_8.name());
            System.out.println("Crawling Taobao for keyword: " + keyword);
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
            searchGoods(1, 2, keyword);
        }
        catch (Exception e){
            System.out.println("Error: " + e);
        }finally {
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
