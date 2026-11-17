package com.csis;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Shared shot card; subclasses supply camera advice and recommended timing. */
public abstract class StoryboardPanel extends JPanel {
    protected final ShotData shot;
    private final Runnable changed;
    private final JLabel thumbnail = new JLabel("+  Import storyboard image", SwingConstants.CENTER);
    private final JLabel number = new JLabel();
    private final JSpinner duration;
    private final JButton up = new JButton("Up");
    private final JButton down = new JButton("Down");

    protected StoryboardPanel(ShotData shot, Runnable changed, Runnable moveUp, Runnable moveDown, Runnable delete) {
        this.shot = shot; this.changed = changed;
        setLayout(new BorderLayout(20, 12));
        setBackground(new Color(35, 39, 48));
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(62, 68, 80)),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        setPreferredSize(new Dimension(1080, 282));
        JPanel heading = row();
        number.setForeground(new Color(157, 175, 255));
        number.setFont(number.getFont().deriveFont(Font.BOLD, 15f));
        heading.add(number); heading.add(new JLabel("  /  " + shot.type));
        heading.add(Box.createHorizontalGlue()); heading.add(up); heading.add(down);
        JButton remove = new JButton("Remove"); heading.add(remove);
        up.addActionListener(e -> moveUp.run()); down.addActionListener(e -> moveDown.run());
        remove.addActionListener(e -> delete.run()); add(heading, BorderLayout.NORTH);
        JPanel imagePanel = new JPanel(new BorderLayout(0, 8)); imagePanel.setOpaque(false);
        thumbnail.setOpaque(true); thumbnail.setBackground(new Color(23, 26, 33));
        thumbnail.setForeground(new Color(157, 166, 184));
        thumbnail.setPreferredSize(new Dimension(240, 135));
        thumbnail.setBorder(BorderFactory.createLineBorder(new Color(72, 79, 94)));
        imagePanel.add(thumbnail, BorderLayout.CENTER);
        JButton imageButton = new JButton("Choose image..."); imageButton.addActionListener(e -> chooseImage());
        imagePanel.add(imageButton, BorderLayout.SOUTH); add(imagePanel, BorderLayout.WEST); refreshImage();
        JPanel editor = new JPanel(new BorderLayout(0, 10)); editor.setOpaque(false);
        JTextField title = new JTextField(shot.title); title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        listen(title, () -> shot.title = title.getText()); editor.add(title, BorderLayout.NORTH);
        JTextArea notes = new JTextArea(shot.notes, 3, 30); notes.setLineWrap(true); notes.setWrapStyleWord(true);
        notes.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); listen(notes, () -> shot.notes = notes.getText());
        JScrollPane notesScroll = new JScrollPane(notes);
        notesScroll.setBorder(BorderFactory.createTitledBorder("Dialogue / action notes"));
        editor.add(notesScroll, BorderLayout.CENTER);
        JPanel controls = row();
        JComboBox<String> angle = new JComboBox<>(ShotData.ANGLES); angle.setSelectedItem(shot.angle);
        angle.addActionListener(e -> { shot.angle = (String) angle.getSelectedItem(); changed.run(); });
        controls.add(new JLabel("Angle ")); controls.add(angle);
        duration = new JSpinner(new SpinnerNumberModel(shot.duration, 1, 3600, 1));
        duration.setPreferredSize(new Dimension(78, 28));
        duration.addChangeListener(e -> { shot.duration = (Integer) duration.getValue(); changed.run(); });
        controls.add(Box.createHorizontalStrut(10)); controls.add(new JLabel("Seconds ")); controls.add(duration);
        JComboBox<String> transition = new JComboBox<>(ShotData.TRANSITIONS); transition.setSelectedItem(shot.transition);
        transition.addActionListener(e -> { shot.transition = (String) transition.getSelectedItem(); changed.run(); });
        controls.add(Box.createHorizontalStrut(10)); controls.add(new JLabel("Exit ")); controls.add(transition);
        editor.add(controls, BorderLayout.SOUTH); add(editor, BorderLayout.CENTER);
        JPanel footer = row(); JButton checklist = new JButton("Camera checklist");
        checklist.addActionListener(e -> JOptionPane.showMessageDialog(this,
                String.join("\n", getCameraSetupChecklist()), shot.type + " checklist", JOptionPane.INFORMATION_MESSAGE));
        JButton recommend = new JButton("Use recommended timing");
        recommend.addActionListener(e -> duration.setValue(computeRecommendedDuration()));
        footer.add(checklist); footer.add(Box.createHorizontalStrut(8)); footer.add(recommend); add(footer, BorderLayout.SOUTH);
    }
    public abstract List<String> getCameraSetupChecklist();
    public abstract int computeRecommendedDuration();
    public static StoryboardPanel create(ShotData data, Runnable changed, Runnable up, Runnable down, Runnable delete) {
        switch (data.type) {
            case ESTABLISHING: return new EstablishingShot(data, changed, up, down, delete);
            case DIALOGUE: return new DialogueCloseUp(data, changed, up, down, delete);
            case ACTION: return new ActionTrackingShot(data, changed, up, down, delete);
            default: throw new IllegalArgumentException("Unknown shot type");
        }
    }
    void setSequence(int index, int count) {
        number.setText(String.format("SHOT %02d", index + 1));
        up.setEnabled(index > 0); down.setEnabled(index < count - 1);
    }
    private static JPanel row() {
        JPanel panel = new JPanel(); panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS)); return panel;
    }
    private void listen(javax.swing.text.JTextComponent text, Runnable update) {
        text.getDocument().addDocumentListener(new DocumentListener() {
            private void update() { update.run(); changed.run(); }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });
    }
    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Storyboard images (PNG, JPEG, GIF)", "png", "jpg", "jpeg", "gif"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage input = ImageIO.read(chooser.getSelectedFile());
            if (input == null) throw new IOException("Choose a supported image file.");
            double scale = Math.min(1, 1600.0 / Math.max(input.getWidth(), input.getHeight()));
            BufferedImage resized = new BufferedImage(Math.max(1, (int)(input.getWidth() * scale)),
                    Math.max(1, (int)(input.getHeight() * scale)), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics(); g.setColor(Color.BLACK);
            g.fillRect(0, 0, resized.getWidth(), resized.getHeight());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(input, 0, 0, resized.getWidth(), resized.getHeight(), null); g.dispose();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(resized, "png", bytes);
            shot.image = bytes.toByteArray(); refreshImage(); changed.run();
        } catch (IOException ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Image import failed", JOptionPane.ERROR_MESSAGE); }
    }
    private void refreshImage() {
        if (shot.image != null) {
            ImageIcon original = new ImageIcon(shot.image);
            double scale = Math.min(240.0 / original.getIconWidth(), 135.0 / original.getIconHeight());
            thumbnail.setText("");
            thumbnail.setIcon(new ImageIcon(original.getImage().getScaledInstance(Math.max(1, (int)(original.getIconWidth()*scale)),
                    Math.max(1, (int)(original.getIconHeight()*scale)), Image.SCALE_SMOOTH)));
        }
    }
}
