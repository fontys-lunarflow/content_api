package nl.lunarflow.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.lunarflow.models.ContentItem;

import java.io.IOException;

public interface MessagingService {
    void sendMessage(ContentItem contentItem) throws IOException;
}
