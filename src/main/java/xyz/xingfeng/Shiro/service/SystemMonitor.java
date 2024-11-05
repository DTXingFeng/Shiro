package xyz.xingfeng.Shiro.service;

import com.sun.management.OperatingSystemMXBean;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * 查询系统监控信息
 */
public class SystemMonitor {

    private String cpuLoad;
    private String memoryUsage;
    private String diskUsage;
    private String systemRunTime;

    public SystemMonitor() {
        // 获取操作系统相关信息
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        // 获取内存信息，使用 OSHI 库
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        double totalMemoryGB = memory.getTotal() / 1024.0 / 1024.0 / 1024.0; // 转换为 GB（double 类型）
        double availableMemoryGB = memory.getAvailable() / 1024.0 / 1024.0 / 1024.0; // 转换为 GB（double 类型）
        double usedMemoryGB = totalMemoryGB - availableMemoryGB; // 已使用内存

        // 四舍五入保留 2 位小数
        this.memoryUsage = String.format("%.2f GB / %.2f GB", usedMemoryGB, totalMemoryGB);

        // 获取 CPU 使用率，延迟一段时间再查询
        double cpuLoad = 0;
        while (true) {
            cpuLoad = osBean.getCpuLoad() * 100;  // 转为百分比
            if (cpuLoad == 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            break;
        }
        this.cpuLoad = String.format("%.2f", cpuLoad);

        // 获取磁盘使用情况
        File disk = new File("/");
        double totalSpace = (double) disk.getTotalSpace() / 1024 / 1024 / 1024;
        double freeSpace = (double) disk.getFreeSpace() / 1024 / 1024 / 1024;
        this.diskUsage =String.format("%.2f GB / %.2f GB", freeSpace, totalSpace);

        // 获取系统运行时间
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        this.systemRunTime = days + " 天 " + hours % 24 + " 小时 " + minutes % 60 + " 分钟 " + seconds % 60 + " 秒";
    }

    public String getCpuLoad() {
        return cpuLoad;
    }

    public String getMemoryUsage() {
        return memoryUsage;
    }

    public String getDiskUsage() {
        return diskUsage;
    }

    public String getSystemRunTime() {
        return systemRunTime;
    }
}
