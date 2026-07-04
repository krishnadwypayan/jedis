package com.krishna.jedis.cmd;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandHandlerTest {

    private final CommandHandler commandHandler = new CommandHandler();

    @Test
    void testPing() {
        Map<Object, String> cases = Map.of(
                new Object[]{"PING"}, "+PONG\r\n",
                new Object[]{"PING", "hello"}, "$5\r\nhello\r\n",
                new Object[]{"PING", "a", "b"}, "-ERR wrong number of arguments for 'ping' command\r\n"
        );

        for (Map.Entry<Object, String> entry : cases.entrySet()) {
            assertEquals(entry.getValue(), commandHandler.evaluate(commandHandler.parse(entry.getKey())));
        }
    }

    @Test
    void unknownCommand() {
        Map<Object, String> cases = Map.of(
                new Object[]{"BLAH"}, "-ERR unknown command 'BLAH', with args beginning with: \r\n",
                new Object[]{"BLAH", "hello"}, "-ERR unknown command 'BLAH', with args beginning with: 'hello'\r\n",
                new Object[]{"BLAH", "hello", "world"}, "-ERR unknown command 'BLAH', with args beginning with: 'hello' 'world'\r\n"
        );

        for (Map.Entry<Object, String> entry : cases.entrySet()) {
            assertEquals(entry.getValue(), commandHandler.evaluate(commandHandler.parse(entry.getKey())));
        }
    }

}