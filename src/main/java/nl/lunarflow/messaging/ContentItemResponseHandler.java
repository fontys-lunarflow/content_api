package nl.lunarflow.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.lunarflow.logging.Logger;
import nl.lunarflow.models.ContentItem;

public class ContentItemResponseHandler implements ResponseHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    private Logger logger;

    @Override
    @Transactional
    public void handleResponse(String correlationId, String response, Subjects subject) {
        if (!correlationId.startsWith("content_api.content_item.")) return;

        Long id = Long.parseLong(correlationId.replace("content_api.content_item.", ""));

        logger.log(String.format("Received message for content item %d", id));
        ContentItem item = ContentItem.findById(id);

        if (item == null) {
            logger.log("Content item with id " + id + " not found.");
            return;
        }

        JsonNode json = null;
        try {
            json = mapper.readTree(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String gitlabUrl = json.path("url").asText();

        if (gitlabUrl != null && !gitlabUrl.isBlank()) {
            logger.log(String.format("Adding github url of %d to database", id));

            item.gitlabIssueUrl = gitlabUrl;
            item.persistAndFlush();
        }
    }
}
