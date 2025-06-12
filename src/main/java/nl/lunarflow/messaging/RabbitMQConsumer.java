package nl.lunarflow.messaging;

import com.rabbitmq.client.Delivery;
import java.io.IOException;

public interface RabbitMQConsumer {
    void init(QueueDeclarer queueDeclarer) throws IOException;
    String handleCallWithResponse(String correlationId, String body, String queueName, Delivery delivery);
    void handleCall(String correlationId, String body, String queueName, Delivery delivery);
}
