package wuziqi;

import java.sql.*;

public class UserService {

    // 用户登录验证（明文密码）
    public User login(String username, String password) {
        String sql = "SELECT id, username FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // 执行重复操作时,用这个创建 pre 对象
            // 预编译 sql 语句，? 是占位符
            stmt.setString(1, username); // 为占位符设置参数
            stmt.setString(2, password); // 直接使用明文密码

            try (ResultSet rs = stmt.executeQuery()) { // 执行 select 语句并获取查询结果，返回一张虚拟表
                if (rs.next()) { // 将游标移到第一行，下一行不存在，返回 false
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // jdbc 代码遇到错误，将这个异常的详细信息和调用堆栈路径打印到标准错误流
        }
        return null;
    }

    // 用户注册（明文密码）
    public boolean register(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // 直接存储明文密码

            int rowsAffected = stmt.executeUpdate(); // 用于修改数据库内容，不返回结果集，返回操作影响的行数
            return rowsAffected > 0;
        } catch (SQLException e) {
            // 检查是否是重复用户名错误
            if (e.getMessage().contains("Duplicate") || e.getErrorCode() == 1062) { // 查描述中是否包含 "Duplicate" 这个词，MySQL 数据库规定的 "重复条目" 错误代码
                return false; // 用户名已存在
            }
            e.printStackTrace();
            return false;
        }
    }
}
