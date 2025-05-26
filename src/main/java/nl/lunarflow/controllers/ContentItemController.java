package nl.lunarflow.controllers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.DELETE;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.QueryParam;
import jakarta.validation.Valid;
import org.jboss.logging.Logger;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.NotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.CriteriaQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.lunarflow.models.*;

@Path("/content-items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContentItemController {

private static final Logger LOGGER = Logger.getLogger(ContentItemController.class);
    
    @Inject
    ObjectMapper objectMapper;

    @Inject
    EntityManager em;

    @GET
    @Transactional
    public List<ContentItem> get(
        @BeanParam ContentItemSearchParams params
        ) {

        // create the query with filters using Criteria API
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ContentItem> cq = cb.createQuery(ContentItem.class);
        Root<ContentItem> root = cq.from(ContentItem.class);



        // create a list of all filters
        List<Predicate> andPredicates = new ArrayList<>();

        // JOINs (for personas and content types)
        Join<ContentItem, Persona> personaJoin = null;
        Join<ContentItem, ContentType> contentTypeJoin = null;

        // FILTERS

        // Project IDs
        if (params.projectIds != null && !params.projectIds.isEmpty()) {
            andPredicates.add(root.get("project").get("id").in(params.projectIds));
        }

        // Person Responsible
        if (params.personResponsibleIds != null && !params.personResponsibleIds.isEmpty()) {
            andPredicates.add(root.get("personResponsibleId").in(params.personResponsibleIds));
        }

        // Lifecycle Stage
        if (params.lifecycleStages != null && !params.lifecycleStages.isEmpty()) {
            andPredicates.add(root.get("lifecycleStage").in(params.lifecycleStages));
        }

        // Status
        if (params.statuses != null && !params.statuses.isEmpty()) {
            andPredicates.add(root.get("status").in(params.statuses));
        }

        // Personas
        if (params.personaIds != null && !params.personaIds.isEmpty()) {
            personaJoin = root.join("personas");
            andPredicates.add(personaJoin.get("id").in(params.personaIds));
        }

        // Content Types
        if (params.contentTypeIds != null && !params.contentTypeIds.isEmpty()) {
            contentTypeJoin = root.join("contentTypes");
            andPredicates.add(contentTypeJoin.get("id").in(params.contentTypeIds));
        }

        // Apply all AND conditions
        cq.where(cb.and(andPredicates.toArray(new Predicate[0])));

        // sort the results
        cq.orderBy(
            cb.asc(root.get("publicationDate")),
            cb.asc(root.get("title"))
        );

        var r = em.createQuery(cq).getResultList();

        return r;
    }

    @GET
    @Path("weekly")
    @Transactional
    public List<ContentItem> getWeekly(
        @QueryParam("week") Integer week,
        @QueryParam("year") Integer year
        ) {
            // null check query params
            if (week == null) {
                throw new BadRequestException("Please supply the week.");
            }

            if (year == null) {
                throw new BadRequestException("Please supply the year.");
            }

            if (week < 1 || week > 53) {
                throw new BadRequestException("Please supply a valid week (1-53).");
            }


            return ContentItem.findByWeekView(week, year);
    }

    @GET
    @Path("monthly")
    @Transactional
    public List<ContentItem> getMonthly(
        @QueryParam("month") Integer month,
        @QueryParam("year") Integer year
        ) {
            // null check the query params
            if (month == null) {
                throw new BadRequestException("Please supply the month.");
            }

            if (year == null) {
                throw new BadRequestException("Please supply the year.");
            }
            
            if (month < 1 || month > 12) {
                throw new BadRequestException("Please supply a valid month (1-12).");
            }

            return ContentItem.findByMonthView(month, year);
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
        entity.contentTypes = updatedContentItem.contentTypes;
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

    public static class ContentItemSearchParams {
        @QueryParam("projectIds")
        List<Integer> projectIds;

        @QueryParam("personResponsibleIds")
        List<String> personResponsibleIds;

        @QueryParam("lifecycleStages")
        List<LifecycleStage> lifecycleStages;

        @QueryParam("statuses")
        List<ContentItemStatus> statuses;

        @QueryParam("personaIds")
        List<Integer> personaIds;

        @QueryParam("contentTypeIds")
        List<Integer> contentTypeIds;

        // @QueryParam("publicationDate")
        // Instant publicationDate;
    }
}
