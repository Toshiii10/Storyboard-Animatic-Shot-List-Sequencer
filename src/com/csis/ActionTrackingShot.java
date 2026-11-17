package com.csis;
import java.util.List;

public final class ActionTrackingShot extends StoryboardPanel {
    public ActionTrackingShot(ShotData shot, Runnable changed, Runnable up, Runnable down, Runnable delete) {
        super(shot, changed, up, down, delete);
    }
    @Override public List<String> getCameraSetupChecklist() {
        return List.of("Walk the tracking path and remove obstacles.", "Balance the gimbal or check dolly movement.",
                "Rehearse blocking, focus marks, and screen direction.", "Confirm safe clearance for cast and camera crew.");
    }
    @Override public int computeRecommendedDuration() { return 8; }
}
