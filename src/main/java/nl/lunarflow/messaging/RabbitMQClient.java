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
        System.out.println("Connecting to RabbitMQ");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQConfig.host);
        factory.setUsername(rabbitMQConfig.username);
        factory.setPassword(rabbitMQConfig.password);
        factory.setPort(rabbitMQConfig.port);

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(rabbitMQConfig.exchange, BuiltinExchangeType.DIRECT, true);

        // Setup 2 different channels, one for sending messages to ticket API, one for receiving
        for (Subjects subject : Subjects.values()) {
            String queueName = "content_api." + subject.toString();
            channel.queueDeclare(queueName, true, false, false, null);

            channel.queueBind(queueName, rabbitMQConfig.exchange, queueName);

            channel.basicConsume(queueName,true, (consumerTag, delivery) ->{
                String correlationId = delivery.getProperties().getCorrelationId();
                String body = new String(delivery.getBody());

                responseHandler.handleResponse(correlationId, body, subject);
//
//                if (replyTo != null && !replyTo.isEmpty()) {
//                    AMQP.BasicProperties replyProps = new AMQP.BasicProperties
//                            .Builder()
//                            .correlationId(correlationId)
//                            .build();
//
//                    channel.basicPublish("", replyTo, replyProps, response.getBytes());
//                }
            }, consumerTag -> {});
        }


    }

    public void sendMessage(ContentItem contentItem, Subjects subject) throws IOException {
        // We are using the content item ID as identifier so I can easily refer to the content item in db after the fact
        // TODO: discuss with the group if this is okay, or we should change this for safety reasons
        String routingKey = "ticket_api." + subject.toString();
        String correlationId = "content_api.content_item." + contentItem.id.toString();

        String queueName = "content_api." + subject.toString();


        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(correlationId)
                .replyTo(queueName)
                .build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(contentItem);

        // Currently we are sending the full content item as json to the tic(tAPI
        // TODO: change this
        channel.basicPublish(rabbitMQConfig.exchange, routingKey, props, json.getBytes());
    }


    public void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
}
