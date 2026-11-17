package com.csis;
import java.util.List;

public final class EstablishingShot extends StoryboardPanel {
    public EstablishingShot(ShotData shot, Runnable changed, Runnable up, Runnable down, Runnable delete) {
        super(shot, changed, up, down, delete);
    }
    @Override public List<String> getCameraSetupChecklist() {
        return List.of("Use a wide lens to establish geography.", "Level the horizon and confirm the chosen camera angle.",
                "Check foreground, background, and location continuity.", "Lock the tripod or plan a slow controlled move.");
    }
    @Override public int computeRecommendedDuration() { return 6; }
}
