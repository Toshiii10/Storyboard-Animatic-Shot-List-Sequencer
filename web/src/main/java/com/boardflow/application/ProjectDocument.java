package com.boardflow.application;
import com.boardflow.domain.StoryboardShot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectDocument(@NotNull UUID id, @Min(0) long version, @NotBlank @Size(max=120) String title,
        @NotNull @Size(max=2000) String description, Instant updatedAt,
        @NotNull @Size(max=250) List<@NotNull @Valid Shot> shots, int totalDuration) {
    public record Shot(@NotNull UUID id, @NotNull StoryboardShot.Type type,
            @NotBlank @Size(max=120) String title, @NotNull @Size(max=100) String scene,
            @NotNull @Size(max=20000) String notes, @NotNull @Size(max=20000) String dialogue,
            @Min(1) @Max(3600) int duration,
            @NotNull @Pattern(regexp="Eye level|High angle|Low angle|Bird's eye|Dutch angle|Over the shoulder") String angle,
            @NotNull @Pattern(regexp="Static|Pan|Tilt|Dolly|Tracking|Handheld") String movement,
            @NotNull @Pattern(regexp="Cut|Dissolve|Fade to black") String transition,
            @NotNull @Size(max=3000000) String image, @NotNull @Size(max=24) String illustration,
            int recommendedDuration, List<String> checklist) { }
}
