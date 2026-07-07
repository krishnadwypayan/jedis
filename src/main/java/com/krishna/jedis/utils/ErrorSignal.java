package com.krishna.jedis.utils;

public class ErrorSignal extends RuntimeException {

    public static final ErrorSignal INSTANCE = new ErrorSignal();

    public ErrorSignal() {
        super(null, null, false, false);
    }

}
