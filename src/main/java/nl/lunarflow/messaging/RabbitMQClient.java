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
public class RabbitMQClient implements MessagingService, QueueDeclarer {
    private Channel channel;
    private Connection connection;

    @Inject
    RabbitMQConsumer consumer;

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

        // We give a reference of this MessagingService to the consumer, so it can declare queues
        consumer.init(this);
    }

    @Override
    public void declareQueue(String queueName) throws IOException {
        String longQueueName = rabbitMQConfig.thisService + "." + queueName;
        channel.queueDeclare(longQueueName, true, false, false, null);

        channel.queueBind(longQueueName, rabbitMQConfig.exchange, longQueueName);

        channel.basicConsume(longQueueName,true, (consumerTag, delivery) ->{
            String correlationId = delivery.getProperties().getCorrelationId();
            String body = new String(delivery.getBody());

            String replyTo = delivery.getProperties().getReplyTo();

            // This is split intentionally, so in my consumer methods I could optimize for not having to return a response
            if (replyTo != null && !replyTo.isEmpty()) {

                String response = consumer.handleCallWithResponse(correlationId, body, queueName, delivery);
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(correlationId)
                        .build();

                channel.basicPublish("", replyTo, replyProps, response.getBytes());
            } else {
                consumer.handleCall(correlationId, body, queueName, delivery);
            }
        }, consumerTag -> {});
    }

    public void sendMessage(String id, String json, String subject, boolean reply) throws IOException {
        // We are using the content item ID as identifier so I can easily refer to the content item in db after the fact
        // TODO: discuss with the group if this is okay, or we should change this for safety reasons
        String routingKey = rabbitMQConfig.otherService + "." + subject;
        String correlationId = rabbitMQConfig.thisService + "." + rabbitMQConfig.correlationId + "." + id;

        String queueName = rabbitMQConfig.thisService + "." + subject;

        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties
                .Builder()
                .correlationId(correlationId);

        if (reply) {
            builder.replyTo(queueName);
        }

        AMQP.BasicProperties props = builder.build();

        // Currently we are sending the full content item as json to the ticketAPI
        // TODO: think if we should change this
        channel.basicPublish(rabbitMQConfig.exchange, routingKey, props, json.getBytes());
    }


    public void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
}
