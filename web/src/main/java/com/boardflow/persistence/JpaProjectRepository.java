package com.boardflow.persistence;
import com.boardflow.application.*;
import com.boardflow.domain.StoryboardShot;
import jakarta.persistence.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Repository @Transactional
public class JpaProjectRepository implements ProjectRepository {
    @PersistenceContext private EntityManager em;
    @Override @Transactional(readOnly=true) public List<ProjectDocument> findAll() {
        return em.createQuery("select p from ProjectEntity p order by p.updatedAt desc", ProjectEntity.class)
                .getResultList().stream().map(this::document).toList();
    }
    @Override @Transactional(readOnly=true) public Optional<ProjectDocument> findById(UUID id) {
        return Optional.ofNullable(em.find(ProjectEntity.class, id)).map(this::document);
    }
    @Override public ProjectDocument save(ProjectDocument project) {
        ProjectEntity entity = em.find(ProjectEntity.class, project.id());
        boolean isNew = entity == null;
        if (isNew) { entity = new ProjectEntity(); entity.id = project.id(); }
        else if (entity.version != project.version()) throw new OptimisticLockingFailureException("This storyboard was changed in another tab.");
        entity.title = project.title(); entity.description = project.description(); entity.updatedAt = Instant.now();
        entity.shots.clear();
        for (ProjectDocument.Shot s : project.shots()) {
            ShotEntity row = new ShotEntity(); row.shotId=s.id(); row.type=s.type(); row.title=s.title(); row.scene=s.scene();
            row.notes=s.notes(); row.dialogue=s.dialogue(); row.duration=s.duration(); row.angle=s.angle();
            row.movement=s.movement(); row.transition=s.transition(); row.image=s.image(); row.illustration=s.illustration(); entity.shots.add(row);
        }
        if (isNew) em.persist(entity);
        em.flush(); return document(entity);
    }
    @Override public void delete(UUID id) { ProjectEntity entity = em.find(ProjectEntity.class, id); if (entity != null) em.remove(entity); }
    private ProjectDocument document(ProjectEntity e) {
        List<ProjectDocument.Shot> shots = e.shots.stream().map(s -> {
            StoryboardShot rules = StoryboardShot.of(s.type, s.dialogue);
            return new ProjectDocument.Shot(s.shotId,s.type,s.title,s.scene,s.notes,s.dialogue,s.duration,s.angle,s.movement,
                    s.transition,s.image,s.illustration,rules.computeRecommendedDuration(),rules.getCameraSetupChecklist());
        }).toList();
        return new ProjectDocument(e.id,e.version,e.title,e.description,e.updatedAt,shots,shots.stream().mapToInt(ProjectDocument.Shot::duration).sum());
    }
}
