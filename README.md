# VANISH — Vanish Class Network

### A Real-Time Messaging Application with Automatically Disappearing Messages

VANISH (Vanish Class Network) is an Android-based messaging application developed as a university Mobile Application Development project. It provides a shared communication space where students can join a common class room and exchange messages in real time.

The application uses WebSocket communication through an externally hosted relay server. Each delivered message has a limited lifetime of 30 seconds and is automatically removed from the visible chat and applicable local message storage after expiration.

VANISH also supports temporary offline message queuing, automatic reconnection, local message storage, and message lifecycle management.

## Features

- **Shared Class Room:** Allows students to connect to a common class communication space.
- **Real-Time Messaging:** Exchanges messages through WebSocket communication.
- **Disappearing Messages:** Delivered messages expire after 30 seconds.
- **Live Countdown:** Displays the remaining lifetime of each delivered message.
- **Message Length Limit:** Supports messages containing up to 280 characters.
- **Offline Message Queue:** Temporarily stores outgoing messages when the connection is unavailable.
- **Automatic Reconnection:** Attempts to restore the WebSocket connection after interruptions.
- **Message Identification:** Uses unique message IDs to identify individual messages.
- **Heartbeat Mechanism:** Periodically checks the connection with the relay.
- **Local Database:** Stores messages and their delivery information using Room Database.
- **Connection Status:** Displays connection information and the number of online members.
- **Settings Screen:** Allows users to manage their display name and relay connection settings.

## Technology Stack

| Technology | Version / Usage |
|---|---|
| Kotlin | Application programming language |
| Android | Mobile application platform |
| Jetpack Compose | User interface development |
| Room Database | Local message storage |
| OkHttp | WebSocket communication |
| Gson | JSON serialization and parsing |
| Gradle | Build and dependency management |
| Android Studio | Development environment |

## Development Environment

The application was developed using the following tools:

| Tool | Purpose |
|---|---|
| Android Studio | Android development |
| JDK | Java development environment |
| Android SDK | Android build and platform tools |
| Gradle | Project compilation |
| Git | Version control |
| GitHub | Source code hosting |

## Application Screens

VANISH provides a simple interface designed for class communication.

- **Connection Setup Screen:** Allows users to enter their display name and connection information before joining the class network.
- **Chat Screen:** Displays messages, sender information, connection status, online members, and message countdowns.
- **Settings Screen:** Provides options for managing profile and relay connection settings.

## Communication Architecture

VANISH communicates through an externally hosted WebSocket relay server.

The Android application connects to the relay using the OkHttp WebSocket client. The relay handles communication between connected students in the shared class room.

The relay server is hosted and maintained separately. Its source code is not included in this Android repository.

### How Communication Works

1. A student enters their display name and joins the class network.
2. The application establishes a WebSocket connection with the relay.
3. The application sends messages through the established connection.
4. The relay forwards messages to connected participants.
5. Receiving devices process and display incoming messages.
6. Delivery and expiry information is handled through protocol packets.

## Message Communication Protocol

VANISH uses a JSON-based communication protocol.

Each packet contains three main fields:

| Field | Description |
|---|---|
| `version` | Identifies the protocol version. |
| `type` | Specifies the packet type. |
| `payload` | Contains the packet data. |

The protocol supports the following packet types:

| Packet Type | Purpose |
|---|---|
| `JOIN` | Requests to join the class network. |
| `JOIN_ACK` | Responds to a join request. |
| `MESSAGE_SEND` | Sends a new message. |
| `MESSAGE_DELIVER` | Delivers a message to a recipient. |
| `DELIVERY_ACK` | Provides delivery acknowledgement. |
| `MESSAGE_EXPIRED` | Announces message expiration. |
| `HEARTBEAT` | Checks the connection. |
| `HEARTBEAT_ACK` | Responds to a heartbeat. |
| `DISCONNECT` | Represents a disconnection notification. |

The protocol definitions and message data classes are maintained in `ChatProtocol.kt`.

## Message Lifecycle

The application defines seven message states to represent different stages of message processing.

| State | Description |
|---|---|
| `CREATED` | Defined state for a newly created message. |
| `WAITING` | Message is waiting for a network connection. |
| `SENT` | WebSocket sending method has accepted the packet locally. |
| `DELIVERED` | Delivery acknowledgement has been received. |
| `DISPLAYED` | Incoming message has been added to the chat. |
| `EXPIRED` | Defined state representing an expired message. |
| `DROPPED` | Pending message is discarded after exceeding its queue lifetime. |

**Implementation Note:** New outgoing messages are initialized directly as `WAITING`. `CREATED` and `EXPIRED` are defined in the protocol but are not explicitly assigned during the normal message-processing flow. Expired messages are removed from the chat and local database.

## Message Expiration

Each delivered message has a lifetime of **30 seconds**.

- The message uses the shared `sent_at` and `expires_at` timestamps provided through the communication protocol.
- The receiving application calculates the remaining lifetime using `expires_at`.
- The countdown and progress indicator update continuously.
- Expired messages are removed from the visible chat.
- Applicable expired message records are deleted from the local Room Database.

Expiration is based on timestamps rather than starting a fresh 30-second timer whenever a message appears on a device.

## Offline Message Handling

VANISH includes a temporary offline message queue.

When the network connection is unavailable:

1. Outgoing messages are saved in the local Room Database.
2. Messages remain in the `WAITING` state.
3. The application attempts to reconnect automatically.
4. Eligible pending messages are retried after reconnection.
5. Messages older than 60 seconds in the queue are discarded.

Unique message IDs help identify messages during retries. Duplicate prevention also depends on the relay's handling of repeated message IDs.

## Connection Management

The application uses OkHttp to maintain its WebSocket connection.

Important connection features include:

- Automatic connection-state updates.
- Periodic heartbeat packets every 15 seconds.
- Heartbeat acknowledgements from the relay.
- Automatic reconnection attempts after connection failure.
- Pending message processing after reconnection.

The application uses the WebSocket sending result to determine whether a packet was accepted by the local sending mechanism. This does not independently confirm that the server received the packet.

## Local Data Storage

VANISH uses **Room Database** for local message management.

The database stores:

- Message IDs
- Sender information
- Message text
- Device IDs
- Message status
- Sent and expiration timestamps
- Local message creation timestamps

The database implementation contains:

| Component | Purpose |
|---|---|
| `ChatMessageEntity.kt` | Defines the chat-message database table. |
| `ChatMessageDao.kt` | Provides database operations for messages. |
| `AppDatabase.kt` | Defines the Room database and migration. |
| `DatabaseProvider.kt` | Provides a shared database instance. |

The application uses database version 2, with a migration from version 1 to version 2 to introduce the chat-message table while preserving existing records.

## Project Structure

```text
VanishClassNetwork/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/example/fieldsync/
│   │       │       │
│   │       │       ├── MainActivity.kt
│   │       │       │
│   │       │       ├── chat/
│   │       │       │   ├── ChatProtocol.kt
│   │       │       │   ├── ChatScreen.kt
│   │       │       │   ├── ChatSocketService.kt
│   │       │       │   ├── ConnectionSetupScreen.kt
│   │       │       │   ├── ChatMessageDao.kt
│   │       │       │   └── ChatMessageEntity.kt
│   │       │       │
│   │       │       └── data/
│   │       │           ├── AppDatabase.kt
│   │       │           ├── ChatMessageDao.kt
│   │       │           ├── ChatMessageEntity.kt
│   │       │           ├── DatabaseProvider.kt
│   │       │           ├── RecordDao.kt
│   │       │           ├── RecordEntity.kt
│   │       │           ├── SyncApi.kt
│   │       │           └── SyncRepository.kt
│   │       │
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   │
│   └── build.gradle.kts
│
├── gradle/
│   └── wrapper/
│
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── README.md
└── settings.gradle.kts
```

**Note:** The `chat/` and `data/` packages contain files with the same DAO and entity names. Room uses the implementations in the `data/` package.

## Requirements

To build and run VANISH, you need:

- Android Studio
- Java Development Kit (JDK)
- Android SDK
- Git
- Android Emulator or a physical Android device
- Internet connection for communication through the hosted relay

## Installation and Setup

### 1. Clone the Repository

```bash
git clone https://github.com/eishashfaq/VanishClassNetwork.git
```

### 2. Navigate to the Project

```bash
cd VanishClassNetwork
```

### 3. Open the Project

Open the cloned project folder in Android Studio.

### 4. Synchronize Gradle

Allow Android Studio to download dependencies and complete Gradle synchronization.

### 5. Build the Application

**Windows PowerShell:**

```powershell
.\gradlew.bat assembleDebug
```

**Linux/macOS:**

```bash
./gradlew assembleDebug
```

### 6. Run the Application

Connect an Android device or start an emulator, select it in Android Studio, and click **Run**.

Alternatively, install the generated APK using:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing and Verification

The application has been tested for the following scenarios:

- Sending and receiving messages between Android devices.
- Automatic message expiration after 30 seconds.
- Matching expiry countdowns on sender and receiver devices.
- Countdown behaviour after backgrounding the application.
- Countdown behaviour after screen rotation.
- Expiration of offline messages after 60 seconds.
- Preventing expired queued messages from being sent after reconnection.
- Sending new messages after reconnecting.
- Duplicate prevention in the tested offline reconnection scenario.
- Removal of expired messages after closing and reopening the application.

These tests cover the scenarios listed above and do not guarantee behaviour under every possible network or device condition.

## Important Notes and Limitations

- The WebSocket relay server is hosted and maintained separately from this Android repository.
- No locally running backend server is required to use the application.
- An internet connection is required for real-time communication.
- The Android application stores pending outgoing messages locally.
- The countdown uses the device's system clock and does not currently correct clock differences using server time.
- Recipient-side offline message delivery depends on the relay server's functionality.
- Local message deletion cannot guarantee removal of screenshots, copied text, notifications, or information saved outside the application.
- Relay-side storage and deletion behaviour is not documented by this Android repository.

## Project Information

| Detail | Description |
|---|---|
| Project Name | VANISH — Vanish Class Network |
| Project Type | Real-Time Messaging Application |
| Course | Mobile Application Development |
| Semester | BS Computer Science — 5th Semester |
| Platform | Android |
| Programming Language | Kotlin |
| UI Framework | Jetpack Compose |
| Communication | WebSocket |
| Local Database | Room Database |
| Message Lifetime | 30 Seconds |
| Repository | [VanishClassNetwork on GitHub](https://github.com/eishashfaq/VanishClassNetwork) |

## Acknowledgements

This project was developed as part of the Mobile Application Development course.

The Android application and its communication protocol were developed as part of the semester project. The WebSocket relay server is maintained separately.

External libraries, documentation, tutorials, and AI assistance used during development should be acknowledged where applicable.