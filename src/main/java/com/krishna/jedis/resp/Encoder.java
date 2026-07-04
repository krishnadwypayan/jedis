package com.krishna.jedis.resp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;

public class Encoder {

    private static final Logger LOGGER = LogManager.getLogger(Encoder.class);

    public String encodeError(String msg) {
        return String.format("-%s\r\n", msg);
    }

    public String encodeSimpleString(Object data) {
        return String.format("+%s\r\n", data);
    }

    public String encodeBulkString(Object data) {
        byte[] bytes = ((String) data).getBytes(StandardCharsets.UTF_8);
        return String.format("$%d\r\n%s\r\n", bytes.length, new String(bytes, StandardCharsets.UTF_8));
    }

    public String encodeArray(Object[] arr) {
        if (arr == null) {
            return "*-1\r\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("*%d\r\n", arr.length));

        for (Object o : arr) {
            String encoded = switch (o) {
                case null -> "$-1\r\n";
                case String s -> encodeBulkString(s);
                case Long l -> encodeInt64(l);
                case Integer i -> encodeInt64(Long.valueOf(i));
                case Object[] a -> encodeArray(a);
                default -> throw new IllegalArgumentException("Unsupported RESP type: " + o.getClass());
            };
            sb.append(encoded);
        }
        LOGGER.debug("Encoded arr: {} as '{}'", arr, sb.toString()
                .replace("\r", "\\r")
                .replace("\n", "\\n"));
        return sb.toString();
    }

    private String encodeInt64(Long obj) {
        return String.format(":%d\r\n", obj);
    }
}
