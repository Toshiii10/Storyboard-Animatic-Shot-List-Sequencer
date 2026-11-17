package com.boardflow.application;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port keeps application logic independent of the database adapter. */
public interface ProjectRepository {
    List<ProjectDocument> findAll();
    Optional<ProjectDocument> findById(UUID id);
    ProjectDocument save(ProjectDocument project);
    void delete(UUID id);
}
