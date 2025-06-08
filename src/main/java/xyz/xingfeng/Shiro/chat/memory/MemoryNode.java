package xyz.xingfeng.Shiro.chat.memory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MemoryNode {
    //记忆节点
    private String node;
    //节点下记忆
    private List<String> memoryList = new ArrayList<>();
    //创建记忆时间
    private long createTime;
    //上一次使用时间
    private long lastTime;
    //使用次数
    private int count;
    //感情强度0-1
    private double emotionalIntensity;

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public List<String> getMemoryList() {
        return memoryList;
    }

    public void setMemoryList(List<String> memoryList) {
        this.memoryList = memoryList;
    }

    public void addMemory(String memory){
        memoryList.add(memory);
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getEmotionalIntensity() {
        return emotionalIntensity;
    }

    public void setEmotionalIntensity(double emotionalIntensity) {
        this.emotionalIntensity = emotionalIntensity;
    }

    public JSONObject toJson(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("node",node);
        JSONArray ml = new JSONArray();
        memoryList.forEach((m)->{
            ml.put((String)m);
        });
        jsonObject.put("memoryList",ml);
        jsonObject.put("createTime",createTime);
        jsonObject.put("lastTime",lastTime);
        jsonObject.put("count",count);
        jsonObject.put("emotionalIntensity",emotionalIntensity);
        return jsonObject;
    }
}
