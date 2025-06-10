package xyz.xingfeng.Shiro.chat;

public class Msg {
    private Long groupId;
    private String msg;
    private String time;
    private String qq;
    private String name;

    public Msg(Long groupId, String msg, String time, String qq, String name) {
        this.groupId = groupId;
        this.msg = msg;
        this.time = time;
        this.qq = qq;
        this.name = name;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getMsg() {
        return msg;
    }

    public String getTime() {
        return time;
    }

    public String getQq() {
        return qq;
    }

    public String getName() {
        return name;
    }
    @Override
    public String toString(){
        return String.format("[%s][qq:%s]【%s】%s",time,qq,name,msg);
    }
}
