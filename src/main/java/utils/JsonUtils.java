package utils;

import java.util.function.Function;

public class JsonUtils {

    /**
     * 提取指定键对应的值并根据提供的转换器转换为指定类型。
     *
     * @param input    完整的输入字符串
     * @param key      需要提取的键
     * @param converter 值转换的函数（例如：Integer::parseInt, Double::parseDouble）
     * @param <T>      返回类型
     * @return 转换后的值
     */
    public static <T> T extractValue(String input, String key, Function<String, T> converter) {
        int keyStartIndex = input.indexOf(key) + key.length() + 2; // 定位键的起始位置，假设键后有 `: ` 格式
        int keyEndIndex = input.indexOf(",", keyStartIndex); // 定位下一个逗号的位置
        if (keyEndIndex == -1) { // 如果逗号不存在，取到末尾
            keyEndIndex = input.length() - 1; // 排除可能的 `}`
        }
        if(keyEndIndex == keyStartIndex) {
            keyEndIndex++;
        }
        String rawValue = input.substring(keyStartIndex, keyEndIndex).trim();
        while (rawValue.endsWith("}")) {
            rawValue = rawValue.substring(0, rawValue.length() - 1).trim();
        }
        if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) { // 移除可能的引号
            rawValue = rawValue.substring(1, rawValue.length() - 1);
        }
        if(rawValue.endsWith("\"")) {
            rawValue = rawValue.substring(0, rawValue.length() - 1);
        }
        return converter.apply(rawValue); // 使用转换器进行类型转换
    }
}
