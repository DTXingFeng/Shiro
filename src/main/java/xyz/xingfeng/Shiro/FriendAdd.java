package xyz.xingfeng.Shiro;

import com.mikuac.shiro.annotation.FriendAddNoticeHandler;
import com.mikuac.shiro.annotation.FriendAddRequestHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.request.FriendAddRequestEvent;
import org.springframework.stereotype.Component;

@Shiro
@Component
public class FriendAdd {

    @FriendAddNoticeHandler
    public void example(Bot bot, FriendAddRequestEvent event){
        //信息为"刑风"时，同意添加好友
        if ("刑风".equals(event.getComment())){
            bot.setFriendAddRequest(event.getFlag(), true,event.getRequestType());
        }
    }

}
