package com.krishna.jedis.resp;

public record RespError(String msg) implements RespValue {
}
