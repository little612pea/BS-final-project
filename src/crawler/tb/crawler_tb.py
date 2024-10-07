# -*- coding: UTF-8 -*-
import jieba
from selenium import webdriver
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from pyquery import PyQuery as pq
import time
import random
import json
import sys

product_list = []

options = webdriver.ChromeOptions()
# 关闭自动测试状态显示 // 会导致浏览器报：请停用开发者模式
options.add_experimental_option("excludeSwitches", ['enable-automation'])
# options.add_argument('--headless=new')
options.add_argument("--window-position=-2400,-2400")
# 把chrome设为selenium驱动的浏览器代理；
driver = webdriver.Chrome(options=options)
# 窗口最大化
driver.maximize_window()
# wait是Selenium中的一个等待类，用于在特定条件满足之前等待一定的时间(这里是15秒)。
# 如果一直到等待时间都没满足则会捕获TimeoutException异常
wait = WebDriverWait(driver, 15)


# 打开页面后会强制停止10秒，请在此时手动扫码登陆
def search_goods(start_page, total_pages, keyword):
    # print('正在搜索: ')
    try:
        driver.get('https://www.taobao.com')
        driver.delete_all_cookies()
        # 加载 cookies信息
        with open("D:\\home\\BS\\BS-final-project\\src\\crawler\\tb\\cookies_tb.txt", "r") as f:
            cookies = json.load(f)
        for cookie in cookies:
            driver.add_cookie(cookie)
        driver.execute_cdp_cmd("Page.addScriptToEvaluateOnNewDocument",
                               {"source": """Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"""})
        # 找到搜索输入框
        input = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "#q")))
        # 找到“搜索”按钮
        submit = wait.until(
            EC.element_to_be_clickable((By.CSS_SELECTOR, '#J_TSearchForm > div.search-button > button')))
        input.send_keys(keyword)
        submit.click()
        # # 搜索商品后会再强制停止10秒，如有滑块请手动操作
        time.sleep(10)

        # 如果不是从第一页开始爬取，就滑动到底部输入页面然后跳转
        if start_page != 1:
            # 滑动到页面底端
            driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
            # 滑动到底部后停留1-3s
            random_sleep(1, 3)

            # 找到输入页面的表单
            pageInput = wait.until(EC.presence_of_element_located(
                (By.XPATH, '//*[@id="root"]/div/div[3]/div[1]/div[1]/div[2]/div[4]/div/div/span[3]/input')))
            pageInput.send_keys(start_page)
            # 找到页面跳转的确定按钮，并且点击
            admit = wait.until(EC.element_to_be_clickable(
                (By.XPATH, '//*[@id="root"]/div/div[3]/div[1]/div[1]/div[2]/div[4]/div/div/button[3]')))
            admit.click()
        get_goods()

        for i in range(start_page + 1, start_page + total_pages):
            page_turning(i)
    except TimeoutException:
        print("search_goods: error")


# 进行翻页处理
def page_turning(page_number):
    # 滑动到页面底端
    driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
    # 等待页面完全加载
    random_sleep(1, 3)
    try:
        # 找到“下一页”按钮并滚动到其可见区域
        next_button = wait.until(EC.presence_of_element_located(
            (By.XPATH, '//*[@id="sortBarWrap"]/div[1]/div[2]/div[2]/div[8]/div/button[2]')))
        driver.execute_script("arguments[0].scrollIntoView(true);", next_button)

        # 再次滑动以确保完全显示
        random_sleep(1, 2)

        driver.execute_script("arguments[0].click()", next_button)

        # 确认翻页是否成功，等待页码变为指定页数
        wait.until(EC.text_to_be_present_in_element(
            (By.XPATH, '//*[@id="sortBarWrap"]/div[1]/div[2]/div[2]/div[8]/div/span/em'), str(page_number)))
        get_goods()
    except TimeoutException:
        # 如果超时，则递归调用，重试翻页
        page_turning(page_number)


# 强制等待的方法，在timeS到timeE的时间之间随机等待
def random_sleep(timeS, timeE):
    # 生成一个S到E之间的随机等待时间
    random_sleep_time = random.uniform(timeS, timeE)
    time.sleep(random_sleep_time)


# 获取每一页的商品信息；
def get_goods():
    # 获取商品前固定等待2-4秒
    random_sleep(1, 3)
    sroll_cnt = 0
    while True:
        if sroll_cnt < 5:
            driver.execute_script('window.scrollBy(0, 1000)')
            time.sleep(0.2)
            sroll_cnt += 1
        else:
            break
    html = driver.page_source
    doc = pq(html)
    # print(doc)
    # 提取所有商品的共同父元素的类选择器
    items = doc('div.content--CUnfXXxv a.doubleCardWrapper--BpyYIb1O').items()
    for item in items:
        # 定位商品标题
        title = item.find('.title--F6pvp_RZ').text()
        href = item.attr('href')  # 直接访问 href 属性
        # 定位价格
        price_int = item.find('.priceInt--j47mhkXk').text()
        price_float = item.find('.priceFloat--zPTqSZZJ').text()
        if price_int and price_float:
            price = float(f"{price_int}{price_float}")
        else:
            price = 0.0
        # 定位交易量
        deal = item.find('.realSales--nOat6VGM').text()
        # 定位店名
        shop = item.find('.shopNameText--APRH8pWb').text()
        # 定位img_url
        img_url = item.find('.mainPic--CuSfUC4j').attr('src')
        if img_url == None:
            img_url = item.find('.mainPic--ZzRJ1jkn').attr('src')
        # 构建商品信息字典
        product = {
            'title': title,
            'price': price,
            'deal': deal,
            'shop': shop,
            'img_url': img_url,
            'source': href
        }
        product_list.append(product)


# 在 main 函数开始时连接数据库
def get_data(keyword):
    pageStart = 1
    pageAll = 2
    # 使用jieba进行分词
    segmented = jieba.cut(keyword)
    segmented_list = list(segmented)
    combined_string = ' '.join(segmented_list)  # 这里用空格作为分隔符
    search_goods(pageStart, pageAll, combined_string)
    print(json.dumps(product_list, ensure_ascii=False))


if len(sys.argv) != 2:
    print("Usage: python crawler_tb.py <query>")
    sys.exit(1)
query = sys.argv[1]
get_data(query)
