package crawler;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class HistoryCrawler {
    private static OutputStreamWriter writer;
    private static WebDriver driver;
    private static WebDriverWait wait;

    public static void searchGoods(String shortUrl) {
//        System.setProperty("webdriver.chrome.driver", "C:\\Users\\23828\\.cache\\selenium\\chromedriver\\win64\\130.0.6723.116\\chromedriver.exe");
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        // 配置 Chrome 选项
        ChromeOptions options = new ChromeOptions();
//        options.setBinary("C:\\Users\\23828\\.cache\\selenium\\chrome\\win64\\130.0.6723.116\\chrome.exe");
        options.setBinary("/usr/bin/chromium-browser");
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--headless");
        options.addArguments("excludeSwitches", "enable-automation");
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        System.out.println("crawling history data...");
        try {
            String link = "http://www.hisprice.cn/his.php?hisurl=" + shortUrl;
            driver.get(link);

            // 防止检测到 WebDriver
            ((JavascriptExecutor) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
            );

            // 等待页面加载
            Thread.sleep(5000);

            // 定位元素
            WebElement rNode = driver.findElement(By.xpath("//*[@id='container']"));

            // 滚动页面以显示元素
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", rNode);

            // 获取页面缩放比例
            Long devicePixelRatio = (Long) ((JavascriptExecutor) driver).executeScript("return window.devicePixelRatio;");
            System.out.println("网页模块尺寸: height=" + rNode.getSize().getHeight() + ", width=" + rNode.getSize().getWidth());

            // 截取整个页面截图
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            BufferedImage fullImage = ImageIO.read(screenshot);

            // 获取元素的坐标
            int left = (int) (rNode.getLocation().getX() * devicePixelRatio);
            int upper = (int) (((rNode.getLocation().getY() - ((Number) ((JavascriptExecutor) driver)
                    .executeScript("return window.pageYOffset;")).doubleValue()) * devicePixelRatio));
            int right = left + (int) (rNode.getSize().getWidth() * devicePixelRatio);
            int bottom = upper + (int) (rNode.getSize().getHeight() * devicePixelRatio);

            left = Math.max(0, left);
            upper = Math.max(0, upper);  // 修正负数坐标
            right = Math.min(fullImage.getWidth(), right);
            bottom = Math.min(fullImage.getHeight(), bottom);
            System.out.println(String.format("元素坐标：(%d, %d, %d, %d)", left, upper, right, bottom));

            // 裁剪图像
            BufferedImage croppedImage = fullImage.getSubimage(left, upper, right - left, bottom - upper);
            File outputfile = new File("history.png");
            ImageIO.write(croppedImage, "png", outputfile);

            // 移动图片到目标目录
            Path sourcePath = outputfile.toPath();
            Path targetPath = new File("frontend/public/" + outputfile.getName()).toPath();
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("图片已保存到：" + targetPath.toString());
        } catch (TimeoutException | InterruptedException e) {
            System.out.println("searchGoods: error");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            driver.quit();
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


    // 在 main 函数开始时连接数据库
    public static void getData(String url) {
        String formattedUrl = formatUrl(url);
        System.out.println(formattedUrl);
        searchGoods(formattedUrl);
        if (driver != null) {
            driver.quit();
        }
    }

    // 格式化 URL
    public static String formatUrl(String text) {
        if (text.startsWith("//")) {
            text = "https:" + text;
        }
        if (text.contains("priceTId")) {
            text = extractIdFromUrl(text);
        }
        System.out.println(text);

        // 创建需要进行 URL 转义的字符映射
        Map<Character, String> escapeDict = new HashMap<>();
        escapeDict.put('/', "%252F");
        escapeDict.put('?', "%253F");
        escapeDict.put('=', "%253D");
        escapeDict.put(':', "%253A");
        escapeDict.put('&', "%26");

        StringBuilder newString = new StringBuilder();
        for (char c : text.toCharArray()) {
            // 如果字符在转义字典中，进行转义
            if (escapeDict.containsKey(c)) {
                newString.append(escapeDict.get(c));
            } else {
                newString.append(c);
            }
        }

        System.out.println("new_string:" + newString.toString());
        return newString.toString();
    }

    // 从 URL 中提取 id 并构造新的 URL
    public static String extractIdFromUrl(String url) {
        // 使用正则表达式查找 id 参数
        Pattern pattern = Pattern.compile("id=(\\d+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String idValue = matcher.group(1);
            // 构造新的 URL
            if (url.contains("detail.tmall.com")) {
                return "https://detail.tmall.com/item.htm?id=" + idValue;
            } else {
                return "https://item.taobao.com/item.htm?id=" + idValue;
            }
        } else {
            return "ID not found in URL";
        }
    }
}