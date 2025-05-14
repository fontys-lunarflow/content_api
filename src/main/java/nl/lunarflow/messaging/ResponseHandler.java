package nl.lunarflow.messaging;

public interface ResponseHandler {
    void handleResponse(String correlationId, String responseJson, Subjects subject);
}
