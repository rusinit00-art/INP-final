package com.auction.server;

import com.auction.common.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private final AuctionServer server;
    private final Consumer<String> logger;

    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, AuctionServer server, Consumer<String> logger) {
        this.socket = socket;
        this.server = server;
        this.logger = logger;
    }
    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                if (!handleMessage(line)) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.accept("[ERROR] Connection error with " + describeClient() + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private boolean handleMessage(String line) {
        String[] parts = line.split("\\|", 2);
        String command = parts[0].trim().toUpperCase();
        String payload = parts.length > 1 ? parts[1].trim() : "";

        switch (command) {
            case Protocol.CMD_JOIN -> {
                if (username != null) {
                    sendMessage(Protocol.error("Already joined as " + username));
                    return true;
                }
                if (payload.isEmpty()) {
                    sendMessage(Protocol.error("Username is required"));
                    return true;
                }
                username = payload;
                server.registerClient(this);
                logger.accept("[CONNECT] " + username + " joined from " + socket.getRemoteSocketAddress());

                AuctionState state = server.getAuctionState();
                sendMessage(Protocol.welcome(
                        state.getItemName(),
                        state.getStartingPrice(),
                        state.getCurrentHighestBid(),
                        state.getCurrentBidder()
                ));
            }
            case Protocol.CMD_BID -> {
                if (username == null) {
                    sendMessage(Protocol.error("Join with a username before placing bids"));
                    return true;
                }
                handleBid(payload);
            }
            case Protocol.CMD_DISCONNECT -> {
                logger.accept("[DISCONNECT] " + describeClient() + " requested disconnect");
                return false;
            }
            default -> sendMessage(Protocol.error("Unknown command: " + command));
        }
        return true;
    }

    private void handleBid(String payload) {
        AuctionState state = server.getAuctionState();

        if (!state.isAuctionOpen()) {
            sendMessage(Protocol.bidRejected("The auction has already ended"));
            logger.accept("[BID REJECTED] " + username + " - auction closed");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(payload);
        } catch (NumberFormatException e) {
            sendMessage(Protocol.bidRejected("Invalid bid amount: " + payload));
            logger.accept("[BID REJECTED] " + username + " - invalid amount: " + payload);
            return;
        }

        if (amount <= 0) {
            sendMessage(Protocol.bidRejected("Bid must be a positive amount"));
            logger.accept("[BID REJECTED] " + username + " - non-positive amount: LKR " + amount);
            return;
        }

        synchronized (state) {
            double currentBid = state.getCurrentHighestBid();
            if (amount <= currentBid) {
                String reason = "Bid must be higher than current highest bid (LKR "
                        + String.format("%.0f", currentBid) + ")";
                sendMessage(Protocol.bidRejected(reason));
                logger.accept("[BID REJECTED] " + username + " bid LKR " + amount
                        + " (current: LKR " + currentBid + ")");
                return;
            }

            state.placeBid(username, amount);
            logger.accept("[BID ACCEPTED] " + username + " bid LKR " + amount);
            server.broadcast(Protocol.bidUpdate(amount, username));
        }
    }

    private void cleanup() {
        server.unregisterClient(this);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        if (username != null) {
            logger.accept("[DISCONNECT] " + username + " left the auction");
        } else {
            logger.accept("[DISCONNECT] Unregistered client from " + socket.getRemoteSocketAddress());
        }
    }

    private String describeClient() {
        return username != null ? username : socket.getRemoteSocketAddress().toString();
    }
}
