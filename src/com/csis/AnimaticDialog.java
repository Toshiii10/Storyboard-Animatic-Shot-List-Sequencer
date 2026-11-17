package com.csis;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;

/** Silent, timed storyboard preview. Transitions occupy the end of each shot. */
public final class AnimaticDialog extends JDialog {
    private final List<ShotData> shots;
    private final Image[] images;
    private final int total;
    private final JLabel caption = new JLabel();
    private final JTextArea notes = new JTextArea(3, 40);
    private final JProgressBar progress;
    private final JButton play = new JButton("Pause");
    private final Timer timer;
    private long started = System.nanoTime();
    private double elapsed;
    private boolean playing = true;
    private int current = -1;
    private final JPanel canvas = new JPanel() {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int index = shotAt(shots, elapsed);
            drawShot(g, index, 1f);
            double start = 0; for (int i = 0; i < index; i++) start += shots.get(i).duration;
            ShotData shot = shots.get(index);
            float mix = (float) Math.max(0, Math.min(1, (elapsed - start - shot.duration + 0.6) / 0.6));
            if (mix > 0 && !"Cut".equals(shot.transition)) {
                if ("Dissolve".equals(shot.transition) && index + 1 < shots.size()) drawShot(g, index + 1, mix);
                else { g.setComposite(AlphaComposite.SrcOver.derive(mix)); g.setColor(Color.BLACK); g.fillRect(0, 0, getWidth(), getHeight()); }
            }
            g.dispose();
        }
        private void drawShot(Graphics2D g, int index, float opacity) {
            if (opacity < 1f) {
                // Composite the entire next frame once, including its letterbox bars.
                java.awt.image.BufferedImage layer = new java.awt.image.BufferedImage(
                        getWidth(), getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                Graphics2D next = layer.createGraphics();
                next.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                drawShot(next, index, 1f); next.dispose();
                g.setComposite(AlphaComposite.SrcOver.derive(opacity));
                g.drawImage(layer, 0, 0, null); return;
            }
            g.setComposite(AlphaComposite.SrcOver.derive(opacity));
            g.setColor(Color.BLACK); g.fillRect(0, 0, getWidth(), getHeight());
            Image image = images[index];
            if (image == null) {
                g.setColor(new Color(31, 35, 45)); g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(185, 197, 255)); g.setFont(getFont().deriveFont(Font.BOLD, 28f));
                String text = "SHOT " + (index + 1) + " / " + shots.get(index).type;
                g.drawString(text, Math.max(20, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2), getHeight() / 2);
                g.setFont(getFont().deriveFont(15f)); g.setColor(Color.LIGHT_GRAY);
                g.drawString("Import a storyboard image to preview this frame.", 30, getHeight() / 2 + 40);
            } else {
                double scale = Math.min((double)getWidth() / image.getWidth(null), (double)getHeight() / image.getHeight(null));
                int w = (int)(image.getWidth(null)*scale), h = (int)(image.getHeight(null)*scale);
                g.drawImage(image, (getWidth()-w)/2, (getHeight()-h)/2, w, h, null);
            }
        }
    };

    public AnimaticDialog(JFrame owner, List<ShotData> shots) {
        super(owner, "BoardFlow / Animatic preview", true);
        if (shots.isEmpty()) throw new IllegalArgumentException("Add a shot before previewing.");
        this.shots = shots; images = new Image[shots.size()];
        total = shots.stream().mapToInt(s -> s.duration).sum();
        for (int i = 0; i < shots.size(); i++) if (shots.get(i).image != null) images[i] = new ImageIcon(shots.get(i).image).getImage();
        setSize(960, 720); setMinimumSize(new Dimension(700, 550)); setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(12, 12)); root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(root); canvas.setBackground(Color.BLACK); root.add(canvas, BorderLayout.CENTER);
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 17f)); root.add(caption, BorderLayout.NORTH);
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        notes.setEditable(false); notes.setLineWrap(true); notes.setWrapStyleWord(true);
        bottom.add(new JScrollPane(notes), BorderLayout.NORTH);
        progress = new JProgressBar(0, total * 100); progress.setStringPainted(true); bottom.add(progress, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton restart = new JButton("Restart");
        restart.addActionListener(e -> { elapsed = 0; started = System.nanoTime(); playing = true; play.setText("Pause"); updateDisplay(); });
        play.addActionListener(e -> {
            if (playing) tick();
            if (elapsed >= total) { elapsed = 0; current = -1; }
            playing = !playing; started = System.nanoTime() - (long)(elapsed * 1_000_000_000L);
            play.setText(playing ? "Pause" : "Play"); updateDisplay();
        });
        controls.add(restart); controls.add(play);
        controls.add(new JLabel("Silent preview / transitions included in shot duration")); bottom.add(controls, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);
        timer = new Timer(33, e -> tick());
        addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { timer.stop(); } });
        updateDisplay(); timer.start();
    }
    private void tick() {
        if (!playing) return;
        elapsed = Math.min(total, (System.nanoTime() - started) / 1_000_000_000.0);
        if (elapsed >= total) { playing = false; play.setText("Play again"); }
        updateDisplay();
    }
    private void updateDisplay() {
        int index = shotAt(shots, elapsed);
        if (current != index) {
            current = index; ShotData s = shots.get(index);
            caption.setText(String.format("%02d / %02d   %s   |   %s   |   %s", index + 1, shots.size(), s.title, s.angle, s.transition));
            notes.setText(s.notes); notes.setCaretPosition(0);
        }
        progress.setValue((int)(elapsed * 100));
        progress.setString(BoardFlowFrame.formatTime((int)elapsed) + " / " + BoardFlowFrame.formatTime(total)); canvas.repaint();
    }
    static int shotAt(List<ShotData> shots, double seconds) {
        double end = 0;
        for (int i = 0; i < shots.size(); i++) { end += shots.get(i).duration; if (seconds < end) return i; }
        return shots.size() - 1;
    }
}
