package com.krishna.jedis.cmd;

import com.krishna.jedis.resp.*;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandHandler {

    private final Encoder encoder;
    private final Map<String, CommandSpec> commandTable;

    public CommandHandler() {
        this.encoder = new Encoder();
        var server = new ServerCommands();
        this.commandTable = Map.of(
                "PING", new CommandSpec("PING", -1, server::ping),
                "CONFIG", new CommandSpec("CONFIG", -2, server::config)
        );
    }

    public byte[] handle(Object decoded) {
        String[] args;
        try {
            args = narrowToCommand(decoded);
        } catch (IllegalArgumentException e) {
            return encoder.encode(new RespError("ERR protocol error: " + e.getMessage()));
        }

        if (args.length == 0) {
            return new byte[0];        // empty inline line → no reply
        }
        RespValue reply = dispatch(args);
        return encoder.encode(reply);
    }

    private RespValue dispatch(String[] args) {
        CommandSpec spec = commandTable.get(args[0].toUpperCase());
        if (spec == null)
            return unknownCommand(args);
        if (!arityOk(spec.arity(), args.length))
            return new RespError("ERR wrong number of arguments for '" + spec.name() + "' command");
        return spec.handler().execute(args);
    }

    private boolean arityOk(int arity, int actual) {
        return arity >= 0 ? actual == arity : actual >= -arity;
    }

    private RespValue unknownCommand(String[] args) {
        String argStr = Arrays.stream(args).skip(1)
                .map(a -> "'" + a + "'").collect(Collectors.joining(" "));
        return new RespError(String.format(
                "ERR unknown command '%s', with args beginning with: %s", args[0], argStr));
    }

    private String[] narrowToCommand(Object decoded) {
        if (!(decoded instanceof Object[] arr))     // String[] satisfies this (covariance)
            throw new IllegalArgumentException("expected array, got " + typeName(decoded));
        String[] args = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            if (!(arr[i] instanceof String s))       // null or Long element → malformed command
                throw new IllegalArgumentException("command args must be bulk strings");
            args[i] = s;
        }
        return args;
    }

    private String typeName(Object o) {
        return o == null ? "null" : o.getClass().getSimpleName();
    }

}
