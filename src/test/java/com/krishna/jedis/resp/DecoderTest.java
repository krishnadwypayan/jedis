package com.krishna.jedis.resp;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DecoderTest {

    private final Decoder decoder = new Decoder();

    @Test
    void decodeInteger() {
        Map<String, Long> cases = Map.of(
                ":-10\r\n", -10L,
                ":10\r\n", 10L,
                ":106877868996\r\n", 106877868996L,
                ":-1068778696\r\n", -1068778696L,
                ":+106877868996\r\n", 106877868996L
        );

        for (Map.Entry<String, Long> entry : cases.entrySet()) {
            assertEquals(entry.getValue(), decoder.decode(ByteBuffer.wrap(entry.getKey().getBytes())).value());
        }

        // incompleteInteger
        Decoder.DecodeResult decodeResult = decoder.decode(ByteBuffer.wrap(":42".getBytes()));
        assertEquals(Decoder.DecodeResult.Status.INCOMPLETE, decodeResult.status());
    }

    @Test
    void decodeSimpleString() {
        Map<String, String> cases = Map.of(
                "+OK\r\n", "OK",
                "+\r\n", ""
        );

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            assertEquals(entry.getValue(), decoder.decode(ByteBuffer.wrap(entry.getKey().getBytes())).value());
        }
    }

    @Test
    void decodeSimpleError() {
        Map<String, String> cases = Map.of(
                "-Error message\r\n", "Error message"
        );

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            assertEquals(entry.getValue(), decoder.decode(ByteBuffer.wrap(entry.getKey().getBytes())).value());
        }
    }

    @Test
    void decodeBulkString() {
        Map<String, String> cases = Map.of(
                "$5\r\nhello\r\n", "hello",
                "$0\r\n\r\n", ""
        );

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            assertEquals(entry.getValue(), decoder.decode(ByteBuffer.wrap(entry.getKey().getBytes())).value());
        }
        assertNull(decoder.decode(ByteBuffer.wrap("$-1\r\n".getBytes())).value());

        // incompleteBulkString
        Decoder.DecodeResult decodeResult = decoder.decode(ByteBuffer.wrap("$4\r\nPI".getBytes()));
        assertEquals(Decoder.DecodeResult.Status.INCOMPLETE, decodeResult.status());
    }

    @Test
    void decodeArray() {
        Map<String, Object[]> cases = Map.of(
                "*0\r\n", new Object[]{},
                "*2\r\n$5\r\nhello\r\n$5\r\nworld\r\n", new String[]{"hello", "world"},
                "*3\r\n:1\r\n:2\r\n:3\r\n", new Long[]{1L, 2L, 3L},
                "*5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$5\r\nhello\r\n", new Object[]{1L, 2L, 3L, 4L, "hello"},
                "*2\r\n*3\r\n:1\r\n:2\r\n:3\r\n*2\r\n+Hello\r\n-World\r\n", new Object[]{new Long[]{1L, 2L, 3L}, new String[]{"Hello", "World"}},
                "*3\r\n$5\r\nhello\r\n$-1\r\n$5\r\nworld\r\n", new String[]{"hello", null, "world"}
        );

        for (Map.Entry<String, Object[]> entry : cases.entrySet()) {
            Object[] arr = (Object[]) decoder.decode(ByteBuffer.wrap(entry.getKey().getBytes())).value();
            assertArrayEquals(entry.getValue(), arr);
        }

        assertNull(decoder.decode(ByteBuffer.wrap("*-1\r\n".getBytes())).value());
    }

}