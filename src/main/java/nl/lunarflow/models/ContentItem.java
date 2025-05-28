package nl.lunarflow.models;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.time.temporal.WeekFields;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ElementCollection;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table
public class ContentItem extends PanacheEntity {

    @NotBlank(message = "Title cannot be blank.")
    public String title;

    @ManyToOne(fetch = FetchType.EAGER)
    public Project project;
    
    @NotBlank(message = "Person responsible cannnot be blank.")
    public String personResponsibleId;

    // TODO: ask diana
    // @URL(protocol = "https")
    // public String link;
    
    // gitlab ticket fields
    @URL(protocol = "https")
    public String gitlabIssueUrl;
    
    public Integer gitlabId;
    
    @Enumerated(EnumType.STRING)
    public LifecycleStage lifecycleStage = LifecycleStage.AWARENESS; // default value
    
    @Enumerated(EnumType.STRING)
    public ContentItemStatus status = ContentItemStatus.BACKLOG;
    
    @ManyToMany(fetch = FetchType.EAGER)
    public List<Persona> personas = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    public List<ContentType> contentTypes = new ArrayList<>();

    // Store GitLab label IDs directly without creating a Label entity
    @ElementCollection(fetch = FetchType.EAGER)
    public List<Long> labelIds = new ArrayList<>();

    // @OneToMany(mappedBy = "contentItem", fetch = FetchType.EAGER)
    // public List<ContentItemLabel> labels = new ArrayList<>();

    @Future(message = "Publication date must be in the future.")
    public Instant publicationDate;

    //TODO: ask diana
    // public String successMeasurements;

    // foreign key field connections in json input
    
    @JsonProperty("projectId")
    public void setProjectId(Long projectId) {
        if (projectId != null) {
            this.project = Project.findById(projectId);
        }
    }

    @JsonProperty("contentTypeIds")
    public void setContentTypeIds(List<Long> contentTypes) {
        for (Long contentTypeId : contentTypes) {
            ContentType contentType = ContentType.findById(contentTypeId);
            if (contentType != null) {
                this.contentTypes.add(contentType);
            }
        }
    }

    @JsonProperty("labelIds")
    public void setLabelIds(List<Long> labelIds) {
        if (labelIds != null) {
            this.labelIds.clear();
            this.labelIds.addAll(labelIds);
        }
    }
    
    @JsonProperty("personaIds")
    public void setPersonaIds(List<Long> personas) {
        for (Long personaId : personas) {
            Persona persona = Persona.findById(personaId);
            if (persona != null) {
                this.personas.add(persona);
            }
        }
    }

    // custom querying functionality

    // find all content items epr 
    public static List<ContentItem> findByWeekView(int week, int year) {
        WeekFields weekFields = WeekFields.ISO;

        // Calculate the ISO week boundaries
        LocalDate startDate = LocalDate
            .now()
            .withYear(year)
            .with(weekFields.weekOfWeekBasedYear(), week)
            .with(weekFields.dayOfWeek(), 1);

        LocalDate endDate = startDate.plusDays(6);
        LocalDateTime endOfDay = LocalDateTime.of(endDate, LocalTime.MAX);

        // Convert to Instants (UTC)
        Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = endOfDay.atZone(ZoneOffset.UTC).toInstant();

        return find("publicationDate >= ?1 and publicationDate <= ?2", Sort.by("publicationDate"), startInstant, endInstant).list();
    }

    public static List<ContentItem> findByMonthView(int month, int year) {
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        // Last day of the selected month
        LocalDate lastOfMonth = firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth());

        WeekFields weekFields = WeekFields.ISO;

        // Start on the Monday of the first visible week
        LocalDate calendarStart = firstOfMonth.with(weekFields.dayOfWeek(), 1); // Monday before or on the 1st

        // End on the Sunday of the last visible week
        LocalDate calendarEnd = lastOfMonth.with(weekFields.dayOfWeek(), 7); // Sunday after or on the last day

        // Convert to Instants (UTC)
        Instant startInstant = calendarStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = calendarEnd.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();

        // Fetch content items in that range
        return find("publicationDate >= ?1 and publicationDate <= ?2", Sort.by("publicationDate"), startInstant, endInstant).list();
    }
}
