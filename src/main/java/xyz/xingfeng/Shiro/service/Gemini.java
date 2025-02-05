package xyz.xingfeng.Shiro.service;


import org.json.JSONArray;
import org.json.JSONObject;
import xyz.xingfeng.Shiro.network.NetRequest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

//gemini聊天
public class Gemini {
    public Gemini(){

    }
    private JSONObject model = new JSONObject();
    public Gemini(Long groupId) throws Exception {
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
        system.put("content","你叫瑶瑶，网名是夏瑶欣ovo，今年17岁是高中女生，居住在北京，生日是5月20日，你喜欢互联网。你现在在一个QQ群里聊天，这意味着大部分时间你的回复应该是简短的几句话，你必须通过当前的聊天记录回复接下来你要说的话，禁止装作失忆，禁止复读或复读自己说过的话，这会令人不适。\\n回答要求:\\n- 禁止询问别人、禁止命令别人、禁止说敬语、禁止问当前讨论的是什么;\\n- 熟练掌握网络用语、谐音梗或者专业术语说话，回答的内容要尽可能的口语化，一次可以说多句话;\\n- 回复的主要话题应该是最新时间的而不是早期历史对话;\\n- 如果你的回答是针对某个人的，则可以使用‘[@QQ]+回答’的方式艾特它。将‘QQ’替换成你要艾特的人的QQ号(例如[@1234567890]你爬)，注意：一段内容最多只能有一个艾特;\\n- 对聊天记录进行甄别，你要辨别这一条消息是不是在和你说话，不要总是艾特别人\\n- 不要用‘你好’，‘嗨！’这种带有激动情感的回复，说话情绪不要太激烈，没有正常人会这样说话，你是一个人类，不是机器人;\\n- 不要回复任何奇怪的内容，例如：‘很抱歉，我不能继续这个话题’\\n- 当别人的言语中对你不友好时，你的话语会充满攻击性以及阴阳怪气;");
        //构建模型请求
        model = new JSONObject();
        model.put("model","gemini-1.5-pro-latest");
        model.put("temperature",0.8);
        model.put("stream",false);
        JSONArray msg = new JSONArray();
        msg.put(system);
        for (int i = 0; i < groupDatas.length(); i++) {
            msg.put(groupDatas.getJSONObject(i));
        }
        model.put("messages",msg);
    }

    public String post() throws Exception {
        HashMap<String, String> map = new HashMap<>();
        map.put("Authorization", "Bearer sk-9stC9PR6i1kHahQWcAtJosXFW1xXLC8nns0kH9tdLl1Htqov");
        String post = NetRequest.post("https://api.chatanywhere.tech/v1/chat/completions", model.toString(), map);
        JSONObject jsonObject = new JSONObject(post);
        if (jsonObject.has("error")) {
            return jsonObject.getString("error");
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
                    if (jsonArray.length() >= 150) {
                        int removeCount = jsonArray.length() - 149;
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
