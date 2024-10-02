import json
from tb.crawler_tb import get_data as get_tb_data
from jd.crawler_jd import get_data as get_jd_data
import os
import sys

# 获取项目根目录
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
sys.path.append(project_root)
def merge_json(json1, json2):
    # 解析 JSON 字符串为 Python 对象
    list1 = json.loads(json1)
    list2 = json.loads(json2)
    # 合并两个列表
    combined_list = list1 + list2
    # 转换回 JSON 字符串
    return json.dumps(combined_list, ensure_ascii=False)

def main(search_term):
    # 获取第一个脚本的数据
    output1 = get_tb_data(search_term)

    # 获取第二个脚本的数据
    output2 = get_jd_data(search_term)

    # 合并两个 JSON 输出
    combined_json = merge_json(output1, output2)
    print(combined_json)

if len(sys.argv) != 2:
    print("Usage: python crawler_tb.py <query>")
    sys.exit(1)
query = sys.argv[1]
main(query)