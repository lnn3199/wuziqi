package text;

import javax.swing.*;
import java.awt.*;

public class GridWidthRemainderExample {
    public static void main(String[] args) {
        JFrame frame = new JFrame("REMAINDER 示例");
        frame.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 第一行：两个正常按钮
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 1;
        frame.add(new JButton("按钮1"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.gridwidth = 1;
        frame.add(new JButton("按钮2"), gbc);
        
        // 第二行：一个占据剩余所有列的按钮
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER; // 占据剩余所有列
        frame.add(new JButton("占据整行的按钮"), gbc);
        
        // 第三行：正常按钮
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 1;
        frame.add(new JButton("按钮3"), gbc);
        
        // 第四行：又一個占据整行的组件
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        frame.add(new JTextField("整行的文本字段"), gbc);
        
        frame.pack();
        frame.setVisible(true);
    }
}