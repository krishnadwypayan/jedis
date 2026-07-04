package com.krishna.jedis.server;

import com.krishna.jedis.cmd.CommandHandler;
import com.krishna.jedis.cmd.JedisCmd;
import com.krishna.jedis.resp.Decoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;

import static com.krishna.jedis.utils.JedisConstants.HOST;
import static com.krishna.jedis.utils.JedisConstants.PORT;

public class JedisServer {

    private static final Logger LOGGER = LogManager.getLogger(JedisServer.class);
    private int clientConnections = 0;
    private final Decoder decoder;
    private final CommandHandler commandHandler;

    public JedisServer(Decoder decoder, CommandHandler commandHandler) {
        this.decoder = decoder;
        this.commandHandler = commandHandler;
    }

    public void startServer() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(HOST, PORT));
            LOGGER.info("Started Jedis server at {}:{}", HOST, PORT);

            try (Selector selector = Selector.open()) {
                // Register with a Selector — only ACCEPT makes sense here
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

                while (true) {
                    // Blocks until atleast one channel is ready
                    selector.select();

                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        iterator.remove();

                        try {
                            if (selectionKey.isAcceptable()) {
                                // accept new connection
                                acceptClient(serverSocketChannel, selector);
                            } else if (selectionKey.isReadable()) {
                                // existing client sent some data
                                respond(selectionKey);
                            } else if (selectionKey.isWritable()) {
                                writeResponse(selectionKey);
                                drainBuffer(selectionKey);
                            }
                        } catch (IOException e) {
                            LOGGER.error("Error handling client, closing connection", e);
                            closeClient(selectionKey);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to start server", ex);
        }
    }

    private void acceptClient(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverSocketChannel.accept();
        if (Objects.nonNull(clientChannel)) {
            ClientState state = new ClientState(
                    ByteBuffer.allocate(1024),
                    ByteBuffer.allocate(1024)
            );

            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ, state);

            clientConnections++;
            LOGGER.info("Client connected: {}. Number of clients connected: {}", clientChannel.getRemoteAddress(), clientConnections);
        } else {
            LOGGER.error("Client channel is null");
        }
    }

    private void respond(SelectionKey selectionKey) throws IOException {
        ClientState state = (ClientState) selectionKey.attachment();
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        int bytes = channel.read(state.readBuf());
        if (bytes > 0) {
            // you got data. flip() the buffer, then read bytes out of it.
            state.readBuf().flip();

            if (LOGGER.isDebugEnabled()) {
                String message = StandardCharsets.UTF_8.decode(state.readBuf().duplicate()).toString()
                        .replace("\r", "\\r")
                        .replace("\n", "\\n");
                LOGGER.debug("Got message: '{}' from client: {}", message, channel.getRemoteAddress());
            }

            state.writeBuf().clear();
            while (state.readBuf().hasRemaining()) {
                try {
                    Decoder.DecodeResult decodeResult = decoder.decode(state.readBuf());
                    if (Decoder.DecodeResult.Status.ERROR == decodeResult.status()) {
                        closeClient(selectionKey);
                        return;
                    } else if (Decoder.DecodeResult.Status.INCOMPLETE == decodeResult.status()) {
                        break;
                    }

                    // SUCCESS path
                    JedisCmd jedisCmd = commandHandler.parse(decodeResult.value());
                    String response = commandHandler.evaluate(jedisCmd);
                    state.writeBuf().put(response.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    LOGGER.error("Error handling client", e);
                    closeClient(selectionKey);
                    return;
                }
            }
            state.writeBuf().flip();  // ready for writing
            writeResponse(selectionKey);
            drainBuffer(selectionKey);
            state.readBuf().compact();
        } else if (bytes == 0) {
            // nothing available right now (rare in NIO, but possible). Do nothing.
        } else {
            // client closed the connection
            closeClient(selectionKey);
        }
    }

    private void writeResponse(SelectionKey selectionKey) throws IOException {
        ByteBuffer buf = ((ClientState) selectionKey.attachment()).writeBuf();
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        channel.write(buf);
    }

    private void drainBuffer(SelectionKey selectionKey) {
        ByteBuffer buf = ((ClientState) selectionKey.attachment()).writeBuf();
        if (buf.hasRemaining()) {
            // partial write — bytes are still in the buffer
            // register OP_WRITE and come back later
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
        } else {
            // fully written
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
            buf.clear();
        }
    }

    private void closeClient(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.close();
        key.cancel();
        clientConnections--;
        LOGGER.info("Client disconnected. Remaining: {}", clientConnections);
    }

    record ClientState(ByteBuffer readBuf, ByteBuffer writeBuf) {}

}
