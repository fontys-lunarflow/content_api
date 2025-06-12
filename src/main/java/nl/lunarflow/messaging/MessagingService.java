package nl.lunarflow.messaging;

import java.io.IOException;

public interface MessagingService {
    void sendMessage(String id, String json, String subject, boolean reply) throws IOException;
}
