package com.krishna.jedis.resp;

import com.krishna.jedis.utils.ErrorSignal;
import com.krishna.jedis.utils.IncompleteSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Decoder {

    private static final Logger LOGGER = LogManager.getLogger(Decoder.class);

    public DecodeResult decode(ByteBuffer data) throws IncompleteSignal {
        if (!data.hasRemaining()) {
            throw IncompleteSignal.INSTANCE;
        }

        data.mark();  // mark so that we can reset if required

        try {
            Object result = decodeOne(data);
            return new DecodeResult(result, DecodeResult.Status.SUCCESS);
        } catch (ErrorSignal e) {
            LOGGER.error("Error parsing data: {}", data, e);
            return new DecodeResult(null, DecodeResult.Status.ERROR);
        } catch (IncompleteSignal e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse. Incomplete data received: {}", data, e);
            }
            data.reset();
            return new DecodeResult(null, DecodeResult.Status.INCOMPLETE);
        }
    }

    private Object decodeOne(ByteBuffer data) throws IncompleteSignal, ErrorSignal {
        byte firstByte = data.get();
        return switch (firstByte) {
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
    }

    private String[] decodeInline(ByteBuffer data) throws IncompleteSignal {
        ByteBuffer result = readLine(data);
        if (result == null || !result.hasRemaining()) {
            return new String[0];
        }
        return StandardCharsets.UTF_8.decode(result).toString().trim().split("\\s+");
    }

    private Object[] decodeArray(ByteBuffer data) throws IncompleteSignal, ErrorSignal {
        int len = int64ToInt(decodeInt64(data));
        if (len < 0) {
            return null;
        }

        Object[] arr = new Object[len];
        for (int i = 0; i < len; i++) {
            arr[i] = decodeOne(data);
        }
        return arr;
    }

    private String decodeBulkString(ByteBuffer data) throws IncompleteSignal {
        int length = int64ToInt(decodeInt64(data));
        if (length < 0) {
            return null;
        }

        if (data.remaining() >= length + 2) {
            // data is already moved till after 'length\r\n'
            String result = StandardCharsets.UTF_8.decode(data.slice(data.position(), length)).toString();
            data.position(data.position() + length + 2);
            return result;
        }
        throw IncompleteSignal.INSTANCE;
    }

    private Long decodeInt64(ByteBuffer data) throws IncompleteSignal, ErrorSignal {
        long value = 0L;
        if (!data.hasRemaining()) {
            throw IncompleteSignal.INSTANCE;
        }

        boolean negative = data.get(data.position()) == '-';
        if (negative || data.get(data.position()) == '+') {
            data.get();
        }
        while (data.hasRemaining()) {
            byte b = data.get();
            if (b != '\r') {
                if (b < '0' || b > '9') {
                    throw ErrorSignal.INSTANCE;
                }
                value = value * 10L + (b - '0');
            } else {
                break;
            }
        }

        if (!data.hasRemaining()) {
            throw IncompleteSignal.INSTANCE;
        }

        data.get(); // consume '\n'
        return negative ? -value : value;
    }

    private String decodeSimpleError(ByteBuffer data) {
        return decodeSimpleString(data);
    }

    private String decodeSimpleString(ByteBuffer data) throws IncompleteSignal {
        return StandardCharsets.UTF_8.decode(readLine(data)).toString();
    }

    /**
     * Returns the line by consuming
     *
     * @param data
     * @return
     */
    private ByteBuffer readLine(ByteBuffer data) throws IncompleteSignal {
        int start = data.position();
        while (data.hasRemaining() && data.get() != '\r') {
        }

        if (!data.hasRemaining() || data.get() != '\n') {
            throw IncompleteSignal.INSTANCE;
        }

        int end = data.position() - 2;
        return data.slice(start, end - start);
    }

    private int int64ToInt(long value) {
        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException e) {
            throw ErrorSignal.INSTANCE;
        }
    }

    public record DecodeResult(Object value, Status status) {
        public enum Status {
            SUCCESS, ERROR, INCOMPLETE
        }
    }

}
