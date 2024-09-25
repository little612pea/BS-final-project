import json
import time
from time import sleep
from selenium import webdriver

print("Begin testing...")
browser = webdriver.Chrome()
url = "https://www.jd.com"
browser.maximize_window()
browser.implicitly_wait(2)
# 访问网站，清空旧cookies信息
browser.get(url)
sleep(5)
browser.delete_all_cookies()

# 加载 cookies信息
with open("cookies.txt", "r") as f:
    cookies = json.load(f)
for cookie in cookies:
    browser.add_cookie(cookie)

# 验证是否登录成功
browser.get(url)
time.sleep(10)
browser.quit()
