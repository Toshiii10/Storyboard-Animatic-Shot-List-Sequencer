package com.boardflow.domain;
import java.util.List;
public final class DialogueCloseUp extends StoryboardShot {
    public DialogueCloseUp(String notes) { super(notes); }
    @Override public List<String> getCameraSetupChecklist() {
        return List.of("Focus on the speaker's eyes.", "Match eyelines and respect the 180-degree axis.",
                "Leave consistent headroom across reverse shots.", "Check microphone placement and record room tone.");
    }
    @Override public int computeRecommendedDuration() {
        int words = notes.isBlank() ? 0 : notes.strip().split("\\s+").length;
        return Math.min(3600, Math.max(3, (int)Math.ceil(words / 2.5) + 1));
    }
}
