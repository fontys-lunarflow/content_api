package nl.lunarflow.messaging;

public enum Subjects {
    TICKET_CREATE("ticket.create"),
    TICKET_READ("ticket.read"),
    TICKET_CLOSE("ticket.close"),
    TICKET_SETLABELS("ticket.setlabels"),

    LABEL_CREATE("label.create"),
    LABEL_DELETE("label.delete"),
    LABEL_LIST("label.list"),
    ;

    // Everything under here is, so I can access the toString() method to convert the enum types into correct string form
    private final String topic;

    ///
    /// @param topic This refers to the topic name within rabbitMQ
    ///
    Subjects(final String topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        return topic;
    }
}
