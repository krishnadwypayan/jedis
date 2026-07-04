package com.krishna.jedis.cmd;

import com.krishna.jedis.resp.Encoder;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CommandHandler {

    private final Encoder encoder;

    public CommandHandler() {
        this.encoder = new Encoder();
    }

    public JedisCmd parse(Object data) throws IllegalArgumentException {
        Object[] arr = (Object[]) data;
        String[] args = Arrays.stream(Arrays.copyOfRange(arr, 1, arr.length))
                .map(o -> (String) o)
                .toArray(String[]::new);
        Command command;
        try {
            command = Command.valueOf(((String) arr[0]).toUpperCase());
        } catch (IllegalArgumentException e) {
            return new JedisCmd(Command.UNKNOWN, (String) arr[0], args);
        }
        return new JedisCmd(command, command.name(), args);
    }

    public String evaluate(JedisCmd cmd) {
        return switch (cmd.command()) {
            case PING -> evalPing(cmd.args());
            case CONFIG -> evalConfig(cmd.args());
            case UNKNOWN -> evalUnknownCommand(cmd.rawName(), cmd.args());
        };
    }

    private String evalUnknownCommand(String cmd, String[] args) {
        String argString = Arrays.stream(args)
                .map(arg -> String.format("'%s'", arg))
                .collect(Collectors.joining(" "));
        return encoder.encodeError(String.format("ERR unknown command '%s', with args beginning with: %s", cmd, argString));
    }

    private String evalPing(String[] args) {
        if (args.length > 1) {
            return encoder.encodeError("ERR wrong number of arguments for 'ping' command");
        }

        if (args.length == 1) {
            return encoder.encodeBulkString(args[0]);
        } else {
            return encoder.encodeSimpleString("PONG");
        }
    }

    private String evalConfig(String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("GET")) {
            return encoder.encodeArray(new Object[]{args[1], args[1].equals("appendonly") ? "no" : ""});
        }
        return encoder.encodeError("ERR unknown CONFIG subcommand");
    }

}
