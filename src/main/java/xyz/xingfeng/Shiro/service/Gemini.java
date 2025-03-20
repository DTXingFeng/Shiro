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
import java.util.*;
import java.util.concurrent.TimeUnit;

//gemini聊天
public class Gemini {
    private String apiKey;
    private String url;
    private String modelName;
    private String prompt;
    private JSONObject model;

    public Gemini() {
        initialization();
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


    

    private void keywordDetection(JSONArray jsonArray){
        //从知识库中获得关键词
        File file = Paths.get("aiConfig").resolve("梗解析.json").toFile();
        if (!file.exists()){
            return;
        }
        //获得关键词
        // 读取关键词
        JSONArray data = readJsonFile(file).getJSONArray("data");
        Set<String> keywords = new HashSet<>(); // 使用Set来避免重复关键词
        Map<String, JSONObject> keywordToDataMap = new HashMap<>(); // 关键词到对应数据的映射

        for (int i = 0; i < data.length(); i++) {
            JSONObject entry = data.getJSONObject(i);
            JSONArray 关键词Array = entry.getJSONArray("关键词");
            for (int j = 0; j < 关键词Array.length(); j++) {
                String keyword = 关键词Array.getString(j);
                if (keywords.contains(keyword)) {
                    System.out.println("重复关键词: " + keyword);
                } else {
                    keywords.add(keyword);
                    keywordToDataMap.put(keyword, entry); // 将关键词与对应的数据关联
                }
            }
        }

        // 提取半数消息的内容
        for (int i = jsonArray.length() / 2; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (!jsonObject.getString("role").equals("user")){
                continue;
            }
            String content = new JSONObject(jsonObject.getString("content")).getString("content");
            for (String keyword : keywords) {
                if (content.contains(keyword)) {
                    JSONObject matchedData = keywordToDataMap.get(keyword);
                    JSONArray 关键词Array = matchedData.getJSONArray("关键词");
                    String 解析 = matchedData.getString("解析");
                    JSONArray 接梗公式 = matchedData.getJSONArray("接梗公式");

                    // 构建输出
                    StringBuilder output = new StringBuilder();
                    output.append("\n关键词：\n");
                    for (int j = 0; j < 关键词Array.length(); j++) {
                        output.append(关键词Array.getString(j)).append("\n");
                    }
                    output.append("分析：\n").append(解析).append("\n");
                    output.append("接梗公式：\n");
                    for (int j = 0; j < 接梗公式.length(); j++) {
                        output.append(接梗公式.getString(j)).append("\n");
                    }

                    // 将结果添加到prompt中（假设prompt是一个全局变量）
                    prompt += output.toString();
                }
            }
        }
    }


    /**
     * 群聊模式
     * @param groupId 群号
     */
    public Gemini group(String groupId) throws Exception{
        Path chatHistoryDir = Paths.get("ChatHistory\\group");
        Path filePath = chatHistoryDir.resolve(groupId + ".json");
        try {
            load(filePath);
        }catch (Exception e){
            e.printStackTrace();
        }
        return this;
    }

    /**
     * 私聊模式
     * @param userId 用户QQ
     */
    public Gemini privateChat(String userId){
        Path chatHistoryDir = Paths.get("ChatHistory\\private");
        Path filePath = chatHistoryDir.resolve(userId + ".json");
        try {
            load(filePath);
        }catch (Exception e){
            e.printStackTrace();
        }
        return this;
    }

    /**
     * 加载数据
     */
    public void load(Path filePath) throws Exception {
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
        //检查历史记录中是否有关键词
        keywordDetection(groupDatas);
        //构建提示词
        JSONObject system = new JSONObject();
        system.put("role","system");
        system.put("content",prompt);
        //构建模型请求
        model = new JSONObject();
        model.put("model",modelName);
        model.put("temperature",1.3);
        model.put("stream",true);
        //将历史记录导入
        JSONArray msg = new JSONArray();
        msg.put(system);
        for (int i = 0; i < groupDatas.length(); i++) {
            msg.put(groupDatas.getJSONObject(i));
        }
        model.put("messages",msg);
    }

    /**
     * 初始化
     */
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

    /**
     * 发送请求
     * @return
     * @throws Exception
     */
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
            Path chatHistoryDir = Paths.get("ChatHistory\\group");
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
