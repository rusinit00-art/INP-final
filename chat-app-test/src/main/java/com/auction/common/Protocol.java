package com.auction.common;

/**
 * Text-based protocol for client-server communication.
 * Messages are newline-delimited strings.
 */
public final class Protocol {

    public static final int PORT = 6000;

    // Client -> Server commands
    public static final String CMD_JOIN = "JOIN";
    public static final String CMD_BID = "BID";
    public static final String CMD_DISCONNECT = "DISCONNECT";

    // Server -> Client responses
    public static final String MSG_WELCOME = "WELCOME";
    public static final String MSG_BID_UPDATE = "BID_UPDATE";
    public static final String MSG_BID_REJECTED = "BID_REJECTED";
    public static final String MSG_AUCTION_END = "AUCTION_END";
    public static final String MSG_ERROR = "ERROR";
    public static final String MSG_DISCONNECTED = "DISCONNECTED";

    private Protocol() {
    }

    public static String welcome(String itemName, double startingPrice,
                                 double currentBid, String bidder) {
        return String.join("|", MSG_WELCOME, itemName,
                String.valueOf(startingPrice),
                String.valueOf(currentBid),
                bidder == null ? "none" : bidder);
    }

    public static String bidUpdate(double amount, String bidder) {
        return String.join("|", MSG_BID_UPDATE,
                String.valueOf(amount), bidder);
    }

    public static String bidRejected(String reason) {
        return MSG_BID_REJECTED + "|" + reason;
    }

    public static String auctionEnd(String winner, double amount) {
        return String.join("|", MSG_AUCTION_END, winner, String.valueOf(amount));
    }

    public static String error(String message) {
        return MSG_ERROR + "|" + message;
    }
}
