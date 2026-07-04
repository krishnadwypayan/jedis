package com.krishna.jedis.cmd;

public record JedisCmd(Command command, String rawName, String[] args) {
}
