package com.csis;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Dependency-free checks; failures throw even without the -ea JVM option. */
public final class BoardFlowTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(BoardFlowTest::checkCards);
        Path file = Files.createTempFile("boardflow-test-", ".boardflow");
        try {
            ShotData a = new ShotData(ShotData.Type.ACTION);
            a.title = "Arrival <scene> & departure"; a.notes = "MAYA: Let's go!\n\u4f60\u597d \u2014 next scene";
            a.angle = "Low angle"; a.transition = "Dissolve"; a.duration = 12;
            BufferedImage image = new BufferedImage(32, 18, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(image, "png", bytes); a.image = bytes.toByteArray();
            ShotData b = new ShotData(ShotData.Type.DIALOGUE); b.duration = 3;
            List<ShotData> original = List.of(a, b);
            ProjectIO.save(file, original); List<ShotData> loaded = ProjectIO.load(file);
            check(loaded.size() == 2 && loaded.get(0).type == ShotData.Type.ACTION && loaded.get(1).type == ShotData.Type.DIALOGUE, "Shot order and types survive saving");
            ShotData copy = loaded.get(0);
            check(copy.title.equals(a.title) && copy.notes.equals(a.notes), "Unicode, XML characters and multiline notes round-trip");
            check(copy.duration == 12 && copy.angle.equals(a.angle) && copy.transition.equals(a.transition), "Timing and camera settings round-trip");
            check(Arrays.equals(copy.image, a.image), "Embedded image survives saving");
            check(AnimaticDialog.shotAt(original, 0) == 0 && AnimaticDialog.shotAt(original, 11.99) == 0, "First shot spans its exact duration");
            check(AnimaticDialog.shotAt(original, 12) == 1 && AnimaticDialog.shotAt(original, 15) == 1, "Shot boundary and finished preview select correct frame");
            check(BoardFlowFrame.formatTime(125).equals("02:05"), "Runtime formatting");
            ProjectIO.save(file, List.of()); check(ProjectIO.load(file).isEmpty(), "Empty storyboard saves and loads");
            ProjectIO.save(file, original);
            Properties invalid = new Properties();
            try (InputStream in = Files.newInputStream(file)) { invalid.loadFromXML(in); }
            invalid.setProperty("shot.0.duration", "0"); write(file, invalid); expectRejected(file);
            invalid.setProperty("shot.0.duration", "12"); invalid.setProperty("shot.0.image", "not-base64"); write(file, invalid); expectRejected(file);
            invalid.setProperty("version", "99"); write(file, invalid); expectRejected(file);
            Files.writeString(file, "not a BoardFlow project"); expectRejected(file);
        } finally { Files.deleteIfExists(file); }
        System.out.println("PASS: " + checks + " BoardFlow checks");
    }
    private static void checkCards() {
        Runnable noop = () -> {};
        for (ShotData.Type type : ShotData.Type.values()) {
            ShotData data = new ShotData(type);
            StoryboardPanel panel = StoryboardPanel.create(data, noop, noop, noop, noop);
            check(panel.getCameraSetupChecklist().size() >= 3, "Each shot has camera advice");
            int expected = type == ShotData.Type.ESTABLISHING ? 6 : type == ShotData.Type.ACTION ? 8 : 3;
            check(panel.computeRecommendedDuration() == expected, "Polymorphic duration for " + type);
            if (type == ShotData.Type.DIALOGUE) {
                data.notes = "one two three four five six seven eight nine ten";
                check(panel.computeRecommendedDuration() == 5, "Dialogue timing reflects word count");
            }
            panel.setSequence(0, 1); panel.setSize(1180, 282); layoutTree(panel);
            BufferedImage render = new BufferedImage(1180, 282, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = render.createGraphics(); panel.printAll(g); g.dispose();
            check(panel.getComponentCount() > 0, "Shot card can be rendered headlessly");
        }
    }
    private static void layoutTree(Container container) {
        container.doLayout(); for (Component c : container.getComponents()) if (c instanceof Container) layoutTree((Container)c);
    }
    private static void write(Path file, Properties properties) throws IOException {
        try (OutputStream out = Files.newOutputStream(file)) { properties.storeToXML(out, "test"); }
    }
    private static void expectRejected(Path file) throws IOException {
        boolean rejected = false;
        try { ProjectIO.load(file); } catch (IOException expected) { rejected = true; }
        check(rejected, "Invalid project rejected");
    }
}
