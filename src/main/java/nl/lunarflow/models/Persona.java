package nl.lunarflow.models;

import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table
public class Persona extends PanacheEntity {
    @NotBlank
    public String name;

    @NotBlank
    public String description;

    // access fields
    @JsonIgnore
    @ManyToMany(mappedBy = "personas")
    public List<ContentItem> contentItems = new ArrayList<>();
}
