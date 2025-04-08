package nl.lunarflow.models;

import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table
public class Project extends PanacheEntity {
    
    @NotBlank
    public String name;

    @Column(length = 6)
    public String color;

    // access fields
    @OneToMany(mappedBy = "project")
    public List<ContentItem> contentItems = new ArrayList<>();
}
