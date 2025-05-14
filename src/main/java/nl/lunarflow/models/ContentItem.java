package nl.lunarflow.models;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Enumerated;
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

    // TODO: ASK DIANA WTF IS CONTENT TYPE
    
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
    public List<Channel> channels = new ArrayList<>();

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

    @JsonProperty("channelIds")
    public void setChannelIds(List<Long> channels) {
        for (Long channelId : channels) {
            Channel channel = Channel.findById(channelId);
            if (channel != null) {
                this.channels.add(channel);
            }
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
    public static List<ContentItem> findByWeekAndYear(int week, int year) {
        WeekFields weekFields = WeekFields.ISO;
        LocalDate startDate = LocalDate
            .now()
            .withYear(year)
            .with(weekFields.weekOfWeekBasedYear(), week)
            .with(weekFields.dayOfWeek(), 1);

        LocalDate endDate = startDate.plusDays(6);
        LocalDateTime endOfDay = LocalDateTime.of(endDate, LocalTime.MAX);

        Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = endOfDay.atZone(ZoneOffset.UTC).toInstant();

        return find("publicationDate >= ?1 and publicationDate <= ?2", startInstant, endInstant).list();
    }
}
