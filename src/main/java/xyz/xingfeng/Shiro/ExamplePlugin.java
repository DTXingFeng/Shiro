package xyz.xingfeng.Shiro;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
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
import com.mikuac.shiro.enums.AtEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import xyz.xingfeng.Shiro.Tool.Static;
import xyz.xingfeng.Shiro.network.NetRequest;
import xyz.xingfeng.Shiro.service.CheckInService;
import xyz.xingfeng.Shiro.service.Gemini;
import xyz.xingfeng.Shiro.service.ImageService;
import xyz.xingfeng.Shiro.service.MinecraftServerPing;

import javax.security.auth.Subject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Shiro
@Component
public class ExamplePlugin {
    private static final Logger Log= LogManager.getLogger(ExamplePlugin.class);

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
        bot.sendGroupMsg(event.getGroupId(), builder.toString(), false);
    }
    //重置当前群记忆
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "重置记忆", senders = 2695570953L)
    public void resetMemory(Bot bot, GroupMessageEvent event) {
        Path chatHistoryDir = Paths.get("ChatHistory");
        Path filePath = chatHistoryDir.resolve(event.getGroupId() + ".json");
        File f = filePath.toFile();
        if (f.exists()) {
            f.delete();
        }
        bot.sendGroupMsg(event.getGroupId(), "记忆已重置", false);
    }

    /**
     * 守望赚钱催促器
     * @param bot
     * @param event
     */
    @GroupMessageHandler
    @MessageHandlerFilter(senders = 1093757211,groups = 915912092)
    public void shouwang(Bot bot, GroupMessageEvent event){
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
     * 今日老婆
     * @param bot
     * @param event\
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "今日老婆")
    public void todayLover(Bot bot, GroupMessageEvent event){
        //随机挑选一个群友当老婆
        ActionList<GroupMemberInfoResp> groupMemberList = bot.getGroupMemberList(event.getGroupId());
        GroupMemberInfoResp groupMemberInfoResp;
        while (true) {
            int i = new Random().nextInt(groupMemberList.getData().size());
            groupMemberInfoResp = groupMemberList.getData().get(i);
            if (groupMemberInfoResp.getUserId().equals(event.getUserId())){
                continue;
            }else {
                break;
            }
        }
        String msg = MsgUtils.builder()
                .at(event.getUserId())
                .text("今日老婆是:")
                .at(groupMemberInfoResp.getUserId())
                .img("https://q1.qlogo.cn/g?b=qq&nk="+groupMemberInfoResp.getUserId()+"&s=640")
                .build();
        bot.sendGroupMsg(event.getGroupId(),msg,false);
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
    @MessageHandlerFilter(cmd = "/图图.*",groups = 635985918)
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

    @PrivateMessageHandler
    public void example(Bot bot, PrivateMessageEvent event, Matcher matcher){
        Log.info("收到了消息"+event.getMessage());
        bot.sendPrivateMsg(2695570953L,"收到了来自"+event.getUserId()+" "+event.getPrivateSender().getNickname()+"的私聊消息,内容是:"+event.getMessage(),false);
    }

    @Order(2)
    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NEED)
    public void gemini(Bot bot,GroupMessageEvent event){
        int i = 0;
        while (true) {
            try {
                String post = new Gemini(event.getGroupId()).post();
                post = post.trim();
                Pattern compile = Pattern.compile("(?s)^\\[@(.*)\\].*");
                Matcher mat = compile.matcher(post);
                if (mat.find()) {
                    String qq = mat.group(1);
                    post = post.replaceAll("\\[@" + qq + "\\]", "");
                    String build = MsgUtils.builder().at(Long.parseLong(qq)).text(post).build();
                    bot.sendGroupMsg(event.getGroupId(), build, false);
                }else {
                    bot.sendGroupMsg(event.getGroupId(), post, false);
                }
                Gemini.addMsg(event.getGroupId(), "model", "model", post);
                return;
            } catch (Exception e) {
                if (i >= 1){
                    bot.sendGroupMsg(event.getGroupId(), "错误信息:" + e.toString(), false);
                    return;
                }
                i++;
            }
        }
    }

    @Order(1)
    @GroupMessageHandler
    public void 提示(Bot bot,GroupMessageEvent event){
        System.out.println(event.getMessage());
        String str = event.getMessage();
        String pattern = "^\\[CQ:image.*\\]";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        if(m.matches()){
            //收到了图片
            pattern = "\\[CQ:image.*?url=([^,]+).*?\\]";
            r = Pattern.compile(pattern);
            m = r.matcher(str);
            if (m.find()) {
                String imageUrl = m.group(1);
                imageUrl = imageUrl.replaceAll("&amp;","&");
                try {
//                  bot.sendGroupMsg(event.getGroupId(),imageUrl,false);

                    String s = NetRequest.postFormUrl("http://120.25.164.240:5000", imageUrl);
                    System.out.println(s);
                    JSONObject jsonObject = new JSONObject(s);
                    if (jsonObject.getInt("code")==1) {
                        if (jsonObject.getBoolean("contains")) {
                            bot.deleteMsg(event.getMessageId());
                            bot.sendGroupMsg(event.getGroupId(), "检测到奶龙", false);
                        }
                    }
                } catch (IOException e) {
                    Log.warn(e);
                }
            }

        } else {
            String msg = "";
            if (str.contains("[CQ:at,qq=391459725")){
                pattern = "(?<=\\[CQ:at.*\\]).*";
                r = Pattern.compile(pattern);
                m = r.matcher(str);

                if (m.find()){
                    msg = m.group();
                }
            }else {
                msg = str;
            }
            // 没收到图片
            Gemini.addMsg(event.getGroupId(),"user",event.getUserId().toString(),"[@"+event.getUserId()+"]:"+msg);
        }
    }




}

