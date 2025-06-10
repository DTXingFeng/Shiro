package xyz.xingfeng.Shiro.chat.memory;

import org.json.JSONObject;
import xyz.xingfeng.Shiro.chat.LLMConfig;
import xyz.xingfeng.Shiro.chat.LLMRequestClient;
import xyz.xingfeng.Shiro.chat.PromptTool;
import xyz.xingfeng.Shiro.chat.heart.Heart;

/**
 * 判断是否符合长期记忆条件
 */
public class MemoryTrigger {
    private String message;
    // 消息分析任务
    public MemoryTrigger(String msg){
        this.message = msg;
    }

    //判断是否触发搜索长期记忆
    public boolean isMemoryTriggered(){
        //先进行简单判断
        if (isSimpleMemoryTriggered()){
            return true;
        }
        if (Math.random() > Heart.getHeartValue() * 0.5) {
            //心情越高就有越大的概率使用llm
            return false;
        }
        //如果简单判断没触发，再使用LLM判断
        return isLongTermMemoryTriggered();
    }

    //简单判断
    private boolean isSimpleMemoryTriggered(){
        //长字符串也能触发长期记忆搜索
        if (message.length() >= 25){
            return true;
        }
        //如果消息包含"记得"、"上次"等关键词，则触发长期记忆
        String[] keywords = {"记得", "上次", "之前", "是什么", "你知道"};
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                System.out.println("触发简单记忆搜索: " + keyword);
                return true;
            }
        }

        System.out.println("没触发简单记忆搜索");
        return false;
    }

    //使用llm判断长记忆
    private boolean isLongTermMemoryTriggered(){
        String systemPrompt =  String.format("""
                消息分析任务
                用户输入："%s"
                
                # 判断要求
                1. 是否在询问历史信息？（如"你记得..."）
                2. 是否包含需要长期记忆的概念？（如人物/地点/习惯）
                3. 是否隐含需要上下文（如"像上次那样"）
                
                # 输出格式
                {
                    "need_memory": bool,
                    "reason": "不超过10字的理由"
                }""",message);
        LLMConfig llmConfig = new LLMConfig();
        llmConfig.LoadConfig(LLMConfig.NANO);
        PromptTool promptTool = new PromptTool(llmConfig.getModelName());
        promptTool.addMessage(PromptTool.SYSTEM,systemPrompt);
        String post = "";
        try {
            post = new LLMRequestClient(llmConfig.getUrl(), llmConfig.getApiKey(), promptTool.buildRequestMessage()).post();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            JSONObject jsonObject = new JSONObject(post);
            if (jsonObject.has("need_memory") && jsonObject.has("reason")) {
                boolean needMemory = jsonObject.getBoolean("need_memory");
                String reason = jsonObject.getString("reason");
                if (needMemory) {
                    System.out.println("触发记忆搜索 " + reason);
                } else {
                    System.out.println("没触发: " + reason);
                }
                return needMemory;
            } else {
                throw new RuntimeException("LLM返回的JSON格式错误，缺少need_memory或reason字段: " + post);
            }
        }catch (Exception e){
            throw new RuntimeException("LLM返回的JSON格式错误: " + post, e);
        }
    }
}
