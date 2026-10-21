package com.csis;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Run Swing on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        // Create the main application window
        JFrame frame = new JFrame("Storyboard & Animatic Shot-List Sequencer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null); // Center on screen

        // Create a simple panel with a welcome message
        JPanel panel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome to the Storyboard Sequencer!", JLabel.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        
        panel.add(welcomeLabel, BorderLayout.CENTER);
        frame.add(panel);

        // Display the window
        frame.setVisible(true);
    }
}