package com.csis;
import java.util.List;

public final class DialogueCloseUp extends StoryboardPanel {
    public DialogueCloseUp(ShotData shot, Runnable changed, Runnable up, Runnable down, Runnable delete) {
        super(shot, changed, up, down, delete);
    }
    @Override public List<String> getCameraSetupChecklist() {
        return List.of("Set focus on the speaker's eyes.", "Match eyelines and preserve the 180-degree axis.",
                "Check headroom and matching reverse-shot framing.", "Keep the microphone out of frame and record room tone.");
    }
    @Override public int computeRecommendedDuration() {
        int words = shot.notes.trim().isEmpty() ? 0 : shot.notes.trim().split("\\s+").length;
        return Math.min(3600, Math.max(3, (int) Math.ceil(words / 2.5) + 1));
    }
}
