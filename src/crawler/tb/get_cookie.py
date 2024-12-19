import json
from time import sleep
from selenium import webdriver

browser = webdriver.Chrome()
browser.maximize_window()
browser.implicitly_wait(2)
print("here")
url = "https://www.taobao.com"
browser.get(url)

sleep(30)

# 获取 cookies
cookies = browser.get_cookies()
# 将 cookies 写入文件
with open("cookies_tb.txt", "w") as f:
    json.dump(cookies, f)
browser.quit()
