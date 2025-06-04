package nl.lunarflow.controllers;

import java.util.List;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import nl.lunarflow.models.Project;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.panache.common.Sort;
import jakarta.annotation.security.RolesAllowed;

@Path("/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectController {

    @GET
    @RolesAllowed({"LunarflowViewers"})
    public List<Project> get() {
        return Project.listAll(Sort.by("name"));
    }
}