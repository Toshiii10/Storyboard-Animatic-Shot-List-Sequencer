package com.csis;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Versioned XML properties, with embedded thumbnails and atomic replacement. */
public final class ProjectIO {
    private ProjectIO() { }
    public static void save(Path file, List<ShotData> shots) throws IOException {
        Properties p = new Properties(); p.setProperty("version", "1");
        p.setProperty("count", Integer.toString(shots.size()));
        for (int i = 0; i < shots.size(); i++) {
            ShotData s = shots.get(i); String key = "shot." + i + ".";
            p.setProperty(key + "type", s.type.name()); p.setProperty(key + "title", s.title);
            p.setProperty(key + "angle", s.angle); p.setProperty(key + "transition", s.transition);
            p.setProperty(key + "notes", s.notes); p.setProperty(key + "duration", Integer.toString(s.duration));
            if (s.image != null) p.setProperty(key + "image", Base64.getEncoder().encodeToString(s.image));
        }
        Path target = file.toAbsolutePath();
        Path temp = Files.createTempFile(target.getParent(), "boardflow-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) { p.storeToXML(out, "BoardFlow project", "UTF-8"); }
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public static List<ShotData> load(Path file) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) { p.loadFromXML(in); }
        try {
            if (!"1".equals(p.getProperty("version"))) throw new IllegalArgumentException("Unsupported project version.");
            int count = Integer.parseInt(required(p, "count"));
            if (count < 0 || count > 10000) throw new IllegalArgumentException("Invalid shot count.");
            List<ShotData> shots = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String key = "shot." + i + ".";
                ShotData s = new ShotData(ShotData.Type.valueOf(required(p, key + "type")));
                s.title = required(p, key + "title"); s.notes = required(p, key + "notes");
                s.angle = required(p, key + "angle"); s.transition = required(p, key + "transition");
                s.duration = Integer.parseInt(required(p, key + "duration"));
                if (s.duration < 1 || s.duration > 3600) throw new IllegalArgumentException("Duration must be 1-3600 seconds.");
                if (!Arrays.asList(ShotData.ANGLES).contains(s.angle) || !Arrays.asList(ShotData.TRANSITIONS).contains(s.transition))
                    throw new IllegalArgumentException("Unknown camera angle or transition.");
                if (p.containsKey(key + "image")) {
                    s.image = Base64.getDecoder().decode(p.getProperty(key + "image"));
                    if (ImageIO.read(new ByteArrayInputStream(s.image)) == null) throw new IllegalArgumentException("Invalid shot image.");
                }
                shots.add(s);
            }
            return shots;
        } catch (IllegalArgumentException ex) { throw new IOException("Invalid BoardFlow project: " + ex.getMessage(), ex); }
    }
    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Missing " + key);
        return value;
    }
}
