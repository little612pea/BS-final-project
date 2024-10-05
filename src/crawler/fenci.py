import jieba

# 商品名称示例
product_name = "羽毛球拍李宁超轻碳素正品成人男女单拍超轻超硬羽毛球拍专业级A+级"

# 使用jieba进行分词
segmented = jieba.cut(product_name)
segmented_list = list(segmented)
combined_string = ' '.join(segmented_list)  # 这里用空格作为分隔符
print(combined_string)
