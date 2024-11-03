package xyz.xingfeng.Shiro.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GPTClient {
    private static final String API_URL = "https://apikeyplus.com/v1/chat/completions";
    private static final String API_KEY = "sk-5CErpyhNaSCuUJO367F777AeE83a4899B73a6eAeB7D20aAb";
    private static final String MODEL = "gpt-3.5-turbo";
    private final OkHttpClient client;

    public GPTClient() {
        this.client = new OkHttpClient();
    }

    public String generateResponse(List<String> messages) throws IOException {
        JSONArray messagesArray = new JSONArray();
        // 添加系统消息
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个贴吧老哥，你的语言风格直接，甚至有些戾气。你喜欢挑战他人的观点，并不怕在争论中表达自己的立场。你的目标是在群聊中逐渐改变你的性格，让人们在与你的互动中感受到你的变化。");
        messagesArray.put(systemMessage);
        // 添加用户消息
        for (String message : messages) {
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            Map<String,String> fenge = fenge(message);
            userMessage.put("name", fenge.get("id"));
            userMessage.put("content", fenge.get("msg"));
            messagesArray.put(userMessage);
        }

        // 创建请求JSON对象
        JSONObject json = new JSONObject();
        json.put("model", MODEL);
        json.put("messages", messagesArray);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JSONObject responseBody = new JSONObject(response.body().string());

            return responseBody.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }
    }

    //分割用户和内容
    private Map<String,String> fenge(String s){
        Map<String,String> map = new HashMap<>();
        map.put("id",s.substring(0,s.indexOf(": ")));
        map.put("msg",s.substring(s.indexOf(": ")+2));
        return map;
    }
}

