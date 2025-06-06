package xyz.xingfeng.Shiro.service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.xingfeng.Shiro.network.NetRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
                BufferedImage bufferedImage = imagePixie(response.body().byteStream());// 确保图片可以被读取
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "PNG", baos);
                byte[] imageBytes = baos.toByteArray();
                base64Image = Base64.getEncoder().encodeToString(imageBytes);
            } else {
                System.out.println("Request failed: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return base64Image;
    }
    //处理图片像素点
    private BufferedImage imagePixie(InputStream is) throws IOException {
        BufferedImage read = ImageIO.read(is);
        int height = read.getHeight();
        int width = read.getWidth();
        read.setRGB(width-1,height-1,0);
        return read;
    }

}
