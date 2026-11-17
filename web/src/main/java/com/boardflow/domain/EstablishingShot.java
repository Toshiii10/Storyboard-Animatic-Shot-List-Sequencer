package com.boardflow.domain;
import java.util.List;
public final class EstablishingShot extends StoryboardShot {
    public EstablishingShot(String notes) { super(notes); }
    @Override public List<String> getCameraSetupChecklist() {
        return List.of("Choose a wide lens to establish the location.", "Level the horizon and check your composition.",
                "Check foreground and background continuity.", "Lock the tripod or rehearse a slow camera move.");
    }
    @Override public int computeRecommendedDuration() { return 6; }
}
