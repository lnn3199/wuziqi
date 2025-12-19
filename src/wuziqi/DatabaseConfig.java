package wuziqi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String URL =
            "jdbc:mysql://localhost:3306/gobangdb"
                    + "?useSSL=false"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&allowPublicKeyRetrieval=true";

    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
