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

    public static Bind fromString(String bindStr) {
        String[] split = bindStr.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid bind sting " + bindStr);
        }
        return new Bind(split[0], split[1]);
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

    @Override
    public String toString() {
        return getFrom() + ":" + getTo();
    }
}
