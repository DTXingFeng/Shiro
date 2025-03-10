package xyz.xingfeng.Shiro.Tool;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图像解析，用于分析表情包以及一些图片
 */
public class ImageAnalysis {

    private static String fileName = "";
    private String subType = "";
    private String imageUrl;

    public ImageAnalysis(String cqImage) {
        //获取文件名
        String pattern = "file=(.*?),";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(cqImage);
        if (m.find()) {
            fileName = m.group(1);
        }
        //获取类型
        pattern = "subType=(.*?),";
        r = Pattern.compile(pattern);
        m = r.matcher(cqImage);
        if (m.find()) {
            subType = m.group(1);
        }
        //获取url
        pattern = "\\[CQ:image.*?url=([^,]+).*?\\]";
        r = Pattern.compile(pattern);
        m = r.matcher(cqImage);
        if (m.find()) {
            imageUrl = m.group(1);
            imageUrl = imageUrl.replaceAll("&amp;", "&");
        }
    }

    /**
     * 通过视觉模型分析表情包/图片
     */
    public void modelAnalysis() {


        OkHttpClient client = new OkHttpClient().newBuilder()
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType,buildRequestMessage());

    }

    /**
     * 构建请求消息
     */
    private String buildRequestMessage(){
        //构建消息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model","deepseek-ai/deepseek-vl2");
        jsonObject.put("stream",false);
        jsonObject.put("max_tokens",2048);
        //创建消息
        JSONArray msgs = new JSONArray();
        msgs.put(new JSONObject("{\n" +
                "      \"role\": \"system\",\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"text\": \"[系统指令] 作为表情包解析器，请按以下格式输出： <视觉要素> 主体对象：{对象描述} 动作表情：{动作/表情分析} 文字内容：{OCR文本} </视觉要素>  <情感分析> 强度：0-1 类型：幽默/嘲讽/惊讶/愤怒... </情感分析>\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }"));
        //创建用户消息
        JSONObject user = new JSONObject();
        user.put("role","user");
        JSONArray content = new JSONArray();
        JSONObject image_url = new JSONObject();
        image_url.put("detail","auto");
        image_url.put("url",fileToBase64());
        content.put(new JSONObject().put("image_url",image_url).put("type","image_url"));
        user.put("content",content);
        //完成
        msgs.put(user);
        jsonObject.put("messages",msgs);
        return jsonObject.toString();
    }

    /**
     * 将文件转为base64
     */
    public static String fileToBase64() {
        String base64String;
        Base64.Encoder encoder = Base64.getEncoder();
        Path path = Paths.get("memes\\image", fileName);

        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            // 读取文件内容并编码为 Base64
            byte[] fileBytes = fis.readAllBytes();
            base64String = encoder.encodeToString(fileBytes);

            // 获取文件的 MIME 类型
            String mimeType = getMimeType();

            // 添加前缀
            base64String = "data:" + mimeType + ";base64," + base64String;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read or encode file: " + fileName, e);
        }

        return base64String;
    }

    /**
     * 根据文件扩展名获取 MIME 类型
     */
    private static String getMimeType() {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream"; // 默认的 MIME 类型
        }
    }
}
