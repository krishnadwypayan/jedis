package com.krishna.jedis.resp;

public sealed interface RespValue permits RespArray, RespBulkString, RespError, RespInteger, RespSimpleString {
}
