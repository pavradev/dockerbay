package com.github.pavradev.dockerbay;

/**
 *
 */
public class Bind {

    public enum BindType {SHARED, PRIVATE}

    private String from;
    private String to;
    private BindType type;

    private Bind(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public static Bind getPrivate(String from, String to) {
        Bind bind = new Bind(from, to);
        bind.type = BindType.PRIVATE;
        return bind;
    }

    public static Bind getShared(String from, String to) {
        Bind bind = new Bind(from, to);
        bind.type = BindType.SHARED;
        return bind;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public boolean isShared() {
        return BindType.SHARED.equals(this.type);
    }

    public boolean isVolume() {
        return !getFrom().startsWith("/");
    }
}
