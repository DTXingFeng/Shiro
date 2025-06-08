package xyz.xingfeng.Shiro.chat.memory;


import okio.Path;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Memory {
    ArrayList<MemoryNode> memoryNodes = new ArrayList<>();

    public Memory(){
        //初始化
        initialization();
        //加载记忆文件
        memoryNodes = loadNode();
    }


    //查询某个记忆节点
    public MemoryNode queryNode(String nodeName){
        for (MemoryNode node : memoryNodes) {
            if (node.getNode().equals(nodeName)) {
                node.setLastTime(System.currentTimeMillis());
                node.setCount(node.getCount()+1);
                return node;
            }
        }
        //如果没有找到，返回null
        return null;
    }

    /**
     * 创建某个记忆节点
     * @param memoryNode 记忆节点
     */
    public void createNode(MemoryNode memoryNode){
        if (queryNode(memoryNode.getNode())==null){
            memoryNodes.add(memoryNode);
        }
    }

    /**
     * 创建某个记忆节点
     * @param nodeName 节点名字
     * @param memoryList    记忆内容
     * @param emotionalIntensity    情绪强度
     */
    public void createNode(String nodeName, List<String> memoryList, double emotionalIntensity){
        MemoryNode memoryNode = new MemoryNode();
        memoryNode.setNode(nodeName);
        memoryNode.setMemoryList(memoryList);
        memoryNode.setCreateTime(System.currentTimeMillis());
        memoryNode.setLastTime(System.currentTimeMillis());
        memoryNode.setCount(0);
        memoryNode.setEmotionalIntensity(emotionalIntensity);
        createNode(memoryNode);
    }

    //合并某个记忆节点
    public void merge(MemoryNode memoryNode){
        MemoryNode node = queryNode(memoryNode.getNode());
        if (node != null){
            List<String> ml = node.getMemoryList();
            ml.forEach((m) ->{
                if (!memoryNode.getMemoryList().contains(m)){
                    memoryNode.addMemory(m);
                }
            });
        }
        //替换掉memoryNodes中的节点
        for (int i = 0; i < memoryNodes.size(); i++) {
            if (memoryNodes.get(i).getNode().equals(memoryNode.getNode())) {
                memoryNodes.set(i, memoryNode);
                return;
            }
        }
    }

    /**
     * 保存节点
     */
    public void save(){
        JSONObject jsonObject = new JSONObject();
        JSONArray data = new JSONArray();
        memoryNodes.forEach((mn) ->{
            data.put(mn.toJson());
        });
        jsonObject.put("data",data);
        Path memory = Path.get("ChatHistory/Memory/memory.json");
        File file = memory.toFile();
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
            bw.write(jsonObject.toString(1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    //从json里加载节点
    public ArrayList<MemoryNode> loadNode(){
        ArrayList<MemoryNode> nodes = new ArrayList<>();
        JSONObject load = load();
        JSONArray data = load.getJSONArray("data");
        data.forEach((item) ->{
            JSONObject jsonObject = (JSONObject) item;
            MemoryNode memoryNode = new MemoryNode();
            memoryNode.setNode(jsonObject.getString("node"));
            List<String> ml = new ArrayList<>();
            jsonObject.getJSONArray("memoryList").forEach((list) ->{
                ml.add((String) list);
            });
            memoryNode.setMemoryList(ml);
            memoryNode.setCreateTime(jsonObject.getLong("createTime"));
            memoryNode.setLastTime(jsonObject.getLong("lastTime"));
            memoryNode.setCount(jsonObject.getInt("count"));
            memoryNode.setEmotionalIntensity(jsonObject.getDouble("emotionalIntensity"));
            nodes.add(memoryNode);
        });
        return nodes;
    }

    //加载记忆文件
    public JSONObject load(){
        Path memory = Path.get("ChatHistory/Memory/memory.json");
        File file = memory.toFile();
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String s = "";
            StringBuilder sb = new StringBuilder();
            while ((s = br.readLine()) != null){
                sb.append(s);
            }
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    //初始化长期记忆文件
    private void initialization(){
        //初始化长记忆模块
        //确保是第一次使用
        //检查文件是否存在
        Path memory = Path.get("ChatHistory/Memory");
        File file = memory.toFile();
        if (!file.exists()){
            file.mkdirs();
        }
        memory = Path.get("ChatHistory/Memory/memory.json");
        file = memory.toFile();
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data",new JSONArray());
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
            bw.write(jsonObject.toString(1));
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
