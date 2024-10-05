import requests
# 请求示例 url 默认请求参数已经做URL编码
url = "https://api-vx.Taobaoapi2014.cn/taobao/item_cat_get/?key=<您自己的apiKey>&secret=<您自己的apiSecret>&num_iid=520813250866"
headers = {
    "Accept-Encoding": "gzip",
    "Connection": "close"
}
if __name__ == "__main__":
    r = requests.get(url, headers=headers)
    json_obj = r.json()
    print(json_obj)