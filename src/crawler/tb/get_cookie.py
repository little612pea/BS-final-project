import json
from time import sleep
from selenium import webdriver
from selenium.webdriver.common.by import By

options = webdriver.ChromeOptions()
# 关闭自动测试状态显示 // 会导致浏览器报：请停用开发者模式
options.add_experimental_option("excludeSwitches", ['enable-automation'])
options.add_argument('--disable-blink-features')

options.add_argument('--disable-blink-features=AutomationControlled')
# 去除浏览器selenium监控

options.add_argument('--disable-gpu')
# 禁用GPU加速

# 把chrome设为selenium驱动的浏览器代理；
browser = webdriver.Chrome(options=options)
browser.maximize_window()
browser.implicitly_wait(2)

url = "https://www.taobao.com"
browser.get(url)
sleep(40)
#
# # 登陆后
# after_login = browser.get_cookies()

# 获取 cookies
cookies = browser.get_cookies()
# 将 cookies 写入文件
with open("cookies_tb.txt", "w")  as f:
    json.dump(cookies, f)
browser.quit()