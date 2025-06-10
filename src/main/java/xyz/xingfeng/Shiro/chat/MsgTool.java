package xyz.xingfeng.Shiro.chat;

import xyz.xingfeng.Shiro.Config.SQLiteConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class MsgTool {
    private Long groupId;
    public MsgTool(Long groupId){
        this.groupId = groupId;
    }

    //存入消息
    public void addMsg(Msg msg){
        try {
            Connection connection = SQLiteConnection.getConnection();
            String sql = "INSERT INTO groupMsg (groupId, msg, time, qq, name) VALUES (?, ?, ?, ?, ?)";
            try (var preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setLong(1, msg.getGroupId());
                preparedStatement.setString(2, msg.getMsg());
                preparedStatement.setString(3, msg.getTime());
                preparedStatement.setString(4, msg.getQq());
                preparedStatement.setString(5, msg.getName());
                preparedStatement.executeUpdate();
            } finally {
                SQLiteConnection.close(connection);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //获取最近的消息
    public Msg getLastMsg() {
        try {
            Connection connection = SQLiteConnection.getConnection();
            String sql = "SELECT * FROM groupMsg WHERE groupId = ? ORDER BY time DESC LIMIT 1";
            try (var preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setLong(1, groupId);
                var resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return new Msg(
                        resultSet.getLong("groupId"),
                        resultSet.getString("msg"),
                        resultSet.getString("time"),
                        resultSet.getString("qq"),
                        resultSet.getString("name")
                    );
                }
            } finally {
                SQLiteConnection.close(connection);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null; // 如果没有找到消息，返回null
    }

    //获取近几条消息
    public Msg[] getRecentMsgs(int count) {
        try {
            Connection connection = SQLiteConnection.getConnection();
            String sql = "SELECT * FROM groupMsg WHERE groupId = ? ORDER BY time DESC LIMIT ?";
            try (var preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setLong(1, groupId);
                preparedStatement.setInt(2, count);
                var resultSet = preparedStatement.executeQuery();

                // 使用ArrayList来存储结果
                java.util.List<Msg> msgs = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    msgs.add(new Msg(
                        resultSet.getLong("groupId"),
                        resultSet.getString("msg"),
                        resultSet.getString("time"),
                        resultSet.getString("qq"),
                        resultSet.getString("name")
                    ));
                }
                return msgs.toArray(new Msg[0]); // 转换为数组返回
            } finally {
                SQLiteConnection.close(connection);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
