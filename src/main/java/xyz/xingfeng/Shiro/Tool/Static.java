package xyz.xingfeng.Shiro.Tool;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class Static {
    public static int date = 0;
    /**
     * 配置文件路径
     */
    public static final String CONFIG_PATH = "config.json";
    /**
     * 将文件内的内容转为json对象
     */
    public static JSONObject getJson() throws Exception {
        File file = Paths.get(CONFIG_PATH).toFile();
        if (!file.exists()) {
            return new JSONObject();
        }
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String temp = "";
        StringBuilder sb = new StringBuilder();
        while ((temp = bufferedReader.readLine()) != null) {
            sb.append(temp);
        }
        return new JSONObject(sb.toString());
    }

}
