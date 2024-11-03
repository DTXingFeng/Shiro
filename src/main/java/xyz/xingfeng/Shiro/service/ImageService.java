package xyz.xingfeng.Shiro.service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.xingfeng.Shiro.network.NetRequest;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;

public class ImageService {
    public String URL = "https://api.lolicon.app/setu/v2?r18=2&excludeAI=true";
    public ImageService() {
    }

    public ImageService(String group) {
        URL = URL + "&tag=" + group;
    }

    //随机图片
    public List<String> getImageUrl() throws Exception {
        List<String> urls = new ArrayList<>();
        String s = new NetRequest().get(URL);
        JSONObject jsonObject = new JSONObject(s);
        if (!jsonObject.getString("error").equals("")){
            throw new Exception("请求api异常:"+jsonObject.toString(1));
        }
        JSONArray data = jsonObject.getJSONArray("data");
        if (data.length() == 0){
            throw new Exception("没有找到图片");
        }
        for (int i =0; i < data.length(); i++){
            JSONObject jsonObject1 = data.getJSONObject(i);
            String string = jsonObject1.getJSONObject("urls").getString("original");
            urls.add(string);
        }
        return urls;
    }
    public String md5(String text){
        StringBuilder hexString = new StringBuilder();
        try {
            // 创建MessageDigest实例，指定使用MD5算法
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 将输入字符串转换为字节数组并更新MessageDigest
            md.update(text.getBytes());

            // 执行哈希计算
            byte[] digest = md.digest();

            // 将字节数组转换为16进制字符串
            hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // 输出MD5哈希值
            System.out.println("MD5 hash: " + hexString.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexString.toString();
    }

    public List<String> aliGetImageUrl() throws Exception {
        List<String> bases = new ArrayList<>();
        String s = md5("xingfeng" + LocalDate.now().toString());
        Map<String,String> map = new HashMap<>();
        map.put("tokens",s);
        String s1 = new NetRequest().get("http://8.219.139.122:8800/pixiv",null,map);
        JSONObject json = new JSONObject(s1);
        if (json.getInt("code")!=200){
            throw new Exception("请求错误");
        }
        JSONArray data = json.getJSONArray("data");
        for (int i = 0; i < data.length(); i++){
            bases.add("base64://"+data.getString(i));
        }
        return bases;
    }
    /**
     * 从反向代理获得图片
     */
    public List<String> getImageBase64() throws Exception {
        List<String> bases = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        String s = NetRequest.get(URL);
        JSONObject jsonObject = new JSONObject(s);
        if (!jsonObject.getString("error").isEmpty()){
            throw new Exception("请求api异常:"+jsonObject.toString(1));
        }
        JSONArray data = jsonObject.getJSONArray("data");
        for (int i =0; i < data.length(); i++){
            JSONObject jsonObject1 = data.getJSONObject(i);
            String string = jsonObject1.getJSONObject("urls").getString("original");
            string = string.replaceAll("https://i.pixiv.re/","http://pixiv.xingfeng.xyz/");
            urls.add(string);
        }
        for (String url : urls){
            String base64 = ImageToBase64(url);
            bases.add("base64://"+base64);
        }
        return bases;
    }


    public String ImageToBase64(String url){
        OkHttpClient client = new OkHttpClient();

        String base64Image = "";
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                byte[] imageBytes = response.body().bytes();
                base64Image = Base64.getEncoder().encodeToString(imageBytes);
            } else {
                System.out.println("Request failed: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return base64Image;
    }


}
