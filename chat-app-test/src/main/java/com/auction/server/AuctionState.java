package com.auction.server;

/**
 * Thread-safe shared auction state managed by the server.
 */
public class AuctionState {

    private final String itemName;
    private final double startingPrice;
    private double currentHighestBid;
    private String currentBidder;
    private boolean auctionOpen;

    public AuctionState(String itemName, double startingPrice) {
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.currentBidder = null;
        this.auctionOpen = true;
    }

    public synchronized String getItemName() {
        return itemName;
    }

    public synchronized double getStartingPrice() {
        return startingPrice;
    }

    public synchronized double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public synchronized String getCurrentBidder() {
        return currentBidder;
    }

    public synchronized boolean isAuctionOpen() {
        return auctionOpen;
    }

    /**
     * Attempts to place a bid. Returns true if accepted, false if rejected.
     */
    public synchronized boolean placeBid(String bidder, double amount) {
        if (!auctionOpen) {
            return false;
        }
        if (amount <= currentHighestBid) {
            return false;
        }
        currentHighestBid = amount;
        currentBidder = bidder;
        return true;
    }

    public synchronized void closeAuction() {
        auctionOpen = false;
    }
}
