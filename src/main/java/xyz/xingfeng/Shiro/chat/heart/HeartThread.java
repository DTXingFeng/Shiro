package xyz.xingfeng.Shiro.chat.heart;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class HeartThread implements Runnable{
    /**
     * 每五分钟就降低心情值0.1
     */
    @Override
    public void run() {
        // 每五分钟降低心情值0.1
        try {
            Thread.sleep(300*1000);
            Heart.setHeartValue(Heart.getHeartValue()-0.1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void init() {
        Thread thread = new Thread(this);
        thread.setDaemon(true); // 设置为守护线程
        thread.start();
    }
}
