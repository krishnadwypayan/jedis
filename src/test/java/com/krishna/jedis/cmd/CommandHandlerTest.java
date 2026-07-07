package com.krishna.jedis.cmd;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class CommandHandlerTest {

    private final CommandHandler commandHandler = new CommandHandler();

    @Test
    void testPing() {
        Map<String[], String> cases = Map.of(
                new String[]{"PING"}, "+PONG\r\n",
                new String[]{"PING", "hello"}, "$5\r\nhello\r\n",
                new String[]{"PING", "a", "b"}, "-ERR wrong number of arguments for 'ping' command\r\n"
        );

        for (Map.Entry<String[], String> entry : cases.entrySet()) {
            assertArrayEquals(entry.getValue().getBytes(StandardCharsets.UTF_8), commandHandler.handle(entry.getKey()));
        }
    }

    @Test
    void unknownCommand() {
        Map<String[], String> cases = Map.of(
                new String[]{"BLAH"}, "-ERR unknown command 'BLAH', with args beginning with: \r\n",
                new String[]{"BLAH", "hello"}, "-ERR unknown command 'BLAH', with args beginning with: 'hello'\r\n",
                new String[]{"BLAH", "hello", "world"}, "-ERR unknown command 'BLAH', with args beginning with: 'hello' 'world'\r\n"
        );

        for (Map.Entry<String[], String> entry : cases.entrySet()) {
            assertArrayEquals(entry.getValue().getBytes(StandardCharsets.UTF_8), commandHandler.handle(entry.getKey()));
        }
    }

}