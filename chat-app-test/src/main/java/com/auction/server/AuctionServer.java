package com.auction.server;

import com.auction.common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class AuctionServer {

    private static final String ITEM_NAME = "Vintage Watch";
    private static final double STARTING_PRICE = 5000.0;

    private final AuctionState auctionState = new AuctionState(ITEM_NAME, STARTING_PRICE);
    private final List<ClientHandler> clients = new ArrayList<>();
    private ServerSocket serverSocket;

    public AuctionState getAuctionState() {
        return auctionState;
    }

    public synchronized void registerClient(ClientHandler handler) {
        clients.add(handler);
    }

    public synchronized void unregisterClient(ClientHandler handler) {
        clients.remove(handler);
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void start() {
        log("=== Real-Time Auction Server ===");
        log("Item: " + ITEM_NAME);
        log("Starting price: LKR " + String.format("%.0f", STARTING_PRICE));
        log("Listening on port " + Protocol.PORT);
        log("Type END and press Enter to close the auction.");
        log("--------------------------------");

        Thread consoleThread = new Thread(this::handleConsoleInput, "ConsoleInput");
        consoleThread.setDaemon(true);
        consoleThread.start();

        try (ServerSocket socket = new ServerSocket(Protocol.PORT)) {
            this.serverSocket = socket;
            while (auctionState.isAuctionOpen()) {
                try {
                    Socket clientSocket = socket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this, this::log);
                    Thread thread = new Thread(handler, "Client-" + clientSocket.getPort());
                    thread.setDaemon(true);
                    thread.start();
                } catch (IOException e) {
                    if (auctionState.isAuctionOpen()) {
                        log("[ERROR] Failed to accept client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log("[FATAL] Server error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void handleConsoleInput() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (auctionState.isAuctionOpen() && scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();
                if ("END".equalsIgnoreCase(input)) {
                    endAuction();
                    break;
                } else if (!input.isEmpty()) {
                    log("[INFO] Unknown command: '" + input + "'. Type END to close the auction.");
                }
            }
        }
    }

    private synchronized void endAuction() {
        if (!auctionState.isAuctionOpen()) {
            return;
        }

        auctionState.closeAuction();

        String winner = auctionState.getCurrentBidder();
        double winningBid = auctionState.getCurrentHighestBid();

        if (winner == null) {
            log("[AUCTION END] No bids received. Item unsold.");
            broadcast(Protocol.auctionEnd("No winner", STARTING_PRICE));
        } else {
            log("[AUCTION END] Winner: " + winner + " with LKR " + String.format("%.0f", winningBid));
            broadcast(Protocol.auctionEnd(winner, winningBid));
        }

        log("Server shutting down. Disconnecting all clients...");
    }

    private void shutdown() {
        synchronized (this) {
            for (ClientHandler client : new ArrayList<>(clients)) {
                client.sendMessage(Protocol.MSG_DISCONNECTED + "|Server closed");
            }
            clients.clear();
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        log("Server stopped.");
    }

    private void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}
