package text;
import javax.swing.*;
import java.awt.*;

public class InsetsExample {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Insets 示例");
        frame.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        // 设置内边距：上=10, 左=5, 下=10, 右=5
        gbc.insets = new Insets(10, 5, 10, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        frame.add(new JButton("按钮1"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        frame.add(new JButton("按钮2"), gbc);

        frame.pack();
        frame.setVisible(true);
    }
}