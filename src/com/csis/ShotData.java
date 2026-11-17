package com.csis;

/** Project data is independent of Swing, so it can be saved and tested. */
public final class ShotData {
    public enum Type {
        ESTABLISHING("Establishing shot"), DIALOGUE("Dialogue close-up"), ACTION("Action tracking shot");
        private final String label;
        Type(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    public static final String[] ANGLES = {"Eye level", "High angle", "Low angle", "Bird's eye", "Dutch angle", "Over the shoulder"};
    public static final String[] TRANSITIONS = {"Cut", "Dissolve", "Fade to black"};
    public final Type type;
    public String title = "Untitled shot";
    public String angle = ANGLES[0];
    public String transition = TRANSITIONS[0];
    public String notes = "";
    public int duration = 5;
    public byte[] image;
    public ShotData(Type type) { this.type = type; }
}
