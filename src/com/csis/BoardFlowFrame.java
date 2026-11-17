package com.csis;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class BoardFlowFrame extends JFrame {
    private final List<ShotData> shots = new ArrayList<>();
    private final JPanel sequence = new JPanel();
    private final JLabel summary = new JLabel();
    private final JLabel status = new JLabel("Ready. Build your story one shot at a time.");
    private final JButton preview = new JButton("Play animatic");
    private Path projectFile;
    private boolean dirty;

    public BoardFlowFrame() {
        super("BoardFlow");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1260, 860); setMinimumSize(new Dimension(1130, 650)); setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 18, 28));
        root.setBackground(new Color(23, 26, 33)); setContentPane(root);
        JPanel top = new JPanel(new BorderLayout(0, 20)); top.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout()); heading.setOpaque(false);
        JLabel brand = new JLabel("BoardFlow  /  Storyboard studio"); brand.setFont(brand.getFont().deriveFont(Font.BOLD, 26f));
        brand.setForeground(new Color(185, 197, 255)); heading.add(brand, BorderLayout.WEST);
        summary.setFont(summary.getFont().deriveFont(Font.BOLD, 17f)); heading.add(summary, BorderLayout.EAST);
        top.add(heading, BorderLayout.NORTH);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); toolbar.setOpaque(false);
        addButton(toolbar, "New", this::newProject); addButton(toolbar, "Open...", this::openProject);
        addButton(toolbar, "Save", () -> save(false)); addButton(toolbar, "Save as...", () -> save(true));
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        JComboBox<ShotData.Type> type = new JComboBox<>(ShotData.Type.values()); toolbar.add(type);
        addButton(toolbar, "+ Add shot", () -> addShot((ShotData.Type) type.getSelectedItem()));
        preview.addActionListener(e -> { if (commitEdits()) new AnimaticDialog(this, shots).setVisible(true); });
        toolbar.add(preview); top.add(toolbar, BorderLayout.CENTER);
        JLabel hint = new JLabel("PLAN YOUR SEQUENCE     /     Import frames, set camera angles, and shape the rhythm of your story.");
        hint.setForeground(new Color(157, 166, 184)); top.add(hint, BorderLayout.SOUTH); root.add(top, BorderLayout.NORTH);
        sequence.setLayout(new BoxLayout(sequence, BoxLayout.Y_AXIS)); sequence.setOpaque(false);
        JScrollPane scroll = new JScrollPane(sequence); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(root.getBackground()); scroll.getVerticalScrollBar().setUnitIncrement(24);
        root.add(scroll, BorderLayout.CENTER); root.add(status, BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { if (confirmDiscard()) dispose(); } });
        getRootPane().registerKeyboardAction(e -> save(false), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        seedDemo(); rebuild();
    }
    private void seedDemo() {
        ShotData a = new ShotData(ShotData.Type.ESTABLISHING); a.title = "01 / The city wakes"; a.duration = 6;
        a.notes = "EXT. CITY ROOFTOP - DAWN\nA wide view of the skyline. Let the audience settle into the world.";
        ShotData b = new ShotData(ShotData.Type.DIALOGUE); b.title = "02 / A quiet decision"; b.duration = 5;
        b.notes = "MAYA: If we leave now, we can still make it.\nHold on her reaction."; b.angle = "Over the shoulder";
        ShotData c = new ShotData(ShotData.Type.ACTION); c.title = "03 / Into the morning"; c.duration = 8;
        c.notes = "Track alongside Maya as she runs toward the station. Maintain screen direction."; c.transition = "Fade to black";
        shots.add(a); shots.add(b); shots.add(c);
    }
    private void addButton(JPanel toolbar, String label, Runnable action) {
        JButton button = new JButton(label); button.addActionListener(e -> action.run()); toolbar.add(button);
    }
    private void addShot(ShotData.Type type) {
        if (!commitEdits()) return;
        ShotData shot = new ShotData(type);
        StoryboardPanel panel = StoryboardPanel.create(shot, () -> {}, () -> {}, () -> {}, () -> {});
        shot.duration = panel.computeRecommendedDuration(); shots.add(shot); changed(); rebuild();
        SwingUtilities.invokeLater(() -> sequence.scrollRectToVisible(new Rectangle(0, sequence.getHeight() - 1, 1, 1)));
    }
    private void rebuild() {
        sequence.removeAll();
        for (ShotData shot : shots) {
            StoryboardPanel panel = StoryboardPanel.create(shot, this::changed,
                    () -> move(shot, -1), () -> move(shot, 1), () -> remove(shot));
            panel.setSequence(shots.indexOf(shot), shots.size()); panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            sequence.add(panel); sequence.add(Box.createVerticalStrut(14));
        }
        if (shots.isEmpty()) {
            JLabel empty = new JLabel("Your next story starts here. Choose a shot type and click + Add shot.");
            empty.setBorder(BorderFactory.createEmptyBorder(70, 24, 70, 24)); sequence.add(empty);
        }
        updateSummary(); sequence.revalidate(); sequence.repaint();
    }
    private void move(ShotData shot, int delta) {
        if (!commitEdits()) return;
        int i = shots.indexOf(shot), next = i + delta;
        if (next >= 0 && next < shots.size()) { Collections.swap(shots, i, next); changed(); rebuild(); }
    }
    private void remove(ShotData shot) {
        if (!commitEdits()) return;
        if (JOptionPane.showConfirmDialog(this, "Remove \"" + shot.title + "\"?", "Remove shot", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            shots.remove(shot); changed(); rebuild();
        }
    }
    private void changed() { dirty = true; updateSummary(); }
    private void updateSummary() {
        int total = shots.stream().mapToInt(s -> s.duration).sum();
        summary.setText(shots.size() + " shots    /    " + formatTime(total) + " runtime");
        preview.setEnabled(!shots.isEmpty());
        setTitle((dirty ? "* " : "") + "BoardFlow - " + (projectFile == null ? "Untitled storyboard" : projectFile.getFileName()));
    }
    static String formatTime(int seconds) { return String.format("%02d:%02d", seconds / 60, seconds % 60); }
    private boolean commitEdits() {
        // Spinner text is not committed by Swing until focus changes or Enter is pressed.
        return commitSpinners(sequence);
    }
    private boolean commitSpinners(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JSpinner) {
                try {
                    JSpinner spinner = (JSpinner) child; spinner.commitEdit();
                    int value = (Integer) spinner.getValue();
                    if (value < 1 || value > 3600) throw new java.text.ParseException("Out of range", 0);
                } catch (java.text.ParseException ex) {
                    JOptionPane.showMessageDialog(this, "Enter a duration from 1 to 3600 seconds."); child.requestFocusInWindow(); return false;
                }
            } else if (child instanceof Container && !commitSpinners((Container) child)) return false;
        }
        return true;
    }
    private boolean confirmDiscard() {
        if (!commitEdits()) return false;
        if (!dirty) return true;
        int answer = JOptionPane.showConfirmDialog(this, "Save your storyboard before continuing?", "Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION);
        return answer == JOptionPane.NO_OPTION || (answer == JOptionPane.YES_OPTION && save(false));
    }
    private void newProject() {
        if (!confirmDiscard()) return;
        shots.clear(); projectFile = null; dirty = false; rebuild(); status.setText("New storyboard. Add your first shot.");
    }
    private JFileChooser chooser() {
        JFileChooser chooser = new JFileChooser(); chooser.setFileFilter(new FileNameExtensionFilter("BoardFlow project (*.boardflow)", "boardflow"));
        if (projectFile != null) chooser.setSelectedFile(projectFile.toFile()); return chooser;
    }
    private void openProject() {
        if (!confirmDiscard()) return;
        JFileChooser chooser = chooser(); if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Path path = chooser.getSelectedFile().toPath(); List<ShotData> loaded = ProjectIO.load(path);
            shots.clear(); shots.addAll(loaded); projectFile = path; dirty = false; rebuild(); status.setText("Opened " + path.getFileName());
        } catch (IOException ex) { error(ex); }
    }
    private boolean save(boolean saveAs) {
        if (!commitEdits()) return false;
        Path target = projectFile;
        if (target == null || saveAs) {
            JFileChooser chooser = chooser(); if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false;
            target = chooser.getSelectedFile().toPath();
            if (!target.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".boardflow")) target = Path.of(target + ".boardflow");
            if (Files.exists(target) && JOptionPane.showConfirmDialog(this, "Replace " + target.getFileName() + "?", "Overwrite project", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return false;
        }
        try { ProjectIO.save(target, shots); projectFile = target; dirty = false; updateSummary(); status.setText("Saved " + target.getFileName()); return true; }
        catch (IOException ex) { error(ex); return false; }
    }
    private void error(Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Project error", JOptionPane.ERROR_MESSAGE); }
}
