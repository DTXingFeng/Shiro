package xyz.xingfeng.Shiro.service;


import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import okhttp3.*;
import org.apache.juli.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.xingfeng.Shiro.Tool.Static;
import xyz.xingfeng.Shiro.network.NetRequest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

//gemini聊天
public class Gemini {
    private String apiKey;
    private String url;
    private String modelName;
    private String prompt;

    public Gemini() {

    }

    private JSONObject readJsonFile(File file) {
        String content = readFileToString(file);
        return new JSONObject(content);
    }

    private String readFileToString(File file) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getPath(), e);
        }finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return sb.toString();
    }

    private JSONObject model = new JSONObject();
    public Gemini(Long groupId) throws Exception {
        initialization();
        Path chatHistoryDir = Paths.get("ChatHistory");
        Path filePath = chatHistoryDir.resolve(groupId + ".json");
        File f = filePath.toFile();
        // 确保文件存在
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file: " + filePath, e);
            }
        }
        BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
        StringBuilder sb = new StringBuilder();
        String temp = "";
        while ((temp = bufferedReader.readLine())!=null){
            sb.append(temp);
        }
        bufferedReader.close();
        JSONArray groupDatas = new JSONObject(sb.toString()).getJSONArray("msg");
        //构建提示词
        JSONObject system = new JSONObject();
        system.put("role","system");
        system.put("content",prompt);
        //构建模型请求
        model = new JSONObject();
        model.put("model",modelName);
        model.put("temperature",1.3);
        model.put("stream",true);
        //构建bot自己的信息，让bot认识自己
//        JSONObject self_msg = new JSONObject();
//        self_msg.put("self_qq","391459725");
//        self_msg.put("self_role","member");
//        model.put("context",self_msg);
        //将历史记录导入
        JSONArray msg = new JSONArray();
        msg.put(system);
        for (int i = 0; i < groupDatas.length(); i++) {
            msg.put(groupDatas.getJSONObject(i));
        }
        model.put("messages",msg);
    }

    private void initialization(){
        Path aiConfigDir = Paths.get("aiConfig");

        // 读取主配置文件
        JSONObject configJson = readJsonFile(new File(Static.CONFIG_PATH));
        String aiConfigFile = configJson.getString("aiConfigFile");

        // 读取AI配置文件
        File aiConfigDav = aiConfigDir.resolve(aiConfigFile + ".json").toFile();
        JSONObject aiConfigJson = readJsonFile(aiConfigDav);
        this.apiKey = aiConfigJson.getString("apiKey");
        this.url = aiConfigJson.getString("url");
        this.modelName = aiConfigJson.getString("modelName");

        // 读取提示文件
        File promptFile = aiConfigDir.resolve("prompt").toFile();
        this.prompt = readFileToString(promptFile);
    }

    public String post() throws Exception {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType,model.toString());
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Authorization", "Bearer "+apiKey)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            processLine(line);
        }
        if (fullResponse.toString().equals("")){
            System.out.println(fullResponse);
        }
        return fullResponse.toString();
    }

    private StringBuilder fullResponse = new StringBuilder();
    private void processLine(String line) {
        if (line.startsWith("data:")) {
            String jsonData = line.substring(5).trim();

            if ("[DONE]".equals(jsonData)) {
                System.out.println("\nStream completed");
                return;
            }

            try {
                JSONObject json = new JSONObject(jsonData);
                JSONArray choices = json.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject delta = choice.getJSONObject("delta");

                    // 提取内容片段
                    if (delta.has("content")) {
                        String content = delta.getString("content");
                        fullResponse.append(content);
                    }

                    // 检查是否结束
                    if (choice.has("finish_reason") && !choice.isNull("finish_reason")) {
                        String finishReason = choice.getString("finish_reason");
                        if ("stop".equals(finishReason)) {
                            System.out.println("\nFinal response: " + fullResponse);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized static void addMsg(GroupMessageEvent event,String role,boolean b){
            // 保存聊天记录
            Path chatHistoryDir = Paths.get("ChatHistory");
            Path filePath = chatHistoryDir.resolve(event.getGroupId() + ".json");

            // 确保目录存在
            try {
                Files.createDirectories(chatHistoryDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory: " + chatHistoryDir, e);
            }

            // 确保文件存在
            File file = filePath.toFile();
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create file: " + filePath, e);
                }
            }

            try {
                // 构建准备存进去的json
                JSONObject msg = new JSONObject();
                msg.put("role", role);
                msg.put("name", event.getUserId().toString());
                if (b){
                    JSONObject content = new JSONObject();
                    content.put("qq",event.getUserId());
                    content.put("content",event.getMessage());
                    content.put("name",event.getSender().getNickname());
                    //附加消息
                    JSONObject metadata = new JSONObject();
                    metadata.put("status",event.getSender().getRole());
                    metadata.put("msgId",event.getMessageId());
                    //获得现在的时间
                    LocalDateTime now = LocalDateTime.now();
                    // 自定义格式
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String formattedTime = now.format(formatter);
                    metadata.put("time",formattedTime);
                    content.put("metadata",metadata);
                    msg.put("content", content.toString());
                }else {
                    msg.put("content", event.getMessage());
                }
                // 读取现有内容
                StringBuilder stringBuilder = new StringBuilder();
                if (file.length() > 0) { // 检查文件是否为空
                    try (BufferedReader bufferedReader = Files.newBufferedReader(filePath)) {
                        String temp;
                        while ((temp = bufferedReader.readLine()) != null) {
                            stringBuilder.append(temp).append("\n");
                        }
                    }
                }

                // 解析现有内容
                JSONObject jsonObject;
                if (stringBuilder.length() > 0) {
                    jsonObject = new JSONObject(stringBuilder.toString());
                    JSONArray jsonArray = jsonObject.getJSONArray("msg");
                    while (jsonArray.toString().length() * 1.5 > 10000){
                        jsonArray.remove(0);
                    }
                    jsonArray.put(msg);
                    jsonObject.put("msg", jsonArray);
                } else {
                    // 如果文件为空，创建新的JSON对象
                    jsonObject = new JSONObject();
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(msg);
                    jsonObject.put("msg", jsonArray);
                }
                FileWriter fileWriter = null;
                // 写入文件
                try {
                    fileWriter = new FileWriter(file);
                    fileWriter.write(jsonObject.toString());
                    fileWriter.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } catch (FileNotFoundException e) {
                throw new RuntimeException("File not found: " + filePath, e);
            } catch (IOException e) {
                File f = filePath.toFile();
                if (f.exists()) {
                    f.delete();
                }
                throw new RuntimeException("IO error: " + filePath, e);
            }
    }
}
