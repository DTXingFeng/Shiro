package xyz.xingfeng.Shiro;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.log.LogMessage;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Shiro
@Component
public class 文游操作 {
    private static final Logger Log= LogManager.getLogger(文游操作.class);
    /**
     * 应战
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^排位对战|^对战",at = AtEnum.NEED)
    public void 应战(Bot bot, GroupMessageEvent event, Matcher matcher){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (BotState.PVP){
            return;
        }
        String action = matcher.group();
        String build = "";
        System.out.println(event.getMessage());
        switch (action){
            case "排位对战":
                build = MsgUtils.builder().text("排位对战").at(event.getUserId()).build();
                break;
            case "对战":
                build = MsgUtils.builder().text("对战").at(event.getUserId()).build();
                break;
        }
        bot.sendGroupMsg(event.getGroupId(),build,false);
        bot.sendGroupMsg(event.getGroupId(),"状态更新为：等待中",false);
        Log.info("状态更新为：等待中");
        BotState.等待 =true;
    }

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = ".*拥有队伍不能为0.*")
    public void 没有队伍(Bot bot, GroupMessageEvent event){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (BotState.等待) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
//            bot.sendGroupMsg(event.getGroupId(), "我需要创建队伍", false);
            Log.info("我需要创建队伍");
            bot.sendGroupMsg(event.getGroupId(),"创建阵容刑风OS",false);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            MsgUtils b = MsgUtils.builder().text("设置出战*刑风OS");
            String[] 创建队伍 = 创建队伍();
            for (int i = 0; i < 创建队伍.length; i++){
                b.text("*"+创建队伍[i]);
            }
            String build = b.build();
            bot.sendGroupMsg(event.getGroupId(),build,false);
            BotState.等待 = false;
        }
    }

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "更新阵容",senders = 2695570953L)
    public void 更新对战阵容(Bot bot, GroupMessageEvent event){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        MsgUtils b = MsgUtils.builder().text("设置出战刑风OS");
        String[] 创建队伍 = 创建队伍();
        for (int i = 0; i < 创建队伍.length; i++){
            b.text("*"+创建队伍[i]);
        }
        String build = b.build();
        bot.sendGroupMsg(event.getGroupId(),build,false);
    }


    public String[] 创建队伍(){
        File file = new File("干员图鉴.txt");
        List<String> list = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String temp = "";
            while ((temp = bufferedReader.readLine())!=null){
                if (temp.equals("")){
                    continue;
                }
                if (temp.contains("(限☆定)")){
                    String s = temp.replace("(限☆定)", "");
                    list.add(s);
                }else {
                    list.add(temp);
                }
            }
            System.out.println(list);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] s = new String[5];
        for (int i =0; i < 5; i++){
            int i1 = new Random().nextInt(0, list.size());
            s[i] = list.get(i1);
            list.remove(i1);
        }
        return s;

    }

    @GroupMessageHandler
    @MessageHandlerFilter(startWith = "发起成功，分配战场完毕")
    public void 进入战斗状态(Bot bot, GroupMessageEvent event){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (BotState.等待) {
            BotState.PVP = true;
            Log.info("状态更新为：战斗中");
            if (event.getMessage().contains("请双方出战阵容")) {
                bot.sendGroupMsg(event.getGroupId(), "出战阵容刑风OS", false);
            }
            BotState.等待 = false;
        }
    }


    /**
     * ban干员
     * @param bot
     * @param event
     * @param matcher
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "(?s).*刑风OS.*ban干员.*数量为\\s(.*)\\s个.*")
    public void ban干员(Bot bot,GroupMessageEvent event,Matcher matcher){
        bot.sendGroupMsg(event.getGroupId(),"我需要ban干员"+matcher.group(1)+"个",false);
    }

    /**
     * 选干员
     * @param bot
     * @param event
     * @param matcher
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "(?s).*刑风OS.*选干员.*数量为\\s(.*)\\s个.*")
    public void 选干员(Bot bot,GroupMessageEvent event,Matcher matcher){
        bot.sendGroupMsg(event.getGroupId(),"我需要选干员"+matcher.group(1)+"个",false);
    }

    /**
     * 准备阶段结束
     * @param bot
     * @param event
     * @param matcher
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "(?s).*-选择均已完毕.*")
    public void 准备阶段结束(Bot bot,GroupMessageEvent event,Matcher matcher){
        bot.sendGroupMsg(event.getGroupId(),"任意消息",false);
    }



    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "(?s).*刑风OS.*快速行动.*")
    public void 快速行动(Bot bot, GroupMessageEvent event, Matcher matcher){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (BotState.可用技能.equals(new ArrayList<>())){
            bot.sendGroupMsg(event.getGroupId(), "普攻", false);
        }else {
            String s = BotState.可用技能.get(new Random().nextInt(0, BotState.可用技能.size()));
            if (s.equals("0")){
                bot.sendGroupMsg(event.getGroupId(), "普攻", false);
            }else {
                bot.sendGroupMsg(event.getGroupId(), "技能"+s, false);
            }
        }
    }
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = ".*\\s1\\..*刑风OS.*\\s.*|.*\\s1\\..*刑风OS.*$")
    public void 我的回合(Bot bot, GroupMessageEvent event, Matcher matcher) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 适应角色提取的正则表达式
        String pattern = "(?<=-)(.*)";
        Pattern r = Pattern.compile(pattern);

        // 获取消息内容
        String message = event.getMessage();

        // 匹配
        Matcher m = r.matcher(message);
        StringBuilder sb = new StringBuilder();

        if (m.find()) {
            String character = m.group(1).trim();  // 提取角色名称
            sb.append("我的回合,当前使用角色: ").append(character);
        }

        // 如果有提取到角色，则发送消息
        if (sb.length() > 0) {
//            bot.sendGroupMsg(event.getGroupId(), sb.toString(), false);
            Log.info(sb.toString());
        } else {
            bot.sendGroupMsg(event.getGroupId(), "没有找到匹配的角色信息", false);
        }
        if (BotState.可用技能.equals(new ArrayList<>())){
            bot.sendGroupMsg(event.getGroupId(), "普攻", false);
        }else {
            String s = BotState.可用技能.get(new Random().nextInt(0, BotState.可用技能.size()));
            if (s.equals("0")){
                bot.sendGroupMsg(event.getGroupId(), "普攻", false);
            }else {
                bot.sendGroupMsg(event.getGroupId(), "技能"+s, false);
            }
        }
    }

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "(?s).*当前(\\d+)回合.*》刑风OS.*")
    public void 我的回合行动(Bot bot, GroupMessageEvent event, Matcher matcher){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        bot.sendGroupMsg(event.getGroupId(),"这是我回合的行为",false);
        //查看可用技能
        String message = event.getMessage();
        Pattern compile = Pattern.compile("(\\d)(?=.√ )",Pattern.MULTILINE);
        Matcher m = compile.matcher(message);
        List<String> list = new ArrayList<>();
        while (m.find()){
            list.add(m.group(1));
        }
        bot.sendGroupMsg(event.getGroupId(),"我可以使用的技能有："+list.toString(),false);
        Log.info("我可以使用的技能有："+list.toString());
        list.add("0");
        BotState.可用技能 = list;
    }

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "(?s).*刑风OS.*已为您替换干员.*-(.*)?-.*")
    public void 我的角色被切换(Bot bot, GroupMessageEvent event, Matcher matcher){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String message = event.getMessage();
        //从中筛选干员信息
        String pattern = "(?<=-)(.*)(?=-\\[)";
        // 创建 Pattern 对象
        Pattern r = Pattern.compile(pattern, Pattern.MULTILINE);
        // 现在创建 matcher 对象
        Matcher m = r.matcher(message);
        if (m.find()) {
//            bot.sendGroupMsg(event.getGroupId(), "我的角色被切换为:" + m.group(1), false);
            Log.info("我的角色被切换为:" + m.group(1));
        }
        r = Pattern.compile("(\\d+)\\.√(?!\\(被\\))",Pattern.MULTILINE);
        m = r.matcher(message);
        List<String> list = new ArrayList<>();
        while (m.find()){
            list.add(m.group(1));
        }
//        bot.sendGroupMsg(event.getGroupId(),"我可以使用的技能有："+list.toString(),false);
        Log.info("我可以使用的技能有："+list.toString());
        list.add("0");
        BotState.可用技能 = list;
    }

    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "(?s).*刑风OS.*对战结束.*")
    public void 对战结束(Bot bot, GroupMessageEvent event, Matcher matcher){
        bot.sendGroupMsg(event.getGroupId(),"我的对战结束了",false);
        BotState.PVP = false;
        BotState.可用技能 = new ArrayList<>();
    }

    /**
     * 主动更新
     * @param bot
     * @param event
     */
    @AnyMessageHandler
    @MessageHandlerFilter(cmd = "更新干员库",senders = {2695570953L})
    public void 更新干员库(Bot bot, AnyMessageEvent event){
        BotState.干员更新 = true;
        bot.sendGroupMsg(915912092L,"干员图鉴",false);
    }
    @GroupMessageHandler
    @MessageHandlerFilter(groups = {915912092L},startWith = "-昵称 『刑风OS』",senders = {2663121176L})
    public void 主动查询更新干员库(Bot bot, GroupMessageEvent event){
        if (!BotState.干员更新){
            return;
        }
        String message = event.getMessage();
        //从中筛选干员信息
        String pattern = "^(?!已收集|总收集)([^\\n=]+)=";

        // 创建 Pattern 对象
        Pattern r = Pattern.compile(pattern, Pattern.MULTILINE);

        // 现在创建 matcher 对象
        Matcher m = r.matcher(message);

        StringBuilder sb = new StringBuilder();

        // 查找匹配项
        while (m.find()) {
            sb.append(m.group(1).trim()).append("\n");
        }

        // 将匹配结果写入文件
        File file = new File("干员图鉴.txt");
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(sb.toString());
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("干员名字已提取并写入干员图鉴.txt文件");
        BotState.干员更新 = false;
    }
}
