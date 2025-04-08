package nl.lunarflow.models;

import java.time.Instant;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
// import org.hibernate.validator.constraints.URL;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table
public class ContentItem extends PanacheEntity {

    @NotBlank(message = "Title cannot be blank.")
    public String title;

    @NotBlank(message = "Subject cannot be blank.")
    public String subject;

    @NotBlank(message = "Topic cannot be blank")
    public String topic;

    // project id (Project model)
    @ManyToOne(fetch = FetchType.EAGER)
    public Project project;

    @Future(message = "Publication date must be in the future.")
    public Instant publicationDate;

    // @URL(protocol = "https")
    public String link;
    
    // @URL(protocol = "https")
    public String gitlabIssueUrl;

    @Enumerated(EnumType.STRING)
    public LifecycleStage lifecycleStage = LifecycleStage.AWARENESS; // default value

    @Enumerated(EnumType.STRING)
    public ContentItemStatus status = ContentItemStatus.BACKLOG;

    // persona (Persona enum)

    public String successMeasurements;
}
