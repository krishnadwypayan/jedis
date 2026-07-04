package com.krishna.jedis.resp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Decoder {

    public DecodeResult decode(ByteBuffer data) throws IOException {
        if (!data.hasRemaining()) {
            throw new IOException("no data found");
        }

        data.mark();  // mark so that we can reset if required

        byte firstByte = data.get();
        DecodeResult result = switch (firstByte) {
            case '+' -> decodeSimpleString(data);
            case '-' -> decodeSimpleError(data);
            case ':' -> decodeInt64(data);
            case '$' -> decodeBulkString(data);
            case '*' -> decodeArray(data);
            default -> {
                data.reset();
                yield decodeInline(data);
            }
        };

        return switch (result.status) {
            case SUCCESS, ERROR -> result;
            case INCOMPLETE -> {
                data.reset();
                yield result;
            }
        };
    }

    private DecodeResult decodeInline(ByteBuffer data) {
        int start = data.position();
        while (data.hasRemaining() && data.get() != '\r') {
        }

        if (!data.hasRemaining() || data.get() != '\n') {
            return new DecodeResult(null, DecodeResult.Status.INCOMPLETE);
        }

        int end = data.position()-2;
        return new DecodeResult(
                StandardCharsets.UTF_8.decode(data.slice(start, end-start)).toString().split(" "),
                DecodeResult.Status.SUCCESS);
    }

    private DecodeResult decodeArray(ByteBuffer data) throws IOException {
        DecodeResult decodeResult = decodeInt64(data);
        if (decodeResult.status == DecodeResult.Status.INCOMPLETE) {
            return decodeResult;
        }

        int len = Math.toIntExact((Long) decodeResult.value);
        if (len < 0) {
            return new DecodeResult(null, DecodeResult.Status.SUCCESS);
        }

        Object[] arr = new Object[len];
        for (int i = 0; i < len; i++) {
            if (!data.hasRemaining()) {
                return new DecodeResult(null, DecodeResult.Status.INCOMPLETE);
            }
            DecodeResult result = decode(data);
            if (result.status == DecodeResult.Status.INCOMPLETE) {
                return result;
            }
            arr[i] = result.value;
        }
        return new DecodeResult(arr, DecodeResult.Status.SUCCESS);
    }

    private DecodeResult decodeBulkString(ByteBuffer data) {
        DecodeResult decodeResult = decodeInt64(data);
        if (decodeResult.status == DecodeResult.Status.INCOMPLETE) {
            return decodeResult;
        }

        int length = Math.toIntExact((Long) decodeResult.value);
        if (length < 0) {
            return new DecodeResult(null, DecodeResult.Status.SUCCESS);
        }

        if (data.remaining() >= length + 2) {
            // data is already moved till after 'length\r\n'
            String result = StandardCharsets.UTF_8.decode(data.slice(data.position(), length)).toString();
            data.position(data.position() + length + 2);
            return new DecodeResult(result, DecodeResult.Status.SUCCESS);
        }
        return new DecodeResult(null, DecodeResult.Status.INCOMPLETE);
    }

    private DecodeResult decodeInt64(ByteBuffer data) {
        long value = 0L;
        if (!data.hasRemaining()) {
            return new DecodeResult(null, DecodeResult.Status.INCOMPLETE);
        }

        boolean negative = data.get(data.position()) == '-';

        if (negative || data.get(data.position()) == '+') {
            data.get();
        }

        while (data.hasRemaining()) {
            byte b = data.get();
            if (b != '\r') {
                value = value * 10L + (b - '0');
            } else {
                break;
            }
        }

        if (!data.hasRemaining()) {
            return new DecodeResult(null, DecodeResult.Status.INCOMPLETE);
        }

        data.get(); // consume '\n'
        return new DecodeResult(negative ? -value : value, DecodeResult.Status.SUCCESS);
    }

    private DecodeResult decodeSimpleError(ByteBuffer data) {
        return decodeSimpleString(data);
    }

    private DecodeResult decodeSimpleString(ByteBuffer data) {
        int start = data.position();
        while (data.hasRemaining() && data.get() != '\r') {
        }

        if (!data.hasRemaining() || data.get(data.position()) != '\n') {
            // incomplete input
            return new DecodeResult(null, DecodeResult.Status.INCOMPLETE);
        }

        int end = data.position() - 1;
        String value = StandardCharsets.UTF_8.decode(data.slice(start, end - start)).toString();
        data.get(); // consume '\n'
        return new DecodeResult(value, DecodeResult.Status.SUCCESS);
    }

    public record DecodeResult(Object value, Status status) {
        public enum Status {
            SUCCESS, ERROR, INCOMPLETE
        }
    }

}
