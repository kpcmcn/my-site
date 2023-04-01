package cn.luischen.utils;

/**
 * @program: official-account
 * @description: CheckUtil工具类
 * @author: xmonster_大魔王
 * @create: 2022-09-13 22:06
 **/

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 校验的工具类   微信使用
 */
@Component
public class CheckUtil {

    private static final String token = "kpcmcn"; //这个token值要和服务器配置一致

    public static boolean checkSignature(String signature, String timestamp, String nonce) {

        String[] arr = new String[]{token, timestamp, nonce};
        // 排序
        Arrays.sort(arr);
        // 生成字符串
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            content.append(arr[i]);
        }

        // sha1加密
        String temp = getSHA1String(content.toString());

        return temp.equals(signature); // 与微信传递过来的签名进行比较
    }

    private static String getSHA1String(String data) {
        // 使用commons codec生成sha1字符串
        return DigestUtils.shaHex(data);
    }
}