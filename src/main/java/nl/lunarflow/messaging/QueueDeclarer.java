package nl.lunarflow.messaging;

import java.io.IOException;

interface QueueDeclarer {
    void declareQueue(String queueName) throws IOException;
}
