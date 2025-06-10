package xyz.xingfeng.Shiro.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteConnection {
    private static String dbPath = "data.db"; // 默认数据库路径

    // 私有构造方法防止实例化
    private SQLiteConnection() {}

    /**
     * 设置数据库路径
     * @param path 数据库文件路径
     */
    public static void setDbPath(String path) {
        dbPath = path;
    }


    /**
     * 获取数据库连接
     * @return Connection对象
     * @throws SQLException 如果连接失败
     */
    public static Connection getConnection() throws SQLException {
        try {
            // 加载SQLite JDBC驱动
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }

        // 创建连接字符串
        String url = "jdbc:sqlite:" + dbPath;

        // 获取连接
        return DriverManager.getConnection(url);
    }

    /**
     * 关闭连接
     * @param conn 要关闭的连接
     */
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Failed to close connection: " + e.getMessage());
            }
        }
    }

    /**
     * 关闭多个资源对象
     * @param resources 可变参数，可传入多个AutoCloseable对象
     */
    public static void closeResources(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("Failed to close resource: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 测试连接是否有效
     * @return true如果连接成功，false如果失败
     */
    public static boolean testConnection() {
        Connection conn = null;
        try {
            conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        } finally {
            close(conn);
        }
    }
}
