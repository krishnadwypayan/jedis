package com.krishna.jedis;

import com.krishna.jedis.cmd.CommandHandler;
import com.krishna.jedis.resp.Decoder;
import com.krishna.jedis.server.JedisServer;

public class Main {

    public static void main(String[] args) {
        JedisServer jedisServer = new JedisServer(new Decoder(), new CommandHandler());
        jedisServer.startServer();
    }

}
