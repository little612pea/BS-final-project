import json
import time
from time import sleep
from selenium import webdriver
import pymysql
from selenium import webdriver
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from pyquery import PyQuery as pq
import time
import random
from urllib.parse import urlparse, urlencode, parse_qs, urlunparse
import re
import sys

product_list=[]
MYSQL_TABLE = 'product'

# MySQL 数据库连接配置
db_config = {
    # 'host': 'host.docker.internal',
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': 'qwerty',
    'database': 'library',
    'charset': 'utf8mb4',
}

# 创建 MySQL 连接对象
conn = pymysql.connect(**db_config)
cursor = conn.cursor()

options = webdriver.ChromeOptions()
# 关闭自动测试状态显示 // 会导致浏览器报：请停用开发者模式
options.add_experimental_option("excludeSwitches", ['enable-automation'])

# 把chrome设为selenium驱动的浏览器代理；
driver = webdriver.Chrome(options=options)
# 窗口最大化
driver.maximize_window()

# wait是Selenium中的一个等待类，用于在特定条件满足之前等待一定的时间(这里是15秒)。
# 如果一直到等待时间都没满足则会捕获TimeoutException异常
wait = WebDriverWait(driver, 15)


# 打开页面后会强制停止10秒，请在此时手动扫码登陆
def search_goods(start_page, total_pages,keyword):
    # print('正在搜索: ')
    try:
        driver.get('https://www.jd.com/')
        # 强制停止10秒，请在此时手动扫码登陆
        driver.delete_all_cookies()
        # 加载 cookies信息
        with open("D:\\home\\BS\\BS-final-project\\src\\crawler\\jd\\cookies_jd.txt", "r") as f:
            cookies = json.load(f)
        for cookie in cookies:
            driver.add_cookie(cookie)
        # 验证是否登录成功
        driver.execute_cdp_cmd("Page.addScriptToEvaluateOnNewDocument",
                               {"source": """Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"""})
        # 找到搜索输入框
        input = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "#key")))
        # 找到“搜索”按钮
        submit = wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, '#search > div > div.form > button')))
        input.send_keys(keyword)
        submit.click()
        # 搜索商品后会再强制停止10秒，如有滑块请手动操作
        time.sleep(10)

        # 如果不是从第一页开始爬取，就滑动到底部输入页面然后跳转
        if start_page != 1:
            # 滑动到页面底端
            driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
            # 滑动到底部后停留1-3s
            random_sleep(1, 3)

            # 找到输入页面的表单
            pageInput = wait.until(EC.presence_of_element_located(
                (By.XPATH, '/html/body/div[5]/div[2]/div[2]/div[1]/div/div[3]/div/span[2]/input')))
            pageInput.send_keys(start_page)
            # 找到页面跳转的确定按钮，并且点击
            admit = wait.until(EC.element_to_be_clickable(
                (By.XPATH, '/html/body/div[5]/div[2]/div[2]/div[1]/div/div[3]/div/span[2]/a')))
            admit.click()
        get_goods()
        for i in range(start_page + 1, start_page + total_pages):
            page_turning(i)
    except TimeoutException:
        print("search_goods: error")


# 进行翻页处理
def page_turning(page_number):
    # print('正在翻页: ', page_number)
    try:
        current_url = driver.current_url
        # print(current_url)
        parsed_url = urlparse(current_url)
        query_params = parse_qs(parsed_url.query)

        # 检查 'page' 和 's' 参数是否存在，并进行转换和更新
        if 'page' in query_params:
            try:
                new_page = int(query_params['page'][0]) + 2
            except ValueError:
                new_page = 1
            query_params['page'] = str(new_page)
        else:
            query_params['page'] = '1'  # 如果参数不存在，设置默认值

        if 's' in query_params:
            try:
                new_s = int(query_params['s'][0]) + 60
            except ValueError:
                new_s = 1
            query_params['s'] = str(new_s)
        else:
            query_params['s'] = '1'  # 如果参数不存在，设置默认值

        new_query = urlencode(query_params, doseq=True)
        new_url = urlunparse(parsed_url._replace(query=new_query))
        # print(new_url)

        # 尝试导航到新的 URL
        try:
            driver.get(new_url)
        except Exception as e:
            print(f"Error navigating to {new_url}: {e}")
        get_goods()
    except TimeoutException:
        page_turning(page_number)


# 强制等待的方法，在timeS到timeE的时间之间随机等待
def random_sleep(timeS, timeE):
    # 生成一个S到E之间的随机等待时间
    random_sleep_time = random.uniform(timeS, timeE)
    time.sleep(random_sleep_time)


# 获取每一页的商品信息；
def get_goods():
    # 获取商品前固定等待2-4秒
    random_sleep(2, 4)
    html = driver.page_source
    doc = pq(html)
    # print(doc)
    # 提取所有商品的共同父元素的类选择器
    items = doc('.gl-item').items()
    for item in items:
        # 定位商品标题
        title = item.find('.p-name.p-name-type-2').text()
        # 定位价格
        price = item.find('.p-price').text()
        # 定位评论数
        comment = item.find('.p-commit').text()
        # 定位店名
        shop = item.find('.p-shop').text()
        # 定位图片url
        img = item.find('.p-img').html()
        pattern = r'data-lazy-img="(.*?)" source-data-lazy-img=""'
        img_url = re.findall(pattern, img)
        if img_url[0] == "done":
            pattern = r'source-data-lazy-img="" src="(.*?)"/>'
            img_url = re.findall(pattern, img)
        # 构建商品信息字典
        product = {
            'title': title,
            'price': price,
            'comment': comment,
            'shop': shop,
            'img_url': img_url,
            'source': 1
        }

        product_list.append(product)
        # print(product)
        save_to_mysql(product)


# 在 save_to_mysql 函数中保存数据到 MySQL
def save_to_mysql(result):
    # try:
    sql = "INSERT INTO {} (title,price,comment,shop,img_url,source) VALUES (%s, %s, %s, %s, %s, %s)".format(MYSQL_TABLE)
    # print("sql语句为:  " + sql)
    cursor.execute(sql, (result['title'], result['price'], result['comment'], result['shop'], result['img_url'], result['source']))
    conn.commit()
    # print('存储到MySQL成功: ', result)
    # except Exception as e:
    #     print('存储到MYsql出错: ', result, e)

def get_data(keyword):
    pageStart = 1
    pageAll = 5
    search_goods(pageStart, pageAll, keyword)
    # cursor.close()
    # conn.close()
    return json.dumps(product_list, ensure_ascii=False)

if __name__ == '__main__':
    get_data("手机")