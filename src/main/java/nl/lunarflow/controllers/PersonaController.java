package nl.lunarflow.controllers;

import java.util.List;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import nl.lunarflow.models.Persona;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.panache.common.Sort;

@Path("/personas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonaController {

    @GET
    public List<Persona> get() {
        return Persona.listAll(Sort.by("name"));
    }
}