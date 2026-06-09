# Real-Time Auction System

A multi-client auction application built with **Java ServerSocket** (server) and **JavaFX** (client). The server broadcasts live bid updates to all connected bidders and enforces bid validity rules.

## Features

- **Server (console):** Listens on port 6000, handles multiple simultaneous clients, validates bids, logs all activity, and closes the auction when the operator types `END`.
- **Client (JavaFX GUI):** Login with username and server IP, view live highest bid, bid history, place bids, and see winner announcement.

## Requirements

- Java 21+
- Maven 3.6+ (optional — batch scripts work without Maven)

## Quick Start (Windows, no Maven)

1. **Compile:**
   ```
   compile.bat
   ```
2. **Start server** (terminal 1):
   ```
   run-server.bat
   ```
3. **Start clients** (terminal 2, 3, …):
   ```
   run-client.bat
   ```
4. Type `END` in the server console to close the auction.

> JavaFX SDK is bundled under `lib/javafx-sdk-21.0.6/` (downloaded automatically during setup).

## Project Structure

```
src/main/java/com/auction/
├── common/Protocol.java          # Message protocol constants
├── server/
│   ├── AuctionServer.java        # Main console server
│   ├── AuctionState.java         # Shared auction state
│   └── ClientHandler.java        # Per-client thread handler
└── client/
    ├── AuctionClientApp.java     # JavaFX main application
    ├── AuctionConnection.java    # Socket connection handler
    └── LoginDialog.java          # Username/server IP prompt
```

## How to Run

### 1. Build the project

```bash
mvn compile
```

### 2. Start the server

```bash
mvn exec:java
```

Or:

```bash
mvn compile exec:java -Dexec.mainClass="com.auction.server.AuctionServer"
```

The server will display:
- Item: **Vintage Watch**
- Starting price: **LKR 5000**
- Listening on port **6000**

Type `END` and press Enter to close the auction and announce the winner.

### 3. Start one or more clients

Open a **new terminal** for each client:

```bash
mvn javafx:run
```

On launch:
1. Enter your **username**
2. Enter **server IP** (use `localhost` if running locally)
3. Click **Join Auction**

### 4. Place bids

- Enter an amount higher than the current highest bid
- Click **Place Bid**
- All clients see accepted bids in real time
- Rejected bids show an error message only to the bidder who submitted them

### 5. End the auction

In the server console, type:

```
END
```

All clients receive the winner announcement and bidding is disabled.

## Protocol

Messages are newline-delimited text:

| Direction | Format | Description |
|-----------|--------|-------------|
| Client → Server | `JOIN\|username` | Join the auction |
| Client → Server | `BID\|amount` | Place a bid |
| Client → Server | `DISCONNECT` | Leave the auction |
| Server → Client | `WELCOME\|item\|start\|current\|bidder` | Initial state |
| Server → Client | `BID_UPDATE\|amount\|bidder` | New highest bid |
| Server → Client | `BID_REJECTED\|reason` | Bid rejected |
| Server → Client | `AUCTION_END\|winner\|amount` | Auction closed |

## Bid Rules

- Bid must be **higher** than the current highest bid
- Bid must be a **positive number**
- Bids are rejected after the auction ends
