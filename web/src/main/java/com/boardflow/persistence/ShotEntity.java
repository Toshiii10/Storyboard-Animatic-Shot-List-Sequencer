package com.boardflow.persistence;
import com.boardflow.domain.StoryboardShot;
import jakarta.persistence.*;
import java.util.UUID;

@Embeddable
class ShotEntity {
    UUID shotId;
    @Enumerated(EnumType.STRING) StoryboardShot.Type type;
    @Column(length=120) String title;
    @Column(length=100) String scene;
    @Lob String notes;
    @Lob String dialogue;
    int duration;
    String angle;
    String movement;
    String transition;
    @Lob String image;
    String illustration;
}
