package xyz.xingfeng.Shiro.service;


import org.json.JSONArray;
import org.json.JSONObject;
import xyz.xingfeng.Shiro.Tool.Static;
import xyz.xingfeng.Shiro.network.NetRequest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

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
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getPath(), e);
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
        JSONArray groupDatas = new JSONObject(sb.toString()).getJSONArray("msg");
        //构建提示词
        JSONObject system = new JSONObject();
        system.put("role","system");
        system.put("content",prompt);
        //构建模型请求
        model = new JSONObject();
        model.put("model",modelName);
        model.put("temperature",1.3);
        model.put("stream",false);
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
        HashMap<String, String> map = new HashMap<>();
        map.put("Authorization", "Bearer "+apiKey);
        String post = NetRequest.post(url, model.toString(), map);
        JSONObject jsonObject = new JSONObject(post);
        if (jsonObject.has("error")) {
            return jsonObject.get("error").toString();
        }
        String string = jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        return string;

    }

    public synchronized static void addMsg(Long groupId, String role, String name, String content){
// 保存聊天记录
            Path chatHistoryDir = Paths.get("ChatHistory");
            Path filePath = chatHistoryDir.resolve(groupId + ".json");

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
                msg.put("name", name);
                msg.put("content", content);

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
                    if (jsonArray.length() >= 100) {
                        int removeCount = jsonArray.length() - 99;
                        for (int i = 0; i < removeCount; i++) {
                            jsonArray.remove(0);
                        }
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

                // 写入文件
                try (FileWriter fileWriter = new FileWriter(file)) {
                    fileWriter.write(jsonObject.toString());
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
