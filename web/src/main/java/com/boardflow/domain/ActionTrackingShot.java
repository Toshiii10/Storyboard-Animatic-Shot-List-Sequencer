package com.boardflow.domain;
import java.util.List;
public final class ActionTrackingShot extends StoryboardShot {
    public ActionTrackingShot(String notes) { super(notes); }
    @Override public List<String> getCameraSetupChecklist() {
        return List.of("Walk the tracking path and clear obstacles.", "Balance the gimbal or check the dolly track.",
                "Rehearse movement, focus marks, and screen direction.", "Confirm safe clearance for the cast and crew.");
    }
    @Override public int computeRecommendedDuration() { return 8; }
}
