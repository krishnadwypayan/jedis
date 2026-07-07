package com.krishna.jedis.cmd;

import com.krishna.jedis.resp.*;

import java.util.List;

public class ServerCommands {

    /**
     * args = ["PING"] or ["PING","msg"]
     *
     * @param args
     * @return
     */
    public RespValue ping(String[] args) {
        if (args.length > 2) {
            return new RespError("ERR wrong number of arguments for 'ping' command");
        }
        return args.length == 2 ? RespBulkString.of(args[1]) : new RespSimpleString("PONG");
    }

    public RespValue config(String[] args) {        // args = ["CONFIG","GET","save"]
        if (args.length >= 3 && args[1].equalsIgnoreCase("GET")) {
            String key = args[2];
            return new RespArray(List.of(
                    RespBulkString.of(key),
                    RespBulkString.of(key.equals("appendonly") ? "no" : "")));
        }
        return new RespError("ERR unknown CONFIG subcommand");
    }

}
