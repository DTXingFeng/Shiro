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

    private int coolDownTime = 0; // 冷却时间，单位为秒


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
        //检查功能是否打开
        if (!isOpen()) {
            return false;
        }
        //文件是否为空
        ArrayList<String> s = readerFile();
        if (s == null || s.isEmpty()) {
            return false;
        }

        // 检查发言冷却
        long l = System.currentTimeMillis();
        if (l - getLastTime() < coolDownTime * 1000L) {
            return false;
        }
        generateCoolDownTime();
        //发言欲是否达标
        if (isReach()){
            //更新发言时间
            updateTime();
            writeSpeechDesire(0);
            return true;
        }
        //发言频率是否达标
        if (s.size() < COUNT) {
            return false;
        }
        updateTime();
        return true;
    }

    /**
     * 生成符合正态分布的冷却时间（20秒到180秒）
     */
    public void generateCoolDownTime() {
        Random random = new Random();
        double mean = 100.0; // 均值，冷却时间的中间值
        double stdDev = 30.0; // 标准差，控制分布的宽度

        // 生成符合正态分布的值
        double gaussianValue = random.nextGaussian() * stdDev + mean;

        // 将值限制在20秒到180秒之间
        int coolDownTime = (int) Math.round(gaussianValue);
        this.coolDownTime = Math.max(20, Math.min(180, coolDownTime));
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
     * 添加时间，以及更新发言欲
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
        double v = readSpeechDesire();
        //随机增加1-4
        v += new Random().nextInt(4) + 1;
        writeSpeechDesire(v);
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

    /**
     * 判定是否达到阈值
     */
    public boolean isReach(){
        double v = readSpeechDesire();
        //发言欲为1-40时为1%发言率,40之后指数上升，直到100为100%发言率
        if (v >= 1 && v <= 40){
            return new Random().nextInt(100) < 1;
        }else if(v > 40) {
            return new Random().nextInt(100) < Math.pow(0.8, v - 40);
        }
        return false;
    }



    /**
     * 获取发言欲文件路径
     */
    private Path getSpeechDesireFilePath() {
        Path chatHistoryDir = Paths.get("TimeTracker");
        return chatHistoryDir.resolve(groupId + "_speechDesire.txt");
    }

    /**
     * 读取发言欲数值
     */
    public double readSpeechDesire() {
        Path filePath = getSpeechDesireFilePath();
        File f = filePath.toFile();
        if (!f.exists()) {
            //创建文件，连带文件夹
            try {
                f.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create speechDesire file", e);
            }
            return 0;
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(f))) {
            String value = bufferedReader.readLine();
            return value == null ? 0 : Double.parseDouble(value);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read speechDesire file", e);
        }
    }

    /**
     * 写入发言欲数值
     */
    public void writeSpeechDesire(double value) {
        Path filePath = getSpeechDesireFilePath();
        File f = filePath.toFile();
        try (FileWriter fileWriter = new FileWriter(f)) {
            fileWriter.write(String.valueOf(value));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write speechDesire file", e);
        }
    }
}
