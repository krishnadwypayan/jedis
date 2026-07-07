package com.krishna.jedis.resp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public record RespBulkString(byte[] data) implements RespValue {

    public static RespBulkString of(String str) {
        return new RespBulkString(str.getBytes(StandardCharsets.UTF_8));
    }

    public static final RespBulkString NIL = new RespBulkString(null);

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RespBulkString that = (RespBulkString) o;
        return Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
