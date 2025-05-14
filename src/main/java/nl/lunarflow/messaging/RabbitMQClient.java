package nl.lunarflow.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rabbitmq.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.lunarflow.models.ContentItem;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class RabbitMQClient implements MessagingService {
    private Channel channel;
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
        channel = connection.createChannel();

        channel.queueDeclare(rabbitMQConfig.replyQueue, true, false, false, null);

        startReplyConsumer();
    }

    public void sendMessage(ContentItem contentItem) throws IOException {
        String correlationId = "content_item." + contentItem.id.toString();

        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(correlationId)
                .replyTo(rabbitMQConfig.replyQueue)
                .build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(contentItem);

        channel.basicPublish(rabbitMQConfig.exchange, rabbitMQConfig.routingKey, props, json.getBytes());
    }

    public void startReplyConsumer() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String correlationId = delivery.getProperties().getCorrelationId();

            String response = new String(delivery.getBody());

            responseHandler.handleResponse(correlationId, response);
        };

        channel.basicConsume(rabbitMQConfig.replyQueue, true, deliverCallback, consumerTag -> {});
    }



    public void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
}
