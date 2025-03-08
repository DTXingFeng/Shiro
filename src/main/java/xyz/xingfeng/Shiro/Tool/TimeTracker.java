package xyz.xingfeng.Shiro.Tool;


import org.json.JSONObject;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

/**
 * 该类用于统计群中活跃度，以判断bot是否发言
 */
public class TimeTracker {
    /**
     * 统计多少秒内的消息
     */
    private static final int TIME = 60;
    /**
     * 限定时间内需要多少信息才判定活跃
     */
    private static final int COUNT = 10;


    private String groupId = null;
    public TimeTracker(String groupId){
        this.groupId = groupId;
    }
    public TimeTracker(){}

    /**
     * 查看该群功能是否打开
     */
    public boolean isOpen() {
        File file = new File(Static.CONFIG_PATH);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String temp = "";
            StringBuilder sb = new StringBuilder();
            while ((temp = bufferedReader.readLine()) != null) {
                sb.append(temp);
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            if (jsonObject.getJSONObject("aichat").has(groupId)) {
                return jsonObject.getJSONObject("aichat").getBoolean(groupId);
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 读取文件
     * @return
     */
    private ArrayList<String> readerFile(){
        Path chatHistoryDir = Paths.get("TimeTracker");
        Path filePath = chatHistoryDir.resolve(groupId + ".txt");
        File f = filePath.toFile();
        // 确保文件存在
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(f));
            String temp = "";
            ArrayList<String> sb = new ArrayList<>();
            while ((temp = bufferedReader.readLine())!=null){
                sb.add(temp);
            }
            return sb;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 是否符合活跃度
     * @return
     */
    public boolean isActive() {
        if (!isOpen()) {
            return false;
        }
        ArrayList<String> s = readerFile();
        if (s == null || s.isEmpty()) {
            return false;
        }
        if (s.size() < COUNT) {
            return false;
        }

        // 动态生成冷却时间
        int coolDownTime = generateCoolDownTime();
        System.out.println("Generated cool down time: " + coolDownTime + " seconds");

        // 检查发言冷却
        long l = System.currentTimeMillis();
        if (l - getLastTime() < coolDownTime * 1000) {
            return false;
        }
        updateTime();
        return true;
    }

    /**
     * 生成符合正态分布的冷却时间（20秒到180秒）
     */
    public int generateCoolDownTime() {
        Random random = new Random();
        double mean = 100.0; // 均值，冷却时间的中间值
        double stdDev = 30.0; // 标准差，控制分布的宽度

        // 生成符合正态分布的值
        double gaussianValue = random.nextGaussian() * stdDev + mean;

        // 将值限制在20秒到180秒之间
        int coolDownTime = (int) Math.round(gaussianValue);
        coolDownTime = Math.max(20, Math.min(180, coolDownTime));

        return coolDownTime;
    }

    /**
     * 更新发言时间
     */
    public void updateTime() {
        Path chatHistoryDir = Paths.get("TimeTracker");
        Path filePath = chatHistoryDir.resolve(groupId);
        File f = filePath.toFile();
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            FileWriter fileWriter = new FileWriter(f);
            fileWriter.write(System.currentTimeMillis() + "");
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获得上次发言时间
     */
    public long getLastTime() {
        Path chatHistoryDir = Paths.get("TimeTracker");
        Path filePath = chatHistoryDir.resolve(groupId);
        File f = filePath.toFile();
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(f));
            String temp = bufferedReader.readLine();
            if (temp == null || temp.equals("")) {
                return 0;
            } else {
                return Long.parseLong(temp);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 验证文件内都是规则内的时间
     */
    public void check() {
        long l = System.currentTimeMillis();
        ArrayList<String> s = readerFile();
        if (s == null || s.equals("")){
            return;
        }
        ArrayList<String> strings = new ArrayList<>();
        for (String s1 : s){
            if (l - Long.parseLong(s1) < TIME * 1000){
                strings.add(s1);
            }
        }
        write(strings);
    }

    /**
     * 写入文件
     */
    public void write(ArrayList<String> strings){
        String s = "";
        for (String s1 : strings){
            s += s1 + "\n";
        }
        Path chatHistoryDir = Paths.get("TimeTracker");
        Path filePath = chatHistoryDir.resolve(groupId + ".txt");
        File f = filePath.toFile();
        try {
            FileWriter fileWriter = new FileWriter(f);
            fileWriter.write(s);
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 添加时间
     */
    public void addTime() {
        Path chatHistoryDir = Paths.get("TimeTracker");
        Path filePath = chatHistoryDir.resolve(groupId + ".txt");
        File f = filePath.toFile();
        //验证正确性
        check();
        ArrayList<String> strings = readerFile();
        //添加时间
        strings.add(System.currentTimeMillis() + "");
        //写入文件
        write(strings);
    }

    /**
     * 打开功能
     */
    public void open() {
        onOrOff(true);
    }
    /**
     * 关闭功能
     */
    public void close() {
        onOrOff(false);
    }

    /**
     * 功能操作
     */
    public void onOrOff(Boolean b){
        File file = new File(Static.CONFIG_PATH);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String temp = "";
            StringBuilder sb = new StringBuilder();
            while ((temp = bufferedReader.readLine()) != null) {
                sb.append(temp);
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONObject aichat = jsonObject.getJSONObject("aichat");
            if (b){
                aichat.put(groupId, true);
            }else {
                aichat.put(groupId, false);
            }
            jsonObject.put("aichat", aichat);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(jsonObject.toString());
            fileWriter.close();
            bufferedReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
