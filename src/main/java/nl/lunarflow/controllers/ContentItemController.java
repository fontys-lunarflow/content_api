package nl.lunarflow.controllers;

import java.util.List;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.DELETE;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.validation.Valid;
import org.jboss.logging.Logger;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.panache.common.Sort;
import jakarta.ws.rs.NotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.WebApplicationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.lunarflow.models.*;

@Path("/content-items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContentItemController {

private static final Logger LOGGER = Logger.getLogger(ContentItemController.class);
    
    @Inject
    ObjectMapper objectMapper;

    @GET
    @Transactional
    public List<ContentItem> get() {
        return ContentItem.listAll(Sort.by("publicationDate"));
    }

    @GET
    @Path("weekly")
    @Transactional
    public List<ContentItem> getWeekly(
        @QueryParam("week") Integer week,
        @QueryParam("year") Integer year
        ) {

            if (week == null) {
                throw new BadRequestException("Please supply the week.");
            }

            if (year == null) {
                throw new BadRequestException("Please supply the week.");
            }

            return ContentItem.findByWeekAndYear(week, year);
    }

    @GET
    @Path("{id}")
    // @Transactional
    public ContentItem getSingle(Long id) {
        ContentItem entity = ContentItem.findById(id);

        if (entity == null) {
            throw new NotFoundException(String.format("Content item with id %d not found.", id));
        }

        return entity;
    }

    @POST
    @Transactional
    public Response create(@Valid ContentItem contentItem) {

        if (contentItem.project == null) {
            // FIXME: i cannot get projectId from the request body, and raising 404 in the deserialization (Content) does not work
            throw new NotFoundException("Please specify an existing project.");
        }

        contentItem.persistAndFlush();

        return Response.ok().entity(contentItem).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public Response update (Long id, @Valid ContentItem updatedContentItem) {
        ContentItem entity = ContentItem.findById(id);

        if (entity == null) {
            throw new NotFoundException(String.format("Content item with id %d not found.", id));
        }

        if (updatedContentItem.project == null) {
            throw new NotFoundException("Please specify an existing project.");
        }

        // update all the fields
        entity.title = updatedContentItem.title;
        entity.project = updatedContentItem.project;
        entity.personResponsibleId = updatedContentItem.personResponsibleId;
        entity.gitlabIssueUrl = updatedContentItem.gitlabIssueUrl;
        entity.gitlabId = updatedContentItem.gitlabId;
        entity.lifecycleStage = updatedContentItem.lifecycleStage;
        entity.status = updatedContentItem.status;
        entity.channels = updatedContentItem.channels;
        entity.publicationDate = updatedContentItem.publicationDate;

        entity.persistAndFlush();

        return Response.ok().entity(entity).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(Long id) {
        ContentItem entity = ContentItem.findById(id);

        if (entity == null) {
            throw new NotFoundException(String.format("Content item with id %d not found.", id));
        }

        entity.delete();
        
        ObjectNode responseBody = objectMapper.createObjectNode();
        responseBody.put("message", "Content item deleted successfully.");


        return Response.ok().entity(responseBody).build();
    }

    /// exception mapper
    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Inject
        ObjectMapper objectMapper;

        @Override
        public Response toResponse(Exception exception) {
            // LOGGER.info() is the default level
            LOGGER.error("Failed to handle request", exception);

            int code = 500;
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception).getResponse().getStatus();
            }

            ObjectNode exceptionJson = objectMapper.createObjectNode();
            exceptionJson.put("exception_type", exception.getClass().getName());
            exceptionJson.put("status_code", code);

            if (exception.getMessage() != null) {
                exceptionJson.put("message", exception.getMessage());
            }

            return Response.status(code)
                    .entity(exceptionJson)
                    .build();
        }
    }
}
