package com.boardflow.web;
import com.boardflow.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/projects")
public class ProjectController {
    private final StoryboardService service;
    public ProjectController(StoryboardService service) { this.service=service; }
    public record CreateProject(@NotBlank @Size(max=120) String title, boolean demo) { }
    public record Summary(UUID id, long version, String title, String description, java.time.Instant updatedAt, int shotCount, int totalDuration) { }
    @GetMapping public List<Summary> list() {
        return service.list().stream().map(p -> new Summary(p.id(),p.version(),p.title(),p.description(),p.updatedAt(),p.shots().size(),p.totalDuration())).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProjectDocument create(@Valid @RequestBody CreateProject request) { return service.create(request.title(), request.demo()); }
    @GetMapping("/{id}") public ProjectDocument get(@PathVariable UUID id) { return service.get(id); }
    @PutMapping("/{id}") public ProjectDocument update(@PathVariable UUID id, @Valid @RequestBody ProjectDocument project) { return service.update(id,project); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { service.delete(id); }
}
