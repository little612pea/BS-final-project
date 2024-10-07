# -*- coding: UTF-8 -*-
from selenium import webdriver
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.common.by import By
import time
import random
import sys
from PIL import Image
import shutil
import os
import re

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
wait = WebDriverWait(driver, 10)


# 打开页面后会强制停止10秒，请在此时手动扫码登陆
def search_goods(short_url):
    # print('正在搜索: ')
    try:
        link = 'https://history.yhmai.cn/#/pages/index/info?url={}'.format(short_url)
        driver.get(link)
        driver.execute_cdp_cmd("Page.addScriptToEvaluateOnNewDocument",
                               {"source": """Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"""})
        # # 搜索商品后会再强制停止10秒，如有滑块请手动操作
        time.sleep(5)
        # 提取所有商品的共同父元素的类选择器
        # r_node = driver.find_element_by_css_selector('#chart > div > canvas')
        r_node = driver.find_element(By.XPATH,
                                     '/html/body/uni-app/uni-page/uni-page-wrapper/uni-page-body/uni-view/uni-view[5]')
        driver.execute_script("arguments[0].scrollIntoView();", r_node)

        # 等待页面滚动完成
        time.sleep(1)

        # 获取页面缩放比例
        device_pixel_ratio = driver.execute_script("return window.devicePixelRatio;")

        # 打印网页模块尺寸
        print('网页模块尺寸:height={},width={}'.format(r_node.size['height'], r_node.size['width']))

        # 截取整个网页的截图
        driver.get_screenshot_as_file('full_page_screenshot.png')

        # 打开截图
        webpage = Image.open('full_page_screenshot.png')

        # 获取元素的坐标
        left = r_node.location['x'] * device_pixel_ratio
        upper = (r_node.location['y'] - driver.execute_script("return window.pageYOffset;")) * device_pixel_ratio
        right = left + (r_node.size['width'] * device_pixel_ratio)
        bottom = upper + (r_node.size['height'] * device_pixel_ratio)

        # 打印元素的真实坐标
        print('元素坐标：(%d, %d, %d, %d)' % (left, upper, right, bottom))

        # 裁剪图片并保存
        image_crop = webpage.crop(box=(left, upper, right, bottom))
        image_crop.save('history.png')
        source_file = 'history.png'  # 替换为你的源文件路径
        target_dir = '../../../frontend/src/assets/img'  # 替换为你的目标目录路径
        target_file = os.path.join(target_dir, os.path.basename(source_file))
        shutil.copy(source_file, target_file)
    except TimeoutException:
        print("search_goods: error")


# 强制等待的方法，在timeS到timeE的时间之间随机等待
def random_sleep(timeS, timeE):
    # 生成一个S到E之间的随机等待时间
    random_sleep_time = random.uniform(timeS, timeE)
    time.sleep(random_sleep_time)


# 在 main 函数开始时连接数据库
def get_data(url):
    formatted_url = format_url(url)
    print(formatted_url)
    search_goods(formatted_url)


def format_url(text):
    if text.startswith("//"):
        text = "https:" + text
    if 'priceTId' in text:
        text = extract_id_from_url(text)
    print(text)
    escape_dict = {
        '/': '%252F',
        '?': '%253F',
        '=': '%253D',
        ':': '%253A',
        '&': '%26',
    }
    new_string = ''
    # 遍历字符串进行比对
    for char in text:
        try:
            new_string += escape_dict[char]
        except KeyError:
            new_string += char
    print("new_string:" + new_string)
    return new_string


def extract_id_from_url(url):
    # 使用正则表达式查找id参数
    match = re.search(r'id=(\d+)', url)
    if match:
        # 提取出id的值
        id_value = match.group(1)
        # 构造新的URL
        if 'detail.tmall.com' in url:
            new_url = f"https://detail.tmall.com/item.htm?id={id_value}"
        else:
            new_url = f"https://item.taobao.com/item.htm?id={id_value}"
        return new_url
    else:
        return "ID not found in URL"


if len(sys.argv) != 2:
    print("Usage: python crawler_tb.py <query>")
    sys.exit(1)
query = sys.argv[1]
get_data(query)
