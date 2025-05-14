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

        requestChannel.exchangeDeclare(rabbitMQConfig.exchange, BuiltinExchangeType.DIRECT, true);
        responseChannel.exchangeDeclare(rabbitMQConfig.exchange, BuiltinExchangeType.DIRECT, true);

        // Setup 2 different channels, one for sending messages to ticket API, one for receiving
        for (Subjects subject : Subjects.values()) {
            responseChannel.queueDeclare(subject.toString(), true, false, false, null);
            requestChannel.queueDeclare(subject.toString(), true, false, false, null);

            responseChannel.queueBind(subject.toString(), rabbitMQConfig.exchange, subject.toString());
            requestChannel.queueBind(subject.toString(), rabbitMQConfig.exchange, subject.toString());
        }


        startReplyConsumer();
    }

    public void sendMessage(ContentItem contentItem, Subjects subject) throws IOException {
        // We are using the content item ID as identifier so I can easily refer to the content item in db after the fact
        // TODO: discuss with the group if this is okay, or we should change this for safety reasons
        String correlationId = "content_item." + contentItem.id.toString();

        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(correlationId)
                .replyTo(rabbitMQConfig.requestQueue)
                .build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(contentItem);

        // Currently we are sending the full content item as json to the tic(tAPI
        // TODO: change this
        requestChannel.basicPublish(rabbitMQConfig.exchange, subject.toString(), props, json.getBytes());
    }

    public void startReplyConsumer() throws IOException {
        // For now there is only a single implementation of the response handler, if this changes it wouldnt be logical to keep it like this
        for (Subjects subject : Subjects.values()) {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String correlationId = delivery.getProperties().getCorrelationId();

                String response = new String(delivery.getBody());
                delivery.getProperties().getHeaders();

                responseHandler.handleResponse(correlationId, response, subject);
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
