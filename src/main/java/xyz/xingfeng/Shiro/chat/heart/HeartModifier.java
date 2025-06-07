package xyz.xingfeng.Shiro.chat.heart;

import org.json.JSONObject;
import xyz.xingfeng.Shiro.Tool.Static;
import xyz.xingfeng.Shiro.chat.LLMConfig;
import xyz.xingfeng.Shiro.chat.LLMRequestClient;
import xyz.xingfeng.Shiro.chat.PromptTool;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HeartModifier {
    private boolean simple = false;
    private String msg;
    public static final String SYSTEM_PROMPT = "判断句子的词性\n" +
            "只允许输出这几个词\n" +
            "正面/负面/中性";

    /**
     * 心情值修改器
     * @param msg
     */
    public HeartModifier(String msg){
        this.msg = msg;
        simple_analysis();
        if (!simple){
            llm_analysis();
        }
    }
    /**
     * 简单分析
     * 如果消息长度大于50，则心情值减少0.02
     */
    public void simple_analysis(){
        if (msg.length() > 50) {
            Heart.setHeartValue(Heart.getHeartValue() - 0.02f * ((double) msg.length() / 50));
            simple = true;
        }
    }

    /**
     * 使用LLM分析消息情感
     */
    public void llm_analysis(){
        LLMConfig llmConfig = new LLMConfig();
        llmConfig.LoadConfig(LLMConfig.NANO);

        PromptTool promptTool = new PromptTool(llmConfig.getModelName());
        JSONObject jsonObject = promptTool.addMessage(PromptTool.SYSTEM, SYSTEM_PROMPT).addMessage(PromptTool.USER, msg)
                .buildRequestMessage();
        String post = null;
        try {
            post = new LLMRequestClient(llmConfig.getUrl(), llmConfig.getApiKey(), jsonObject).post();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (post){
            case "正面":
                Heart.setHeartValue(Heart.getHeartValue() + 0.05);
                break;
            case "负面":
                Heart.setHeartValue(Heart.getHeartValue() - 0.05);
                break;
            case "中性":
                Heart.setHeartValue(Heart.getHeartValue() + 0.01);
                break;
            default:
                throw new RuntimeException("LLM返回了未知的结果: " + post);
        }
    }
}
