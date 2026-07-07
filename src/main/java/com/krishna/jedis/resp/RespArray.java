package com.krishna.jedis.resp;

import java.util.List;

public record RespArray(List<RespValue> items) implements RespValue {

    public static final RespArray NIL = new RespArray(null);

}
