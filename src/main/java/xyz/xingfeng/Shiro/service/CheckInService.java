package xyz.xingfeng.Shiro.service;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Random;

public class CheckInService {
    private static final int MIN_POINTS = 50;
    private static final int MAX_POINTS = 500;
    private static final String USER_FOLDER = "user";

    private long userId;
    private Path userFilePath;

    public CheckInService(long userId) {
        this.userId = userId;
        this.userFilePath = Paths.get(USER_FOLDER, userId + ".json");

        // Ensure the user directory exists
        try {
            Files.createDirectories(userFilePath.getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String checkIn() {
        UserCheckInData data = readUserData();

        LocalDate today = LocalDate.now();
        if (today.equals(data.getLastCheckInDate())) {
            return "今日已签到，已连续签到" + data.getConsecutiveDays() + "天";
        }

        int pointsEarned = getRandomPoints();
        if (data.getLastCheckInDate() != null && !today.minusDays(1).equals(data.getLastCheckInDate())) {
            data.setConsecutiveDays(1); // 重置连续签到天数为 1
            data.setLastCheckInDate(today);
            data.addPoints(pointsEarned);
            writeUserData(data);
            return "签到成功，已断签，获得" + pointsEarned + "积分";
        }

        data.setConsecutiveDays(data.getConsecutiveDays() + 1);
        data.setLastCheckInDate(today);
        data.addPoints(pointsEarned);
        writeUserData(data);
        return "签到成功，已连续签到" + data.getConsecutiveDays() + "天，获得" + pointsEarned + "积分";
    }

    private int getRandomPoints() {
        Random rand = new Random();
        return rand.nextInt(MAX_POINTS - MIN_POINTS + 1) + MIN_POINTS;
    }

    private UserCheckInData readUserData() {
        if (Files.exists(userFilePath)) {
            try {
                String content = Files.readString(userFilePath);
                JSONObject jsonObject = new JSONObject(content);

                UserCheckInData data = new UserCheckInData();
                data.setLastCheckInDate(LocalDate.parse(jsonObject.getString("lastCheckInDate")));
                data.setConsecutiveDays(jsonObject.getInt("consecutiveDays"));
                data.setPoints(jsonObject.getInt("points"));
                return data;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new UserCheckInData();
    }

    private void writeUserData(UserCheckInData data) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lastCheckInDate", data.getLastCheckInDate().toString());
        jsonObject.put("consecutiveDays", data.getConsecutiveDays());
        jsonObject.put("points", data.getPoints());

        try {
            Files.writeString(userFilePath, jsonObject.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class UserCheckInData {
        private LocalDate lastCheckInDate;
        private int consecutiveDays;
        private int points;

        public LocalDate getLastCheckInDate() {
            return lastCheckInDate;
        }

        public void setLastCheckInDate(LocalDate lastCheckInDate) {
            this.lastCheckInDate = lastCheckInDate;
        }

        public int getConsecutiveDays() {
            return consecutiveDays;
        }

        public void setConsecutiveDays(int consecutiveDays) {
            this.consecutiveDays = consecutiveDays;
        }

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }

        public void addPoints(int points) {
            this.points += points;
        }
    }
}
