package com.auction.client;

import com.auction.common.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Manages the TCP socket connection between the JavaFX client and the auction server.
 */
public class AuctionConnection {

    private final String serverHost;
    private final String username;
    private final Consumer<String> onMessage;
    private final Runnable onDisconnected;

    private Socket socket;
    private PrintWriter out;
    private Thread listenerThread;

    public AuctionConnection(String serverHost, String username,
                             Consumer<String> onMessage,
                             Runnable onDisconnected) {
        this.serverHost = serverHost;
        this.username = username;
        this.onMessage = onMessage;
        this.onDisconnected = onDisconnected;
    }

    public void connect() throws IOException {
        socket = new Socket(serverHost, Protocol.PORT);
        out = new PrintWriter(socket.getOutputStream(), true);

        listenerThread = new Thread(this::listen, "ServerListener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        send(Protocol.CMD_JOIN + "|" + username);
    }

    private void listen() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                onMessage.accept(line);
            }
        } catch (IOException e) {
            onMessage.accept(Protocol.MSG_ERROR + "|Connection lost: " + e.getMessage());
        } finally {
            onDisconnected.run();
        }
    }

    public void sendBid(double amount) {
        send(Protocol.CMD_BID + "|" + amount);
    }

    public void disconnect() {
        if (out != null) {
            send(Protocol.CMD_DISCONNECT);
        }
        close();
    }

    private void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
