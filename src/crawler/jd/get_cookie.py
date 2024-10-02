import json
from time import sleep
from selenium import webdriver
from selenium.webdriver.common.by import By

browser = webdriver.Chrome()
browser.maximize_window()
browser.implicitly_wait(2)

url = "https://www.taobao.com"
browser.get(url)

# # 登陆前
# before_login = browser.get_cookies()
#
# # 定位，点击“请登录”
# browser.find_element(By.CLASS_NAME,"link-login").click()
# sleep(5)
# # # 定位，点击“账户登录”
# # browser.find_element(By.LINK_TEXT,"账户登录").click()
# # sleep(5)
# # 定位，输入账号
# username = browser.find_element(By.ID,"loginname")
# username.clear()
# username.send_keys(input("用户名："))
# # 定位，输入密码
# password = browser.find_element(By.ID,"nloginpwd")
# password.clear()
# password.send_keys(input("密码："))
# sleep(5)
# # 定位，点击登录
# browser.find_element(By.ID,"loginsubmit").click()
#
sleep(30)
#
# # 登陆后
# after_login = browser.get_cookies()

# 获取 cookies
cookies = browser.get_cookies()
# 将 cookies 写入文件
with open("cookies_tb.txt", "w")  as f:
    json.dump(cookies, f)
browser.quit()