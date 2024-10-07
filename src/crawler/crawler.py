# -*- coding: UTF-8 -*-
import json
from tb.crawler_tb import get_data as get_tb_data
from jd.crawler_jd import get_data as get_jd_data
import os
import sys

# 获取项目根目录
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
sys.path.append(project_root)


def main(search_term):
    get_tb_data(search_term)
    get_jd_data(search_term)


if len(sys.argv) != 2:
    print("Usage: python crawler_tb.py <query>")
    sys.exit(1)
query = sys.argv[1]
main(query)
