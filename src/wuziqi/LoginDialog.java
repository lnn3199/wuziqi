package wuziqi;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField; // 实现密码隐藏
    private JButton loginButton;
    private JButton registerButton;
    private UserService userService; // 用于处理用户登录与注册的服务类
    private User currentUser; // 记录当前登录的用户。
    private boolean loggedIn = false;

    public LoginDialog(Frame parent) {
        super(parent, "登录", true); // 第一个参数是父窗口，第二个是对话框标题，第三个是是否为静态对话框
        userService = new UserService();
        initializeComponents();
        layoutComponents();
        setupEventHandlers(); // 为按钮添加事件监听器，用于处理用户点击登录或注册按钮时的操作。
        setSize(300, 200);
        setLocationRelativeTo(parent); // 并使其在父窗口的中心位置显示。
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // 设置关闭操作为仅销毁当前对话框，不退出程序
    }

    private void initializeComponents() { // 创建对话框所需的所有Swing组件对象并初始化它们
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        loginButton = new JButton("登录");
        registerButton = new JButton("注册");
    }

    private void layoutComponents() { // 使用GridBagLayout布局管理器精确布置所有界面组件的位置，创建整齐的用户界面布局。
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(); // 创建GridBagConstraints对象，用于配置每个组件在网格布局中的约束条件
        gbc.insets = new Insets(5, 5, 5, 5); // 设置组件与单元格边界之间的内边距

        // 用户名
        gbc.gridx = 0;
        gbc.gridy = 0; // gridx列索引, gridy行索引
        add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        add(usernameField, gbc);

        // 密码
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("密码:"), gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; // 横跨列数
        add(buttonPanel, gbc);
    }

    private void setupEventHandlers() {
        loginButton.addActionListener(e -> performLogin());
        registerButton.addActionListener(e -> performRegister());

        // 回车键登录
        getRootPane().setDefaultButton(loginButton); // getRootPane()获取对话框的根面板，setDefaultButton()设置默认按钮。
    }

    private void performLogin() {
        String username = usernameField.getText().trim(); // 去除字符串两端的空白字符
        String password = new String(passwordField.getPassword()); // getPassword 返回的是 char[]

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名和密码", "登录错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = userService.login(username, password);
        if (user != null) {
            currentUser = user;
            loggedIn = true;
            dispose(); // 关闭登录对话框
        } else {
            JOptionPane.showMessageDialog(this, "用户名或密码错误", "登录失败", JOptionPane.ERROR_MESSAGE); // 消息类型（显示错误图标）
        }
    }

    private void performRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名和密码", "注册错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (userService.register(username, password)) {
            JOptionPane.showMessageDialog(this, "注册成功，请登录", "注册成功", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "注册失败，用户名可能已存在", "注册错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Getters
    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
}
