package com.boardflow.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="storyboard_projects")
class ProjectEntity {
    @Id UUID id;
    @Version long version;
    @Column(nullable=false, length=120) String title;
    @Column(nullable=false, length=2000) String description;
    Instant updatedAt;
    @ElementCollection(fetch=FetchType.LAZY)
    @CollectionTable(name="storyboard_shots", joinColumns=@JoinColumn(name="project_id"))
    @OrderColumn(name="sequence_position")
    List<ShotEntity> shots = new ArrayList<>();
}
