package xyz.xingfeng.Shiro.chat;

import org.json.JSONObject;
import xyz.xingfeng.Shiro.Tool.Static;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LLMConfig {
    private String url;
    private String apiKey;
    private String modelName;
    public static final String NANO = "nano";
    public static final String MAIN = "main";
    private String nanoAi = "";
    private String mainAi = "";
    public LLMConfig(){
        JSONObject json = Static.getJson();
        nanoAi = json.getString("nanoAiUrl");
        mainAi = json.getString("aiConfigFile");
    }

    public void LoadConfig(String type){
        Path aiConfigDir = Paths.get("aiConfig");
        File file = null;
        if (type.equals(NANO)){
            file = aiConfigDir.resolve(nanoAi + ".json").toFile();
        }else if (type.equals(MAIN)){
            file = aiConfigDir.resolve(mainAi+ ".json").toFile();
        }else {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
        if (!file.exists()){
            throw new RuntimeException("AI config file does not exist: " + file.getPath());
        }
        JSONObject jsonObject = readJsonFile(file);
        this.url = jsonObject.getString("url");
        this.apiKey = jsonObject.getString("apiKey");
        this.modelName = jsonObject.getString("modelName");
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

    public String getUrl() {
        return url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelName() {
        return modelName;
    }
}
