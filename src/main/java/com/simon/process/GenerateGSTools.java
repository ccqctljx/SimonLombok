package com.simon.process;

import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

/**
 * @Author 陈辰强
 * @Date 2020/2/27 21:20
 */
public class GenerateGSTools {

    public static final String GET_METHOD = "get";
    public static final String SET_METHOD = "set";


    /**
     * 根据类型生成方法
     * @param fieldName 字段名称
     * @param methodType 方法类型
     * @return
     */
    public static Name generateMethodNames(Name fieldName, String methodType, Names names) {
        return names.fromString(methodType + upperFirstLetter(fieldName.toString()));
    }

    /**
     * 将首字母大写
     * @param str
     * @return
     */
    private static String upperFirstLetter(String str) {
        char[] ch = str.toCharArray();
        if (ch[0] >= 'a' && ch[0] <= 'z') {
            ch[0] = (char) (ch[0] - 32);
        }
        return new String(ch);

    }

}
