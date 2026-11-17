package com.boardflow.application;
import com.boardflow.domain.StoryboardShot.Type;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service @Transactional
public class StoryboardService {
    private final ProjectRepository repository;
    public StoryboardService(ProjectRepository repository) { this.repository = repository; }
    public List<ProjectDocument> list() { return repository.findAll(); }
    public ProjectDocument get(UUID id) { return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Storyboard not found.")); }
    public ProjectDocument create(String title, boolean demo) {
        return repository.save(new ProjectDocument(UUID.randomUUID(),0,title,demo ? "A short film about the moments before everything changes." : "",Instant.now(),demo ? demoShots() : List.of(),0));
    }
    public ProjectDocument update(UUID id, ProjectDocument project) {
        get(id);
        if (!id.equals(project.id())) throw new IllegalArgumentException("Project ID does not match.");
        Set<UUID> ids = new HashSet<>();
        for (ProjectDocument.Shot shot : project.shots()) {
            if (!ids.add(shot.id())) throw new IllegalArgumentException("Each shot must have a unique ID.");
            if (!shot.image().isEmpty()) validateImage(shot.image());
        }
        return repository.save(project);
    }
    private void validateImage(String image) {
        if (!image.startsWith("data:image/jpeg;base64,") && !image.startsWith("data:image/png;base64,")) throw new IllegalArgumentException("Images must be PNG or JPEG.");
        try {
            byte[] decoded = Base64.getDecoder().decode(image.substring(image.indexOf(',') + 1));
            try (java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(decoded);
                 javax.imageio.stream.ImageInputStream stream = javax.imageio.ImageIO.createImageInputStream(in)) {
                Iterator<javax.imageio.ImageReader> readers = javax.imageio.ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw new IllegalArgumentException("Unreadable image.");
                javax.imageio.ImageReader reader = readers.next();
                try { reader.setInput(stream); if (reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096) throw new IllegalArgumentException("Image is too large."); }
                finally { reader.dispose(); }
            }
        } catch (java.io.IOException | IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid shot image. Upload a PNG or JPEG up to 4096 pixels per side."); }
    }
    public void delete(UUID id) { get(id); repository.delete(id); }
    private ProjectDocument.Shot shot(Type type, String title, String scene, String notes, String dialogue, int seconds, String illustration) {
        return new ProjectDocument.Shot(UUID.randomUUID(),type,title,scene,notes,dialogue,seconds,"Eye level",type==Type.ACTION ? "Tracking" : "Static","Cut","",illustration,0,List.of());
    }
    private List<ProjectDocument.Shot> demoShots() {
        return List.of(
            shot(Type.ESTABLISHING,"The city wakes","01 · EXT. CITY / DAWN","A quiet skyline before the first train. Hold the frame as the sun finds the rooftops.","",6,"city"),
            shot(Type.ACTION,"A familiar route","01 · EXT. STREET / DAWN","Track behind Maya. The city moves around her while she keeps her eyes on the station.","",8,"street"),
            shot(Type.DIALOGUE,"One last message","02 · INT. CAFE / MORNING","Close on Maya, lit by the window. Let the silence sit before she speaks.","If we leave now, we can still make it.",5,"cafe"),
            shot(Type.ESTABLISHING,"The platform","03 · EXT. STATION / MORNING","Wide shot. Two figures wait at opposite ends of the platform.","",6,"station"),
            shot(Type.DIALOGUE,"The decision","03 · EXT. STATION / MORNING","A tight reverse angle. Keep the eyeline consistent with the previous shot.","Then let's stop waiting.",3,"portrait"),
            shot(Type.ACTION,"Into the morning","03 · EXT. STATION / MORNING","Move with them as the train arrives. Hold their silhouettes in the closing frame.","",8,"train"));
    }
}
