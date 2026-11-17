package com.boardflow.domain;
import java.util.List;

/** Framework-independent rules for both the web UI and a future JavaFX UI. */
public abstract class StoryboardShot {
    public enum Type { ESTABLISHING, DIALOGUE, ACTION }
    protected final String notes;
    protected StoryboardShot(String notes) { this.notes = notes == null ? "" : notes; }
    public abstract List<String> getCameraSetupChecklist();
    public abstract int computeRecommendedDuration();
    public static StoryboardShot of(Type type, String notes) {
        return switch (type) {
            case ESTABLISHING -> new EstablishingShot(notes);
            case DIALOGUE -> new DialogueCloseUp(notes);
            case ACTION -> new ActionTrackingShot(notes);
        };
    }
}
