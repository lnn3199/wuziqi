package wuziqi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Stack;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class GomokuGame extends JFrame {
    private static final int BOARD_SIZE = 15; // 15x15棋盘
    private static final int CELL_SIZE = 40;  // 每个格子大小
    private static final int MARGIN = 30;     // 边距

    private ChessPiece[][] board = new ChessPiece[BOARD_SIZE][BOARD_SIZE];
    private boolean isBlackTurn = true;       // 黑子先手
    private Stack<Point> moveStack = new Stack<>(); // 记录落子位置用于悔棋
    private User currentUser;                 // 当前登录用户

    private JPanel chessBoard;
    private JLabel statusLabel;

    private Timer gameTimer; // 游戏计时器,每秒更新一次，显示总游戏时间和当前玩家的用时。
    private long gameStartTime;
    private long currentPlayerStartTime;
    private JLabel timerLabel;

    // ======== 数据库相关内部类 ========
    // 游戏记录实体类
    class GameRecordEntity {
        private String player1Name;
        private String player2Name;
        private String winnerName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<GameStepInfo> steps;

        public GameRecordEntity() {
            this.steps = new ArrayList<>();
            this.startTime = LocalDateTime.now();
        }

        // Getters and Setters
        public String getPlayer1Name() { return player1Name; }
        public void setPlayer1Name(String player1Name) { this.player1Name = player1Name; }

        public String getPlayer2Name() { return player2Name; }
        public void setPlayer2Name(String player2Name) { this.player2Name = player2Name; }

        public String getWinnerName() { return winnerName; }
        public void setWinnerName(String winnerName) { this.winnerName = winnerName; }

        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getStartTime() { return startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public List<GameStepInfo> getSteps() { return steps; }
        public void addStep(GameStepInfo step) { this.steps.add(step); }

        public int getStepsCount() { return steps.size(); }
    }

    // 游戏步骤信息类
    class GameStepInfo {
        private int row;
        private int col;
        private boolean isBlack;
        private LocalDateTime timestamp;

        public GameStepInfo(int row, int col, boolean isBlack) {
            this.row = row;
            this.col = col;
            this.isBlack = isBlack;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public int getRow() { return row; }
        public int getCol() { return col; }
        public boolean isBlack() { return isBlack; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    // ==================================

    // 当前游戏记录
    private GameRecordEntity currentGameRecord;

    public GomokuGame(User currentUser) {//接收登录用户信息，创建新游戏记录，初始化界面和计时器。
        this.currentUser = currentUser;
        this.currentGameRecord = new GameRecordEntity();

        currentGameRecord.setPlayer1Name(currentUser.getUsername());
        currentGameRecord.setPlayer2Name("玩家2");

        initUI();//调用方法初始化用户界面（创建窗口、棋盘、按钮等）
        initTimer();// 调用方法初始化游戏计时器
    }

    private void initUI() {
        setTitle("五子棋小游戏");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);// 设置关闭窗口时退出程序
        setResizable(false);// 禁止调整窗口大小

        // 初始化棋盘面板
        chessBoard = new ChessBoardPanel(); // 创建自定义棋盘面板
        int panelWidth = (BOARD_SIZE - 1) * CELL_SIZE + 2 * MARGIN;  // 14×40+60=620
        int panelHeight = (BOARD_SIZE - 1) * CELL_SIZE + 2 * MARGIN; // 14×40+60=620

        chessBoard.setPreferredSize(new Dimension(panelWidth, panelHeight));
        chessBoard.addMouseListener(new BoardMouseListener());

        // 使用面板组合两个标签
        JPanel topPanel = new JPanel(new BorderLayout());

        timerLabel = new JLabel("游戏时间: 00:00:00 | 当前玩家: 00:00", JLabel.CENTER);
        timerLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        timerLabel.setPreferredSize(new Dimension(600, 25));

        statusLabel = new JLabel("黑子先行", JLabel.CENTER);
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        statusLabel.setPreferredSize(new Dimension(600, 30));

        topPanel.add(timerLabel, BorderLayout.NORTH);
        topPanel.add(statusLabel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        JButton restartButton = new JButton("重新开始");
        JButton undoButton = new JButton("悔棋");
        JButton saveButton = new JButton("保存棋局");

        restartButton.addActionListener(e -> restartGame());
        undoButton.addActionListener(e -> undoMove());
        saveButton.addActionListener(e -> saveCurrentGame());

        buttonPanel.add(restartButton);
        buttonPanel.add(undoButton);
        buttonPanel.add(saveButton);

        // 布局
        add(chessBoard, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();  // 自动调整窗口大小以适合所有组件
        setLocationRelativeTo(null);// 将窗口显示在屏幕中央
    }

    // 棋盘绘制面板,专门用于绘制棋盘和棋子。
    class ChessBoardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawBoard(g);
            drawChessPieces(g);
        }

        private void drawBoard(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制背景
            g2d.setColor(new Color(209, 140, 78));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 计算棋盘绘制边界
            int boardRight = MARGIN + (BOARD_SIZE - 1) * CELL_SIZE;   // 右侧边界：30 + 14×40 = 590
            int boardBottom = MARGIN + (BOARD_SIZE - 1) * CELL_SIZE;  // 底部边界：30 + 14×40 = 590

            // 绘制网格线
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.5f));

            // 绘制15条垂直线（i从0到14）
            for (int i = 0; i < BOARD_SIZE; i++) {
                int x = MARGIN + i * CELL_SIZE;

                // 垂直线 - 从顶部MARGIN到底部边界
                g2d.drawLine(x, MARGIN, x, boardBottom);
            }

            // 绘制15条水平线（
            for (int j = 0; j < BOARD_SIZE; j++) {
                int y = MARGIN + j * CELL_SIZE;

                // 水平线 - 从左侧MARGIN到右侧边界
                g2d.drawLine(MARGIN, y, boardRight, y);
            }

            // 计算中心位置
            int centerX = MARGIN + 7 * CELL_SIZE;  // 第7条线（从0开始）
            int centerY = MARGIN + 7 * CELL_SIZE;  // 第7条线（从0开始）
            g2d.fillOval(centerX - 3, centerY - 3, 6, 6);

            //在棋盘的四个角附近绘制额外的标记点，
            int[][] starPoints = {{3,3}, {3,11}, {11,3}, {11,11}};
            for (int[] point : starPoints) {
                int starX = MARGIN + point[1] * CELL_SIZE;
                int starY = MARGIN + point[0] * CELL_SIZE;
                g2d.fillOval(starX - 3, starY - 3, 6, 6);
            }

            g2d.dispose(); // 释放Graphics2D对象
        }

        private void drawChessPieces(Graphics g) {// 绘制棋子
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (int row = 0; row < BOARD_SIZE; row++) {
                for (int col = 0; col < BOARD_SIZE; col++) {
                    if (board[row][col] != null) {//如果该位置有棋子
                        board[row][col].draw(g2d,//调用棋子的draw方法
                                MARGIN + col * CELL_SIZE,//对应棋盘交叉点
                                MARGIN + row * CELL_SIZE);
                    }
                }
            }

            g2d.dispose();
        }
    }

    // 鼠标点击事件处理
    class BoardMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            // 计算点击位置对应的棋盘坐标,将像素坐标转换为棋盘的行列坐标
            int col = Math.round((float)(x - MARGIN) / CELL_SIZE);
            int row = Math.round((float)(y - MARGIN) / CELL_SIZE);

            // 检查坐标有效性
            if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
                placeChess(row, col);
            }
        }
    }

    // 落子处理,处理落子的完整逻辑，包括检查位置、创建棋子、记录步骤、判断胜负、切换回合等。
    private void placeChess(int row, int col) {
        // 如果该位置已经有棋子，则不能落子
        if (board[row][col] != null) {
            return;
        }

        // 记录当前玩家用时
        long currentTime = System.currentTimeMillis();

        // 创建新棋子
        board[row][col] = new ChessPiece(isBlackTurn);
        moveStack.push(new Point(row, col)); // 将落子位置记录到栈中，用于悔棋

        // 记录这步棋
        currentGameRecord.addStep(new GameStepInfo(row, col, isBlackTurn));

        // 判断是否获胜
        if (checkWin(row, col)) {
            String winner = isBlackTurn ? "黑子" : "白子";
            String winnerName = isBlackTurn ? currentGameRecord.getPlayer1Name() : currentGameRecord.getPlayer2Name();
            currentGameRecord.setWinnerName(winnerName);
            currentGameRecord.setEndTime(LocalDateTime.now());

            // 停止计时器
            if (gameTimer != null) {
                gameTimer.stop();
            }

            JOptionPane.showMessageDialog(this, winner + "获胜！");// 显示获胜对话框
            saveCurrentGame();
            restartGame();
        } else {
            // 切换回合，重置当前玩家开始时间
            isBlackTurn = !isBlackTurn;
            currentPlayerStartTime = currentTime; // 重置计时
            statusLabel.setText(isBlackTurn ? "轮到黑子" : "轮到白子"); // 更新状态显示
        }

        // 重绘棋盘
        chessBoard.repaint();
    }

    // 胜负判断算法
    private boolean checkWin(int row, int col) {
        ChessPiece piece = board[row][col];
        if (piece == null) return false;

        // 四个方向检查: 水平、垂直、主对角线、副对角线
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

        for (int[] dir : directions) {
            int count = 1; // 包括当前棋子

            // 正向搜索
            for (int i = 1; i <= 4; i++) {
                int r = row + dir[0] * i;
                int c = col + dir[1] * i;
                if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE &&
                        board[r][c] != null && board[r][c].isBlack == piece.isBlack) {
                    count++;// 相同颜色棋子，计数增加
                } else {
                    break; // 遇到空位或不同颜色棋子，停止搜索
                }
            }

            // 反向搜索
            for (int i = 1; i <= 4; i++) {
                int r = row - dir[0] * i;
                int c = col - dir[1] * i;
                if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE &&
                        board[r][c] != null && board[r][c].isBlack == piece.isBlack) {
                    count++;
                } else {
                    break;
                }
            }

            if (count >= 5) {
                return true;
            }
        }

        return false;
    }

    // 初始化计时器，并在每秒更新显示当前游戏时间和当前玩家的时间。
    private void initTimer() {
        gameStartTime = System.currentTimeMillis();// 记录游戏开始时间
        currentPlayerStartTime = gameStartTime;// 当前玩家开始时间初始化为游戏开始时间

        if (timerLabel != null) {
            gameTimer = new Timer(1000, e -> updateTimerDisplay());// 创建每秒触发一次的计时器
            gameTimer.start();
        }
    }

    private void updateTimerDisplay() {//更新计时器显示
        long currentTime = System.currentTimeMillis();
        long totalGameTime = currentTime - gameStartTime;// 计算总游戏时间
        long currentPlayerTime = currentTime - currentPlayerStartTime; // 计算当前玩家用时

        String gameTimeStr = formatTime(totalGameTime);
        String playerTimeStr = formatTime(currentPlayerTime);
        String currentPlayer = isBlackTurn ? "黑子" : "白子";

        timerLabel.setText(String.format("游戏时间: %s | %s用时: %s",
                gameTimeStr, currentPlayer, playerTimeStr)); // 更新显示
    }

    private String formatTime(long milliseconds) {//格式化时间
        long seconds = milliseconds / 1000;// 将毫秒转换为秒
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;// 计算剩余秒数
        return String.format("%02d:%02d:%02d", hours, minutes, secs); // 格式化为"时:分:秒"
    }

    // 悔棋功能
    private void undoMove() {
        if (!moveStack.isEmpty()) {
            Point lastMove = moveStack.pop();
            board[lastMove.x][lastMove.y] = null; // 清空该位置的棋子
            isBlackTurn = !isBlackTurn;
            statusLabel.setText(isBlackTurn ? "轮到黑子" : "轮到白子");
            chessBoard.repaint();
        }
    }

    // 重新开始游戏
    private void restartGame() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }

        board = new ChessPiece[BOARD_SIZE][BOARD_SIZE];// 清空棋盘数组
        isBlackTurn = true;
        moveStack.clear();    // 清空落子记录栈

        // 创建新的游戏记录
        currentGameRecord = new GameRecordEntity();
        if (currentUser != null) {
            currentGameRecord.setPlayer1Name(currentUser.getUsername());
        } else {
            currentGameRecord.setPlayer1Name("玩家1");
        }
        currentGameRecord.setPlayer2Name("玩家2");

        statusLabel.setText("黑子先行");
        chessBoard.repaint();
        initTimer();
    }

    // 保存当前棋局到数据库
    private void saveCurrentGame() {
        if (currentGameRecord.getSteps().isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有棋局可保存！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            // 确保游戏记录有结束时间
            if (currentGameRecord.getEndTime() == null) {
                currentGameRecord.setEndTime(LocalDateTime.now());
            }

            // 保存到数据库
            saveGameToDatabase();

            JOptionPane.showMessageDialog(this,
                    "棋局记录已保存到数据库！",
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "保存失败: " + e.getMessage(),
                    "数据库错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // 保存游戏到数据库的方法
    private void saveGameToDatabase() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/gobangdb"
                + "?useSSL=false"
                + "&serverTimezone=Asia/Shanghai"
                + "&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.setAutoCommit(false); // 开始事务

            try {
                // 1. 插入游戏记录
                String insertGameSql = "INSERT INTO game_records " +
                        "(player1_name, player2_name, winner_name, start_time, end_time, steps_count) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement gameStmt = conn.prepareStatement(insertGameSql, Statement.RETURN_GENERATED_KEYS)) {//告诉JDBC要返回自动生成的键
                    gameStmt.setString(1, currentGameRecord.getPlayer1Name());
                    gameStmt.setString(2, currentGameRecord.getPlayer2Name());
                    gameStmt.setString(3, currentGameRecord.getWinnerName() != null ? currentGameRecord.getWinnerName() : "");
                    gameStmt.setTimestamp(4, Timestamp.valueOf(currentGameRecord.getStartTime()));
                    gameStmt.setTimestamp(5, Timestamp.valueOf(currentGameRecord.getEndTime()));
                    gameStmt.setInt(6, currentGameRecord.getStepsCount());

                    gameStmt.executeUpdate();

                    // 获取生成的游戏ID
                    try (ResultSet rs = gameStmt.getGeneratedKeys()) {//这个方法返回一个 ResultSet 对象，包含数据库自动生成的主键值
                        int gameId = -1;
                        if (rs.next()) {//移动到结果集第一行并获取ID
                            gameId = rs.getInt(1); // 获取自增主键ID,获取第一列的值
                        }

                        if (gameId == -1) {
                            throw new SQLException("Failed to get generated game ID");
                        }

                        // 2. 插入所有游戏步骤
                        String insertStepSql = "INSERT INTO game_steps " +
                                "(game_id, step_number, row_pos, col_pos, piece_color, step_time) " +
                                "VALUES (?, ?, ?, ?, ?, ?)";

                        try (PreparedStatement stepStmt = conn.prepareStatement(insertStepSql)) {
                            List<GameStepInfo> steps = currentGameRecord.getSteps();
                            for (int i = 0; i < steps.size(); i++) {
                                GameStepInfo step = steps.get(i);
                                stepStmt.setInt(1, gameId);
                                stepStmt.setInt(2, i + 1);
                                stepStmt.setInt(3, step.getRow());
                                stepStmt.setInt(4, step.getCol());
                                stepStmt.setString(5, step.isBlack() ? "BLACK" : "WHITE");
                                stepStmt.setTimestamp(6, Timestamp.valueOf(step.getTimestamp()));
                                stepStmt.addBatch();  // 添加到批处理
                            }
                            stepStmt.executeBatch();// 执行批处理，一次性插入所有步骤
                        }
                    }
                }

                // 提交事务
                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // 棋子类
    class ChessPiece {
        boolean isBlack;

        ChessPiece(boolean isBlack) {
            this.isBlack = isBlack;
        }

        void draw(Graphics2D g2d, int x, int y) {
            // 绘制棋子阴影效果
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval(x - CELL_SIZE/2 + 2, y - CELL_SIZE/2 + 2, CELL_SIZE - 4, CELL_SIZE - 4);

            // 绘制棋子本体
            if (isBlack) {
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(Color.WHITE);
            }
            g2d.fillOval(x - CELL_SIZE/2 + 1, y - CELL_SIZE/2 + 1, CELL_SIZE - 4, CELL_SIZE - 4);

            // 绘制边框
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x - CELL_SIZE/2 + 1, y - CELL_SIZE/2 + 1, CELL_SIZE - 4, CELL_SIZE - 4);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { // 把 GUI 创建任务放到事件调度线程(EDT)执行，保证界面流畅
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());// 设置程序外观为系统默认风格，比如 Windows 风格
            } catch (Exception e) {
                e.printStackTrace();// 如果设置失败，打印错误但不中断程序
            }

            // 先显示登录对话框
            LoginDialog loginDialog = new LoginDialog(null);
            loginDialog.setVisible(true);

            // 如果登录成功，启动游戏主窗口
            if (loginDialog.isLoggedIn()) {
                GomokuGame game = new GomokuGame(loginDialog.getCurrentUser());
                game.setVisible(true);
            }
        });
    }
}