package com.csis;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("com.formdev.flatlaf.FlatDarkLaf").getMethod("setup").invoke(null);
            } catch (ReflectiveOperationException ignored) { }
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 8);
            new BoardFlowFrame().setVisible(true);
        });
    }

}
