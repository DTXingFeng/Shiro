package xyz.xingfeng.Shiro.chat;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class LLMRequestClient {
    private String url;
    private String apiKey;
    private JSONObject prompt;
    public LLMRequestClient(String url,String apiKey,JSONObject prompt){
        this.url = url;
        this.apiKey = apiKey;
        this.prompt = prompt;
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
        RequestBody body = RequestBody.create(mediaType,prompt.toString());
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
}
