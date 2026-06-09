package com.auction.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Simple login dialog to collect username and server IP before connecting.
 */
public class LoginDialog {

    private String username;
    private String serverHost;
    private boolean confirmed;

    public boolean showAndWait() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Join Auction");

        Label userLabel = new Label("Username:");
        TextField userField = new TextField();
        userField.setPromptText("Your name");

        Label hostLabel = new Label("Server IP:");
        TextField hostField = new TextField("localhost");
        hostField.setPromptText("e.g. localhost");

        Button joinButton = new Button("Join Auction");
        Button cancelButton = new Button("Cancel");

        joinButton.setDefaultButton(true);
        joinButton.setOnAction(e -> {
            String user = userField.getText().trim();
            String host = hostField.getText().trim();

            if (user.isEmpty()) {
                userField.setStyle("-fx-border-color: red;");
                return;
            }
            if (host.isEmpty()) {
                hostField.setStyle("-fx-border-color: red;");
                return;
            }

            username = user;
            serverHost = host;
            confirmed = true;
            dialog.close();
        });

        cancelButton.setOnAction(e -> {
            confirmed = false;
            dialog.close();
        });

        HBox buttons = new HBox(10, joinButton, cancelButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(userLabel, 0, 0);
        grid.add(userField, 1, 0);
        grid.add(hostLabel, 0, 1);
        grid.add(hostField, 1, 1);
        grid.add(buttons, 1, 2);

        dialog.setScene(new Scene(grid, 360, 160));
        dialog.showAndWait();

        return confirmed;
    }

    public String getUsername() {
        return username;
    }

    public String getServerHost() {
        return serverHost;
    }
}
