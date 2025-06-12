package nl.lunarflow.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import jakarta.inject.Inject;
import nl.lunarflow.logging.Logger;
import nl.lunarflow.models.ContentItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RabbitMQUser implements RabbitMQConsumer {
    @Inject
    private Logger logger;

    private final ObjectMapper mapper = new ObjectMapper();


    @Override
    public void init(QueueDeclarer queueDeclarer) throws IOException {
        // Converts the enum to a list of strings, so the client doesn't have to know about the enum
        for (String subject : Arrays.stream(Subjects.values()).map(subjects -> {return subjects.name();}).toArray(String[]::new)) {
            queueDeclarer.declareQueue(subject);
        }
    }

    @Override
    public String handleCallWithResponse(String correlationId, String body, String queueName, Delivery delivery) {
        if (!correlationId.startsWith("content_api.content_item.")) return null;
        Long id;
        try {
            id = Long.parseLong(correlationId.replace("content_api.content_item.", ""));
        } catch (NumberFormatException e) {
            logger.log(String.format("Id %s is an invalid id", correlationId));
            return null;
        }
        logger.log(String.format("Received message for content item %d", id));

        ContentItem item = ContentItem.findById(id);

        if (item == null) {
            logger.log("Content item with id " + id + " not found.");
            return null;
        }

        JsonNode json = null;
        try {
            json = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String response = null;
        switch (Subjects.getSubject(queueName)) {
            case Subjects.TICKET_CREATE -> response = handleTicketCreate(item, json, delivery);
            case Subjects.TICKET_CLOSE -> logger.log(String.format("Ticket closed for content item %d", id));
            case Subjects.TICKET_READ -> response = handleTicketRead(item, json, delivery);
            case Subjects.TICKET_SETLABELS -> logger.log(String.format("Ticket setlabels for content item %d", id));
            case Subjects.LABEL_CREATE -> logger.log(String.format("Label created for content item %d", id));
            case Subjects.LABEL_LIST -> logger.log(String.format("Label list for content item %d", id));
            case Subjects.LABEL_DELETE -> logger.log(String.format("Label deleted for content item %d", id));

            case null -> {} // getSubject returns null if the string is not equal to an enum
        }

        return response;
    }

    private String handleTicketRead(ContentItem item, JsonNode json, Delivery delivery) {
        logger.log(String.format("Read ticket %d", item.id));
        JsonNode labelsNode = json.path("labels");
        List<String> labels = new ArrayList<>();

        if (labelsNode.isArray()) {
            for (JsonNode labelNode : labelsNode) {
                labels.add(labelNode.asText());
            }
        }

        return mapper.valueToTree(labels).toString();
    }

    private String handleTicketCreate(ContentItem item, JsonNode json, Delivery delivery) {
        String gitlabUrl = json.path("url").asText();

        if (gitlabUrl != null && !gitlabUrl.isBlank()) {
            logger.log(String.format("Adding github url of %d to database", item.id));

            item.gitlabIssueUrl = gitlabUrl;
            item.persistAndFlush();

            return mapper.valueToTree(item).toString();
        }

        return null;
    }

    @Override
    public void handleCall(String correlationId, String body, String queueName, Delivery delivery) {
        handleCallWithResponse(correlationId, body, queueName, delivery);
    }
}
