package nl.lunarflow.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RabbitMQConfig {

//    @ConfigProperty(name = "rabbitmq.host")
    public String host = "rabbitmq";

//    @ConfigProperty(name = "rabbitmq.port")
    public int port = 5672;

//    @ConfigProperty(name = "rabbitmq.username")
    public String username = "guest";

//    @ConfigProperty(name = "rabbitmq.password")
    public String password = "guest";

//    @ConfigProperty(name = "rabbitmq.exchange")
    public String exchange = "ticket.exchange";

//    @ConfigProperty(name = "rabbitmq.response-queue")
    public String responseQueue = "ticketapi.response";

//    @ConfigProperty(name = "rabbitmq.request-queue")
    public String requestQueue = "ticketapi.request";
}
