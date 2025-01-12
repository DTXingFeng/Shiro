package xyz.xingfeng.Shiro.service;


import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * 群老婆类
 */
public class Wife {
    private static final String WIFE_PATH = "Wife/";

    //将一个群友添加进群老婆列表
    public void addWife(String qq, String group){
        Path chatHistoryDir = Paths.get(WIFE_PATH);
        Path filePath = chatHistoryDir.resolve(group + ".txt");
        File file =filePath.toFile();
        // 确保文件存在
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file: " + filePath, e);
            }
        }
        //将群友添加进群老婆列表
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            int i = 0;
            ArrayList<String> strings = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null){
                strings.add(line);
                i++;
            }
            if (i>200){
                //如果群老婆数量超过200，删除第一个
                strings.remove(0);
            }
            strings.add(qq);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            for (String s : strings){
                bufferedWriter.write(s);
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
            bufferedReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    //抽取一个群老婆
    public String getWife(String group) {
        Path chatHistoryDir = Paths.get(WIFE_PATH);
        Path filePath = chatHistoryDir.resolve(group + ".txt");
        File file =filePath.toFile();
        // 确保文件存在
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file: " + filePath, e);
            }
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            ArrayList<String> strings = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                strings.add(line);
            }
            strings.add("391459725");
            int i = (int) (Math.random() * strings.size());
            return strings.get(i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
