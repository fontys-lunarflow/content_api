package nl.lunarflow.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rabbitmq.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.lunarflow.models.ContentItem;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class RabbitMQClient implements MessagingService {
    private Channel requestChannel;
    private Channel responseChannel;
    private Connection connection;

    @Inject
    ResponseHandler responseHandler;

    @Inject
    RabbitMQConfig rabbitMQConfig;

    @PostConstruct
    void init() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQConfig.host);
        factory.setUsername(rabbitMQConfig.username);
        factory.setPassword(rabbitMQConfig.password);
        factory.setPort(rabbitMQConfig.port);

        connection = factory.newConnection();
        requestChannel = connection.createChannel();
        responseChannel = connection.createChannel();

        responseChannel.queueDeclare(rabbitMQConfig.responseQueue, true, false, false, null);
        requestChannel.queueDeclare(rabbitMQConfig.requestQueue, true, false, false, null);

        startReplyConsumer();
    }

    public void sendMessage(ContentItem contentItem, Subjects subject) throws IOException {
        String correlationId = "content_item." + contentItem.id.toString();

        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(correlationId)
                .replyTo(rabbitMQConfig.requestQueue)
                .build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(contentItem);

        requestChannel.basicPublish(rabbitMQConfig.exchange, subject.toString(), props, json.getBytes());
    }
    
    private void handleCallback(String consumerTag, Delivery delivery, Subjects subject) throws IOException {
        String correlationId = delivery.getProperties().getCorrelationId();

        String response = new String(delivery.getBody());
        delivery.getProperties().getHeaders();

        responseHandler.handleResponse(correlationId, response, subject);
    }

    public void startReplyConsumer() throws IOException {
        for (Subjects subject : Subjects.values()) {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                handleCallback(consumerTag, delivery, subject);
            };

            responseChannel.basicConsume(subject.toString(), true, deliverCallback, consumerTag -> {});
        }
    }


    public void close() throws IOException, TimeoutException {
        requestChannel.close();
        responseChannel.close();
        connection.close();
    }
}
