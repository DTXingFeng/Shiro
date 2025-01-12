package xyz.xingfeng.Shiro.service;


import java.io.*;
import java.util.ArrayList;

/**
 * 群老婆类
 */
public class Wife {
    private static final String WIFE_PATH = "Wife/";

    //将一个群友添加进群老婆列表
    public void addWife(String qq, String group){
        File file = new File(WIFE_PATH+group+".txt");
        if (!file.exists()){
            //如果文件不存在，创建文件
            file.mkdirs();
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    //抽取一个群老婆
    public String getWife(String group) {
        File file = new File(WIFE_PATH + group + ".txt");
        if (!file.exists()) {
            //如果文件不存在，创建文件
            file.mkdirs();
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
