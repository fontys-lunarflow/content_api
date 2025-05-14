package nl.lunarflow.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RabbitMQConfig {

    @ConfigProperty(name = "rabbitmq.host")
    public String host;

    @ConfigProperty(name = "rabbitmq.port")
    public int port;

    @ConfigProperty(name = "rabbitmq.username")
    public String username;

    @ConfigProperty(name = "rabbitmq.password")
    public String password;

    @ConfigProperty(name = "rabbitmq.exchange")
    public String exchange;

    @ConfigProperty(name = "rabbitmq.response-queue")
    public String responseQueue;

    @ConfigProperty(name = "rabbitmq.request-queue")
    public String requestQueue;
}
