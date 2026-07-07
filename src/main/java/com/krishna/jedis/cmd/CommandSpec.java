package com.krishna.jedis.cmd;

public record CommandSpec(String name, int arity, CommandFn handler) {
}
