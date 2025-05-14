package nl.lunarflow.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import nl.lunarflow.models.ContentItem;

@ApplicationScoped
public class ContentItemResponseHandler implements ResponseHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional
    public void handleResponse(String correlationId, String response) {
        try {
            if (!correlationId.startsWith("content_item.")) return;

            Long id = Long.parseLong(correlationId.replace("content_item.", ""));
            ContentItem item = ContentItem.findById(id);

            if (item == null) {
                System.out.println("Content item with id " + id + " not found.");
                return;
            }

            JsonNode json = mapper.readTree(response);
            String gitlabUrl = json.path("gitlab_url").asText();

            if (gitlabUrl != null && !gitlabUrl.isBlank()) {
                item.gitlabIssueUrl = gitlabUrl;
                item.persistAndFlush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
