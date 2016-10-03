package com.github.pavradev.dockerbay;

/**
 *
 */
public class Bind {

    private String from;
    private String to;

    private Bind(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public static Bind create(String from, String to) {
        Bind bind = new Bind(from, to);
        return bind;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public boolean isFromVolume() {
        return !getFrom().startsWith("/");
    }
}
