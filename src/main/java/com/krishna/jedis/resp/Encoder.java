package com.krishna.jedis.resp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Encoder {

    private static final Logger LOGGER = LogManager.getLogger(Encoder.class);
    private static final String CRLF = "\r\n";

    public byte[] encode(RespValue respValue) {
        return switch (respValue) {
            case RespSimpleString s -> encodeSimpleString(s.value());
            case RespError e        -> encodeError(e.msg());
            case RespInteger i      -> encodeInt64(i.value());
            case RespBulkString b   -> encodeBulkString(b.data());
            case RespArray a        -> encodeArray(a.items());
        };
    }

    private byte[] encodeError(String msg) {
        return (String.format("-%s%s", msg, CRLF)).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] encodeSimpleString(String data) {
        return (String.format("+%s%s", data, CRLF)).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] encodeBulkString(byte[] data) {
        if (data == null) {
            return ("$-1" + CRLF).getBytes(StandardCharsets.UTF_8);
        }

        var out = new ByteArrayOutputStream();
        out.writeBytes((String.format("$%d%s", data.length, CRLF)).getBytes(StandardCharsets.UTF_8));
        out.writeBytes(data);
        out.writeBytes(CRLF.getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private byte[] encodeArray(List<RespValue> arr) {
        if (arr == null) {
            return (String.format("*-1%s", CRLF)).getBytes(StandardCharsets.UTF_8);
        }

        var out = new ByteArrayOutputStream();
        out.writeBytes(String.format("*%d%s", arr.size(), CRLF).getBytes(StandardCharsets.UTF_8));
        for (RespValue value : arr) {
            out.writeBytes(encode(value));
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Encoded arr: {} as '{}'", arr, out.toString()
                    .replace("\r", "\\r")
                    .replace("\n", "\\n"));
        }
        return out.toByteArray();
    }

    private byte[] encodeInt64(long obj) {
        return String.format(":%d%s", obj, CRLF).getBytes(StandardCharsets.UTF_8);
    }
}
