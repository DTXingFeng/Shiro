package xyz.xingfeng.Shiro;

import com.mikuac.shiro.annotation.*;
import com.mikuac.shiro.annotation.common.Order;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.common.ActionList;
import com.mikuac.shiro.dto.action.common.MsgId;
import com.mikuac.shiro.dto.action.response.GroupMemberInfoResp;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import com.mikuac.shiro.dto.event.notice.GroupBanNoticeEvent;
import com.mikuac.shiro.enums.AtEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import xyz.xingfeng.Shiro.Tool.ImageAnalysis;
import xyz.xingfeng.Shiro.Tool.Static;
import xyz.xingfeng.Shiro.Tool.TimeTracker;
import xyz.xingfeng.Shiro.network.NetRequest;
import xyz.xingfeng.Shiro.service.*;

import javax.security.auth.Subject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Shiro
@Component
public class ExamplePlugin {
    private static final Logger Log= LogManager.getLogger(ExamplePlugin.class);


    /**
     * 群禁言事件
     * @param bot
     * @param event
     */
    @GroupBanNoticeHandler
    public void ban(Bot bot, GroupBanNoticeEvent event){
        Long groupId = event.getGroupId();
        String subType = event.getSubType();
        if (event.getUserId() == bot.getSelfId()) {
            Log.info("被群：{} {}", groupId, subType);
            if (subType.equals("ban")){
                new TimeTracker(groupId + "").close();
            }else if (subType.equals("lift_ban")){
                new TimeTracker(groupId + "").open();
            }
        }
    }

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "/help|/帮助|/帮助信息")
    public void help(Bot bot, GroupMessageEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append("帮助信息:\n");
        builder.append("/图图 [tag] 随机获取tag标签的图片\n");
        builder.append("/图图 随机获取一张图片\n");
        builder.append("查询Java服务器 [服务器地址]:[端口号] 查询指定服务器的状态\n");
        builder.append("签到 签到并获得积分\n");
        builder.append("重置记忆 重置当前群记忆\n");
        builder.append("今日老婆：获取今日老婆\n");
        builder.append("服务器状态：获取当前服务器状态\n");
        builder.append("打开自动群聊：打开自动群聊功能\n");
        builder.append("关闭自动群聊：关闭自动群聊功能\n");
        builder.append("切换ai配置 [模型名]：切换ai配置\n");
        builder.append("ai配置列表：查看所有ai配置\n");
        bot.sendGroupMsg(event.getGroupId(), builder.toString(), false);
    }

    //重置当前群记忆
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "重置记忆")
    public void resetMemory(Bot bot, GroupMessageEvent event) {
        Path chatHistoryDir = Paths.get("ChatHistory");
        Path filePath = chatHistoryDir.resolve(event.getGroupId() + ".json");
        File f = filePath.toFile();
        if (f.exists()) {
            boolean delete = f.delete();
            if (delete){
                bot.sendGroupMsg(event.getGroupId(), "记忆已重置", false);
            }else {
                bot.sendGroupMsg(event.getGroupId(), "文件删除失败", false);
            }
        }

    }

    /**
     * 守望赚钱催促器
     * @param bot
     * @param event
     */
    @GroupMessageHandler
    @MessageHandlerFilter(senders = 1093757211,groups = 915912092)
    public void shouwang(Bot bot, GroupMessageEvent event){
        //每天只提示一次
        if (LocalDate.now().getDayOfYear() != Static.date){
            Static.date = LocalDate.now().getDayOfYear();
            String msg = MsgUtils.builder()
                    .at(1093757211)
                    .text("什么时候带大伙赚钱")
                    .build();
            bot.sendGroupMsg(event.getGroupId(),msg,false);
        }
    }

    /**
     * 当前pc占用状态
     * @param bot
     * @param event
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "服务器状态")
    public void serverStatus(Bot bot, GroupMessageEvent event) {
        SystemMonitor systemMonitor = new SystemMonitor();
        String build = MsgUtils.builder()
                .text("服务器状态:\n")
                .text("CPU使用率: ")
                .text(systemMonitor.getCpuLoad() + "%\n")
                .text("内存使用率: ")
                .text(systemMonitor.getMemoryUsage() + "\n")
                .text("磁盘使用率: ")
                .text(systemMonitor.getDiskUsage() + "\n")
                .text("持续运行时间: ")
                .text(systemMonitor.getSystemRunTime())
                .build();
        bot.sendGroupMsg(event.getGroupId(),build, false);
    }
    /**
     * 今日老婆
     * @param bot
     * @param event
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "今日老婆")
    public void todayLover(Bot bot, GroupMessageEvent event) {
        ActionList<GroupMemberInfoResp> groupMemberList = bot.getGroupMemberList(event.getGroupId());
        Collections.shuffle(groupMemberList.getData()); // 打乱列表顺序

        String today = LocalDate.now().toString(); // 优化日期获取
        String filePath = "todayLover/" + event.getGroupId() + ".json";

        //先随机抽取一个老婆
        Wife wife = new Wife();
        while (true) {
            String wife1 = wife.getWife(String.valueOf(event.getGroupId()));
            if (wife1 != null) {
                //wife1不能是自己
                if (wife1.equals(String.valueOf(event.getUserId()))) {
                    continue;
                }
                if (!hasLoverToday(filePath, String.valueOf(event.getUserId()), today)) {
                    saveLoverInfo(filePath, String.valueOf(event.getUserId()), wife1, today);
                    sendMessage(bot, String.valueOf(event.getGroupId()), String.valueOf(event.getUserId()), String.valueOf(wife1));
                    return;
                } else {
                    // 已经有老婆，发送已存在老婆的消息
                    String existingLover = getExistingLover(bot, event.getGroupId(),filePath, event.getUserId(), today);
                    if (existingLover != null) {
                        sendExistingLoverMessage(bot, String.valueOf(event.getGroupId()), String.valueOf(event.getUserId()), existingLover);
                        return;
                    }
                }
                return;
            }
        }

    }


    /**
     * 判断是否已经有老婆
     * @param filePath 文件路径
     * @param userId 用户id
     * @param today 今天日期
     * @return
     */
    private boolean hasLoverToday(String filePath, String userId, String today) {
        File f = new File(filePath);
        if (!f.exists()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONArray jsonArray = jsonObject.getJSONArray("data");

            for (int j = 0; j < jsonArray.length(); j++) {
                if (jsonArray.getJSONObject(j).has(userId)) {
                    String date = jsonArray.getJSONObject(j).getString(userId);
                    if (date.equals(today)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 保存老婆信息
     * @param filePath 文件路径
     * @param userId 用户id
     * @param loverId 老婆id
     * @param today 今天日期
     */
    private void saveLoverInfo(String filePath, String userId, String loverId, String today) {
        File f = new File(filePath);
        StringBuilder stringBuilder;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
            stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        JSONArray jsonArray = new JSONArray();
        JSONObject json = new JSONObject(stringBuilder.toString());
        json.put(userId, today);
        json.put("userId", loverId);
        jsonArray.put(json);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", jsonArray);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Bot bot, String groupId, String userId, String loverId) {
        String message = MsgUtils.builder()
                .at(Long.parseLong(userId))
                .text("今日老婆是:")
                .at(Long.parseLong(loverId))
                .img("https://q1.qlogo.cn/g?b=qq&nk=" + loverId + "&s=640")
                .build();
        bot.sendGroupMsg(Long.parseLong(groupId), message, false);
    }

    private String getExistingLover(Bot bot,Long groupId,String filePath, Long userId, String today) {
        File f = new File(filePath);
        if (!f.exists()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONArray jsonArray = jsonObject.getJSONArray("data");

            for (int j = 0; j < jsonArray.length(); j++) {
                if (jsonArray.getJSONObject(j).has(String.valueOf(userId))) {
                    String date = jsonArray.getJSONObject(j).getString(String.valueOf(userId));
                    if (date.equals(today)) {
                        String loverId = jsonArray.getJSONObject(j).getString("userId");
                        return loverId;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendExistingLoverMessage(Bot bot, String groupId, String userId, String existingLover) {
        String message = MsgUtils.builder()
                .at(Long.parseLong(userId))
                .text("今天已经有老婆了，ta是:")
                .at(Long.parseLong(existingLover))
                .img("https://q1.qlogo.cn/g?b=qq&nk=" + existingLover + "&s=640")
                .build();
        bot.sendGroupMsg(Long.parseLong(groupId), message, false);
    }



    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "查询Java服务器\\s(.*)")
    public void minecraftServer(Bot bot, GroupMessageEvent event, Matcher matcher) {
        MinecraftServerPing.MinecraftServerStatus status;
        try {
            Pattern compile = Pattern.compile("^查询Java服务器\\s(.*):(\\d+)");
            Matcher mat = compile.matcher(event.getMessage());
            if (mat.find()){
                String serverAddress = mat.group(1);
                int serverPort = Integer.parseInt(mat.group(2));
                // 创建 MinecraftServerPing 对象并查询服务器
                MinecraftServerPing ping = new MinecraftServerPing(serverAddress, serverPort);
                status = ping.query();
            }else {
                // 获取服务器地址
                String serverAddress = matcher.group(1);
                // 创建 MinecraftServerPing 对象并查询服务器
                MinecraftServerPing ping = new MinecraftServerPing(serverAddress, 25565);
                status = ping.query();
            }

            // 构建回复消息
            StringBuilder response = new StringBuilder();
            response.append("服务器信息:\n");
            response.append("MOTD: ").append(status.getDescription()).append("\n");
            response.append("在线玩家数: ").append(status.getOnline()).append("\n");
            response.append("最大玩家数: ").append(status.getMax()).append("\n");
            response.append("游戏版本: ").append(status.getVersion()).append("\n");
            response.append("协议版本: ").append(status.getProtocol()).append("\n");
            response.append("在线玩家:").append("\n");
            for (String s : status.getPlayers()) {
                response.append("\t").append(s).append("\n");
            }
            response.append("\b");
            // 发送回复消息
            bot.sendGroupMsg(event.getGroupId(), response.toString(), false);
        } catch (IOException e) {
            // 处理查询失败的情况
            bot.sendGroupMsg(event.getGroupId(), "查询服务器失败: " + e.getMessage(), false);
        }
    }

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "/图图.*")
    public void randomImage(Bot bot,GroupMessageEvent event,Matcher matcher){
        List<String> imageUrl = new ArrayList<>();
        Pattern compile = Pattern.compile("/图图 (.*)");
        Matcher mat = compile.matcher(event.getMessage());
        try {
            if (mat.find()){
                imageUrl = new ImageService(mat.group(1)).getImageBase64();
            }else {
                imageUrl = new ImageService().getImageBase64();
            }
        } catch (Exception e) {
            bot.sendGroupMsg(event.getGroupId(),"api请求过程发生错误,错误原因"+e.toString(),false);
        }
        if (imageUrl.equals(new ArrayList<>())){
            //没能从api中获得url
            bot.sendGroupMsg(event.getGroupId(),"api请求过程发生错误:没能从api中获得url",false);
        }
        MsgUtils builder = MsgUtils.builder();
        for (String s : imageUrl){
            builder.img(s);
        }
        String build = builder.build();
        try {
            ActionData<MsgId> msgIdActionData = bot.sendGroupMsg(event.getGroupId(), build, false);
            Log.info(msgIdActionData.getRetCode());
        }catch (Exception e){
            bot.sendGroupMsg(event.getGroupId(),"图片发送失败,错误原因"+e.toString(),false);
        }

    }

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "签到")
    public void checkIn(Bot bot,GroupMessageEvent event){
        String s = new CheckInService(event.getUserId()).checkIn();
        bot.sendGroupMsg(event.getGroupId(),s,false);
        Log.info(s);
    }


    /**
     * 打开\关闭自动群聊
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "打开自动群聊|关闭自动群聊",senders = 2695570953L)
    public void openAutoChat(Bot bot,GroupMessageEvent event,Matcher matcher){
        String action = matcher.group();
        switch (action) {
            case "打开自动群聊":
                new TimeTracker(event.getGroupId()+"").open();
                break;
            case "关闭自动群聊":
                new TimeTracker(event.getGroupId()+"").close();
                break;
            default:
        }
        bot.sendGroupMsg(event.getGroupId(),"已"+action+"功能",false);
    }

    /**
     * 切换模型
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "切换ai配置 (.*)")
    public void changeModel(Bot bot,GroupMessageEvent event,Matcher matcher){
        String model = matcher.group(1);
        Path aiConfigDir = Paths.get("aiConfig");
        File file = aiConfigDir.resolve(model + ".json").toFile();
        if (!file.exists()){
            bot.sendGroupMsg(event.getGroupId(),"模型不存在",false);
            return;
        }
        File f = new File(Static.CONFIG_PATH);
        JSONObject jsonObject = readJsonFile(f);
        jsonObject.put("aiConfigFile",model);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        bot.sendGroupMsg(event.getGroupId(),"已切换ai配置为"+model,false);
    }

    /**
     * ai配置列表
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "ai配置列表")
    public void modelList(Bot bot,GroupMessageEvent event){
        Path aiConfigDir = Paths.get("aiConfig");
        File[] files = aiConfigDir.toFile().listFiles();
        StringBuilder builder = new StringBuilder();
        builder.append("ai配置列表:\n");
        for (File file : files) {
            builder.append(file.getName().replace(".json","")).append("\n");
        }
        bot.sendGroupMsg(event.getGroupId(), builder.toString(), false);
    }

    /**
     * 创建ai配置
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "(?s)创建ai配置 (.*)")
    public void createModel(Bot bot,PrivateMessageEvent event,Matcher matcher) {
        JSONObject json = new JSONObject();
        String[] key = {"apiKey", "url", "modelName"};
        String[] split = matcher.group(1).split("\n");
        if (split.length != 4) {
            bot.sendPrivateMsg(event.getUserId(), "参数错误", false);
            return;
        }
        for (int i = 0; i < split.length; i++) {
            if (i == 0) {
                Path aiConfigDir = Paths.get("aiConfig");
                File file = aiConfigDir.resolve(split[i] + ".json").toFile();
                if (file.exists()) {
                    bot.sendPrivateMsg(event.getUserId(), "模型已存在", false);
                    return;
                }
                try {
                    file.createNewFile();
                    continue;
                } catch (IOException e) {
                    Log.error(e);
                    bot.sendPrivateMsg(event.getUserId(), "创建失败", false);
                    return;
                }
            }
            json.put(key[i - 1], split[i]);
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("aiConfig/" + split[0] + ".json"))) {
            bw.write(json.toString());
        } catch (Exception e) {
            Log.error(e);
            bot.sendPrivateMsg(event.getUserId(), "创建失败", false);
            return;
        }
        bot.sendPrivateMsg(event.getUserId(), "配置："+split[0]+"创建成功", false);
    }


    /**
     * 被艾特强制聊天
     * @param bot
     * @param event
     */
    @Order(2)
    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NEED)
    public void gemini(Bot bot,GroupMessageEvent event){
        new Thread(()->{
            aichat(bot,event,true);
        }).start();
    }

    /**
     * ai聊天
     */
    public synchronized void aichat(Bot bot,GroupMessageEvent event,boolean b) {
        try {
            String post = "";
            post = new Gemini(event.getGroupId()).post();
            post = post.trim();
            if (post.equals("")) {
                throw new Exception("返回为空");
            }
            ActionData<MsgId> msgIdActionData = bot.sendGroupMsg(event.getGroupId(), post, false);
            event.setMessageId(msgIdActionData.getRetCode());
            event.setUserId(391459725L);
            event.getSender().setRole("member");
            event.getSender().setNickname("刑风OS");
            event.setMessage(post);
            Gemini.addMsg(event, "assistant",false);
        } catch (Exception e) {
            if (b) {
                bot.sendGroupMsg(event.getGroupId(), "似了", false);
            }
            Log.error(e);
        }

    }


    /**
     * 收到群信息就启用的方法
     * @param bot
     * @param event
     */
    @Order(1)
    @GroupMessageHandler
    public void 提示(Bot bot,GroupMessageEvent event) {
        Log.info(event.getMessage());
        String str = event.getMessage();
        String pattern = "^\\[CQ:image.*\\]||^\\[CQ:video.*\\]||^\\[CQ:json.*\\]||^\\[CQ:mface.*\\]||^\\[CQ:xml.*\\]||^\\[CQ:share.*\\]||^\\[CQ:music.*\\]||^\\[CQ:forward.*\\]";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        synchronized (this) {
            if (m.matches()) {
                //如果是图片
                if (str.startsWith("[CQ:image")) {
                    try {
                        event.setMessage("图片信息:\n"+new ImageAnalysis(event.getMessage()).analysis());
                    }catch (Exception e){
                        Log.error(e);
                    }
                    Gemini.addMsg(event, "user", true);
                }
//            pattern = "\\[CQ:image.*?url=([^,]+).*?\\]";
//            r = Pattern.compile(pattern);
//            m = r.matcher(str);
//            if (m.find()) {
//                String imageUrl = m.group(1);
//                imageUrl = imageUrl.replaceAll("&amp;","&");
//                try {
//                    String s = NetRequest.postFormUrl("http://120.25.164.240:5000", imageUrl);
//                    System.out.println(s);
//                    JSONObject jsonObject = new JSONObject(s);
//                    if (jsonObject.getInt("code")==1) {
//                        if (jsonObject.getBoolean("contains")) {
//                            bot.deleteMsg(event.getMessageId());
//                            bot.sendGroupMsg(event.getGroupId(), "检测到奶龙", false);
//                        }
//                    }
//                } catch (IOException e) {
//                    Log.warn(e);
//                }
//            }

            } else {
                new Wife().addWife(event.getUserId().toString(), event.getGroupId().toString());
                Gemini.addMsg(event, "user", true);
                new TimeTracker(event.getGroupId() + "").addTime();
                if (new TimeTracker(event.getGroupId() + "").isActive()) {
                    new Thread(() -> {
                        aichat(bot, event, false);
                    }).start();
                }
            }
        }
    }

    private JSONObject readJsonFile(File file) {
        String content = readFileToString(file);
        return new JSONObject(content);
    }

    private String readFileToString(File file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getPath(), e);
        }
        return sb.toString();
    }


}

