package com.auction.client;

import com.auction.common.Protocol;
import javafx.application.Application;
import java.io.IOException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * JavaFX client for the real-time auction system.
 */
public class AuctionClientApp extends Application {

    private Label itemLabel;
    private Label startingPriceLabel;
    private Label highestBidLabel;
    private Label winnerLabel;
    private TextArea historyArea;
    private TextField bidField;
    private Button placeBidButton;
    private Button disconnectButton;

    private AuctionConnection connection;
    private boolean auctionEnded;

    @Override
    public void start(Stage stage) {
        LoginDialog login = new LoginDialog();
        if (!login.showAndWait()) {
            Platform.exit();
            return;
        }

        String username = login.getUsername();
        String serverHost = login.getServerHost();

        stage.setTitle("Auction Client - " + username);
        stage.setScene(buildMainScene(username, serverHost));
        stage.setOnCloseRequest(e -> {
            disconnect();
            Platform.exit();
        });
        stage.show();

        connectToServer(username, serverHost);
    }

    private Scene buildMainScene(String username, String serverHost) {
        Label titleLabel = new Label("Live Auction");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));

        itemLabel = new Label("Item: Connecting...");
        itemLabel.setFont(Font.font(14));

        startingPriceLabel = new Label("Starting Price: --");
        startingPriceLabel.setFont(Font.font(14));

        highestBidLabel = new Label("Current Highest Bid: --");
        highestBidLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        highestBidLabel.setStyle("-fx-text-fill: #1a5f2a;");

        winnerLabel = new Label("");
        winnerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        winnerLabel.setStyle("-fx-text-fill: #8b0000;");
        winnerLabel.setWrapText(true);
        winnerLabel.setVisible(false);

        historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setWrapText(true);
        historyArea.setPrefRowCount(12);
        VBox.setVgrow(historyArea, Priority.ALWAYS);

        bidField = new TextField();
        bidField.setPromptText("Enter bid amount (LKR)");
        HBox.setHgrow(bidField, Priority.ALWAYS);

        placeBidButton = new Button("Place Bid");
        placeBidButton.setOnAction(e -> placeBid());

        disconnectButton = new Button("Disconnect");
        disconnectButton.setOnAction(e -> {
            disconnect();
            Platform.exit();
        });

        HBox bidBox = new HBox(10, bidField, placeBidButton, disconnectButton);
        bidBox.setAlignment(Pos.CENTER_LEFT);

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(6);
        infoGrid.add(itemLabel, 0, 0);
        infoGrid.add(startingPriceLabel, 0, 1);
        infoGrid.add(highestBidLabel, 0, 2);

        VBox root = new VBox(12, titleLabel, infoGrid, winnerLabel, historyArea, bidBox);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.TOP_LEFT);

        appendHistory("Connecting to " + serverHost + " as " + username + "...");

        return new Scene(root, 520, 480);
    }

    private void connectToServer(String username, String serverHost) {
        connection = new AuctionConnection(
                serverHost,
                username,
                this::handleServerMessage,
                () -> Platform.runLater(this::onConnectionLost)
        );

        Thread connectThread = new Thread(() -> {
            try {
                connection.connect();
                Platform.runLater(() -> appendHistory("Connected to auction server."));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    appendHistory("Failed to connect: " + e.getMessage());
                    disableBidding();
                });
            }
        }, "ConnectThread");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void handleServerMessage(String message) {
        Platform.runLater(() -> processMessage(message));
    }

    private void processMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length == 0) {
            return;
        }

        String type = parts[0];

        switch (type) {
            case Protocol.MSG_WELCOME -> {
                if (parts.length >= 5) {
                    itemLabel.setText("Item: " + parts[1]);
                    startingPriceLabel.setText("Starting Price: LKR " + formatAmount(parts[2]));
                    updateHighestBid(parts[3], parts[4]);
                    appendHistory("Joined auction for " + parts[1] + ".");
                }
            }
            case Protocol.MSG_BID_UPDATE -> {
                if (parts.length >= 3) {
                    updateHighestBid(parts[1], parts[2]);
                    appendHistory("New highest bid: LKR " + formatAmount(parts[1]) + " by " + parts[2]);
                }
            }
            case Protocol.MSG_BID_REJECTED -> {
                String reason = parts.length >= 2 ? parts[1] : "Bid rejected";
                appendHistory("[REJECTED] " + reason);
            }
            case Protocol.MSG_AUCTION_END -> {
                if (parts.length >= 3) {
                    String winner = parts[1];
                    String amount = parts[2];
                    showWinner(winner, amount);
                }
            }
            case Protocol.MSG_ERROR -> {
                String error = parts.length >= 2 ? parts[1] : "Unknown error";
                appendHistory("[ERROR] " + error);
            }
            case Protocol.MSG_DISCONNECTED -> {
                appendHistory("Disconnected from server.");
                disableBidding();
            }
            default -> appendHistory("[SERVER] " + message);
        }
    }

    private void updateHighestBid(String amount, String bidder) {
        String bidderText = "none".equalsIgnoreCase(bidder) ? "No bids yet" : bidder;
        highestBidLabel.setText("Current Highest Bid: LKR " + formatAmount(amount) + " (" + bidderText + ")");
    }

    private void showWinner(String winner, String amount) {
        auctionEnded = true;
        disableBidding();

        if ("No winner".equalsIgnoreCase(winner)) {
            winnerLabel.setText("Auction ended — no bids were placed.");
        } else {
            winnerLabel.setText("AUCTION OVER! Winner: " + winner
                    + " with LKR " + formatAmount(amount));
        }
        winnerLabel.setVisible(true);
        appendHistory("=== Auction ended. Winner: " + winner + " (LKR " + formatAmount(amount) + ") ===");
    }

    private void placeBid() {
        if (auctionEnded || connection == null) {
            return;
        }

        String text = bidField.getText().trim();
        if (text.isEmpty()) {
            appendHistory("[REJECTED] Please enter a bid amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(text);
            connection.sendBid(amount);
            bidField.clear();
        } catch (NumberFormatException e) {
            appendHistory("[REJECTED] Invalid number: " + text);
        }
    }

    private void disableBidding() {
        bidField.setDisable(true);
        placeBidButton.setDisable(true);
    }

    private void onConnectionLost() {
        if (!auctionEnded) {
            appendHistory("Connection to server lost.");
            disableBidding();
        }
    }

    private void disconnect() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    private void appendHistory(String text) {
        historyArea.appendText(text + System.lineSeparator());
    }

    private String formatAmount(String amount) {
        try {
            return String.format("%.0f", Double.parseDouble(amount));
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
