package nl.lunarflow.controllers;

import java.util.List;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.panache.common.Sort;
import nl.lunarflow.models.ContentType;
import jakarta.annotation.security.RolesAllowed;

@Path("/content-types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContentTypeController {

    @GET
    @RolesAllowed({"LunarflowViewers"})
    public List<ContentType> get() {
        return ContentType.listAll(Sort.by("name"));
    }
}
