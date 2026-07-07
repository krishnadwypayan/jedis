package com.krishna.jedis.utils;

public class IncompleteSignal extends RuntimeException {

    public static final IncompleteSignal INSTANCE = new IncompleteSignal();

    public IncompleteSignal() {
        super(null, null, false, false);
    }

}
