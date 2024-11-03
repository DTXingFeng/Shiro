package xyz.xingfeng.Shiro.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MessageStorage {
    private static final String DIRECTORY = "groupMsg";
    private static final int MAX_MESSAGES = 100;

    public MessageStorage() {
        // 创建存储消息的目录
        File dir = new File(DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void addMessage(long groupId, Long senderUser, String message) {
        String fileName = getFileName(groupId);
        List<String> messages = getMessageHistory(groupId);
        messages.add(senderUser + ": " + message);

        // 如果消息数量超过限制，删除最早的一条
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (String msg : messages) {
                writer.write(msg);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getMessageHistory(long groupId) {
        String fileName = getFileName(groupId);
        List<String> messages = new ArrayList<>();
        if (Files.exists(Paths.get(fileName))) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    messages.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }

    private String getFileName(long groupId) {
        return DIRECTORY + "/" + groupId + ".txt";
    }
}

