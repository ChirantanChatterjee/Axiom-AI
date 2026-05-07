package com.axiomai.ui;

import com.axiomai.math.solver.MathSolver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import com.axiomai.service.AIService;

public class ChatGUI extends JFrame {

    private JTextPane chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private final AIService aiService;

    public ChatGUI() {
        this.aiService = new AIService();
        setTitle("Math Expert AI CC");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Pastel palette
        Color pastelBackground = new Color(245, 240, 255);   // lavender
        Color pastelUser = new Color(255, 230, 240);         // pink bubble
        Color pastelAI = new Color(230, 250, 245);           // mint bubble
        Color pastelText = new Color(70, 70, 70);

        Font chatFont = new Font("Segoe UI", Font.PLAIN, 16);

        // Chat area (JTextPane allows styled text)
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(chatFont);
        chatArea.setBackground(Color.WHITE);
        chatArea.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(pastelBackground);
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Rounded input field
        inputField = new JTextField();
        inputField.setFont(chatFont);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(8, 10, 8, 10)
        ));

        // Send button
        sendButton = new JButton(" Send");
        sendButton.setFont(chatFont);
        sendButton.setBackground(new Color(255, 220, 240));
        sendButton.setFocusPainted(false);
        sendButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        sendButton.setIcon(UIManager.getIcon("OptionPane.informationIcon"));

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Listeners
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        getContentPane().setBackground(pastelBackground);

        setVisible(true);
    }

    private void appendBubble(String sender, String message, Color bubbleColor) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();

            // Bubble style
            SimpleAttributeSet bubble = new SimpleAttributeSet();
            StyleConstants.setBackground(bubble, bubbleColor);
            StyleConstants.setForeground(bubble, new Color(60, 60, 60));
            StyleConstants.setFontSize(bubble, 16);
            StyleConstants.setLeftIndent(bubble, 10);
            StyleConstants.setRightIndent(bubble, 10);
            StyleConstants.setSpaceAbove(bubble, 8);
            StyleConstants.setSpaceBelow(bubble, 8);
            StyleConstants.setLineSpacing(bubble, 0.2f);

            doc.insertString(doc.getLength(), sender + ": " + message + "\n", bubble);

        } catch (Exception ignored) {}
    }

    private void sendMessage() {
        String userText = inputField.getText();
        if (userText.isEmpty()) return;

        appendBubble("You", userText, new Color(255, 230, 240));

        String answer = aiService.process(userText);

        appendBubble("Math Expert AI CC", answer, new Color(230, 250, 245));

        inputField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatGUI::new);
    }
}
