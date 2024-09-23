import json
from time import sleep
from selenium import webdriver

browser = webdriver.Firefox()
browser.maximize_window()
browser.implicitly_wait(2)

url = "https://www.jd.com"
browser.get(url)

# 登陆前
before_login = browser.get_cookies()

# 定位，点击“请登录”
browser.find_element_by_class_name("link-login").click()
sleep(5)
# 定位，点击“账户登录”
browser.find_element_by_link_text("账户登录").click()
sleep(5)
# 定位，输入账号
username = browser.find_element_by_id("loginname")
username.clear()
username.send_keys(input("用户名："))
# 定位，输入密码
password = browser.find_element_by_id("nloginpwd")
password.clear()
password.send_keys(input("密码："))
sleep(5)
# 定位，点击登录
browser.find_element_by_id("loginsubmit").click()

sleep(20)

# 登陆后
after_login = browser.get_cookies()

# 获取 cookies
cookies = browser.get_cookies()
# 将 cookies 写入文件
with open("cookies.txt", "w")  as f:
    json.dump(cookies, f)
browser.quit()