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

    private final String text;

    Subjects(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
