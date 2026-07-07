package com.krishna.jedis.cmd;

import com.krishna.jedis.resp.RespValue;

@FunctionalInterface
public interface CommandFn {

    /**
     * This method is responsible for executing the command.
     *
     * @param args includes the 'command' at args[0] and other args[1...]
     * @return response for the command
     */
    RespValue execute(String[] args);

}
