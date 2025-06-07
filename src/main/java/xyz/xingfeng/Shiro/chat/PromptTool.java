package xyz.xingfeng.Shiro.chat;

import org.json.JSONArray;
import org.json.JSONObject;

public class PromptTool {
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";
    public static final String SYSTEM = "system";
    private boolean stream = true;
    private double temperature = 1.3;
    private int max_tokens = 8192;
    private String modelName = "Pro/deepseek-ai/DeepSeek-V3";
    private JSONArray messages = new JSONArray();
    public PromptTool(String modelName){
        this.modelName = modelName;
    }
    public PromptTool(String modelName, boolean stream, double temperature, int max_tokens) {
        this.modelName = modelName;
        this.stream = stream;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
    }

    public void addMessage(String role, String content) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("role", role);
        jsonObject.put("content", content);
        messages.put(jsonObject);
    }

    public JSONObject buildRequestMessage() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", modelName);
        jsonObject.put("stream", stream);
        jsonObject.put("temperature", temperature);
        jsonObject.put("max_tokens", max_tokens);
        jsonObject.put("messages", messages);
        return jsonObject;
    }
}
