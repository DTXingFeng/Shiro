package xyz.xingfeng.Shiro.chat.heart;

/**
 * bot心情类
 */
public class Heart {
    //心情值0-1
    //当大于0.8是兴奋状态
    //0.3-0.8是普通状态
    //小于0.3是疲惫状态
    private static double heartValue = 0;

    // 提供安全的设置方法
    public synchronized static void setHeartValue(double value) {
        if (value < 0) {
            heartValue = 0;
        } else if (value > 1) {
            heartValue = 1;
        } else {
            heartValue = value;
        }
    }

    // 提供获取方法
    public static double getHeartValue() {
        return heartValue;
    }

    // 获取当前心情状态
    public static String getHeartState() {
        if (heartValue > 0.8) {
            return "兴奋状态";
        } else if (heartValue >= 0.3) {
            return "普通状态";
        } else {
            return "疲惫状态";
        }
    }
}
