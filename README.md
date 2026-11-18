# Vanish Class Network

## 1. Project Overview

Vanish Class Network is an Android-based real-time messaging application developed as a semester project for the Mobile Application Development course.

The application provides a shared communication space where members of a class can exchange messages through a common WebSocket relay server.

Its main purpose is to make messages temporary. Each delivered message has a 30-second lifetime controlled by a shared expiry timestamp.

The application supports real-time messaging, message expiry countdowns, offline message queuing, and automatic reconnection.

## 2. Project Information

- **Project Name:** Vanish Class Network
- **Course:** Mobile Application Development
- **Semester:** BS Computer Science, 5th Semester
- **Platform:** Android
- **Programming Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Database:** Room Database
- **Communication:** WebSocket

## 3. Main Features

- Real-time messaging between connected class members.
- Shared communication through a WebSocket relay server.
- Automatic message expiry after 30 seconds.
- Countdown timer and progress indicator for delivered messages.
- Local storage of pending messages.
- Automatic reconnection after network interruptions.
- Unique message IDs for identifying messages.
- Automatic deletion of expired local messages.
- Display of connection status and online members.
- Support for multiple connected Android devices.

## 4. Technologies and Libraries

| Technology | Purpose |
|---|---|
| Kotlin | Android application development |
| Jetpack Compose | Building the user interface |
| Room Database | Local message storage |
| OkHttp | WebSocket communication |
| Gson | JSON serialization and parsing |
| Android Studio | Development environment |
| Gradle | Project build and dependency management |

## 5. Project Structure

The repository contains the Android application source code, resources, database implementation, and Gradle configuration.

```text
VanishClassNetwork/
│
├── app/
│   ├── build.gradle.kts
│   │
│   └── src/
│       └── main/
│           │
│           ├── java/
│           │   └── com/
│           │       └── example/
│           │           └── fieldsync/
│           │               │
│           │               ├── MainActivity.kt
│           │               │
│           │               ├── chat/
│           │               │   ├── ChatProtocol.kt
│           │               │   ├── ChatScreen.kt
│           │               │   ├── ChatSocketService.kt
│           │               │   ├── ConnectionSetupScreen.kt
│           │               │   ├── ChatMessageDao.kt
│           │               │   └── ChatMessageEntity.kt
│           │               │
│           │               └── data/
│           │                   ├── AppDatabase.kt
│           │                   ├── ChatMessageDao.kt
│           │                   ├── ChatMessageEntity.kt
│           │                   ├── DatabaseProvider.kt
│           │                   ├── RecordDao.kt
│           │                   ├── RecordEntity.kt
│           │                   ├── SyncApi.kt
│           │                   └── SyncRepository.kt
│           │
│           ├── res/
│           │   ├── drawable/
│           │   ├── mipmap-anydpi-v26/
│           │   └── values/
│           │
│           └── AndroidManifest.xml
│
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── README.md
└── settings.gradle.kts
```

### Important Files and Directories

| File or Directory | Purpose |
|---|---|
| `MainActivity.kt` | Manages the main application flow and message processing. |
| `chat/` | Contains chat interface, protocol, and WebSocket communication files. |
| `ChatProtocol.kt` | Defines message statuses, packet formats, and protocol constants. |
| `ChatScreen.kt` | Displays the chat interface and message countdowns. |
| `ChatSocketService.kt` | Handles WebSocket connections and communication. |
| `data/` | Contains database and existing record-synchronization functionality. |
| `ChatMessageEntity.kt` | Defines the local chat-message database table. |
| `ChatMessageDao.kt` | Provides operations for managing chat messages. |
| `AppDatabase.kt` | Defines the Room database and migration. |
| `DatabaseProvider.kt` | Creates and provides the database instance. |
| `res/` | Contains application resources such as icons and styles. |
| `AndroidManifest.xml` | Contains Android application configuration. |
| `gradle/` | Contains Gradle wrapper files. |
| `build.gradle.kts` | Contains project and module build configurations. |
| `settings.gradle.kts` | Defines the Gradle project configuration. |
| `gradlew` and `gradlew.bat` | Allow Gradle commands to run on Linux/macOS and Windows. |

**Note:** The `chat/` and `data/` directories both contain files named `ChatMessageDao.kt` and `ChatMessageEntity.kt`. The Room database uses the versions in the `data/` package.

## 6. System Architecture

The application uses three main components.

### Android Application

The Android application provides the user interface, manages message creation and display, maintains local message records, and handles WebSocket communication.

### WebSocket Relay Server

A separately hosted relay server connects participating Android devices and forwards messages between connected members.

The relay server is hosted and maintained separately. Its source code is not included in this Android repository.

### Local Database

Room Database stores chat messages locally. It supports offline queuing, message retrieval, status updates, and deletion of expired messages.

### Communication Flow

1. A user creates a message in the Android application.
2. The application saves the message locally.
3. The message is sent through the WebSocket connection when available.
4. The relay forwards the message to connected recipients.
5. The application processes delivery acknowledgements and displays received messages.
6. The application removes messages when their expiry deadline passes.

## 7. Message Lifecycle

The application defines seven message statuses to represent different stages of message processing.

| Status | Description |
|---|---|
| CREATED | Defined status representing a newly created message. |
| WAITING | Message is stored locally while waiting for transmission. |
| SENT | The WebSocket sending method has accepted the packet locally. |
| DELIVERED | A delivery acknowledgement has been received from the relay. |
| DISPLAYED | An incoming message has been added to the receiving device's chat. |
| EXPIRED | Defined status representing a message that has reached its expiry deadline. |
| DROPPED | A pending message is discarded after exceeding its maximum queue age. |

**Implementation note:** The application currently initializes outgoing messages directly as `WAITING`. It does not explicitly assign the `CREATED` or `EXPIRED` statuses during normal processing. Expired messages are removed from the active chat and local database.

### Message Processing

1. When a user sends a message, the application generates a unique message ID.
2. The message is initially assigned the `WAITING` status.
3. The message is saved in the local Room database before transmission is attempted.
4. If a connection is available, the application attempts to send the message through the WebSocket relay.
5. When the WebSocket sending method accepts the packet, the status changes to `SENT`.
6. When a delivery acknowledgement arrives from the relay, the application updates the status to `DELIVERED`.
7. On the receiving device, an incoming message is added to the chat with the `DISPLAYED` status.
8. The message is removed when its shared expiry deadline passes or an expiry notification is received.

**Note:** `SENT` indicates local acceptance by the WebSocket sending method, not confirmed server receipt.

### Offline Message Handling

Messages created while disconnected are stored locally with the `WAITING` status.

The application retries pending messages after the connection is restored.

A pending message has a maximum queue age of 60 seconds. If this limit is reached before transmission, the application marks it as `DROPPED` and deletes its local database record.

The application also retrieves messages with `SENT` status when processing pending messages, allowing them to be retried if an acknowledgement has not been received.

Duplicate prevention across network retries also depends on the relay's handling of unique message IDs.

## 8. Communication Protocol

The Android application communicates with a shared WebSocket relay server using JSON packets.

The OkHttp library manages WebSocket connections, while Gson converts Kotlin data classes into JSON and parses incoming packets.

### Relay Connection

The application connects to the shared relay server:

`wss://vanish-backend-mcls.onrender.com`

The relay server is hosted and maintained separately from this Android repository.

### Connection Process

1. The application opens a WebSocket connection using OkHttp.
2. After the connection opens, the client sends a `JOIN` packet containing the device ID, display name, and optional room code.
3. The server responds with `JOIN_ACK`, including connection information.
4. Once joining is accepted, the application considers itself connected and starts sending periodic heartbeats.
5. If the connection closes or fails, the application updates its connection state and attempts to reconnect.

### Message Communication

The application uses JSON packets with three main fields:

- `version`: identifies the communication protocol version.
- `type`: identifies the packet purpose.
- `payload`: contains the packet's data.

The protocol supports message sending, message delivery, delivery acknowledgements, expiry notifications, joining a room, heartbeats, and disconnection information.

### Heartbeat and Reconnection

The application sends a heartbeat every 15 seconds to monitor the connection.

It also attempts to reconnect automatically after a disconnection.

### Shared Protocol

The protocol definitions are maintained in `ChatProtocol.kt`.

Both the Android application and relay server must follow compatible packet formats and field names for successful communication.

## 9. Message Expiry Mechanism

The application uses a shared expiry timestamp to control how long a delivered message remains visible.

### 30-Second Message Lifetime

The message packet contains:

- `sent_at`: the message timestamp supplied through the relay protocol.
- `expires_at`: the shared expiry deadline.

The receiving application uses `expires_at` to calculate the remaining lifetime. It does not start a new 30-second timer when the message is displayed.

The chat screen updates the countdown approximately every 150 milliseconds and displays the remaining time alongside a progress bar.

### Automatic Expiry

While the chat screen is active, the application checks message expiry approximately every 500 milliseconds.

When a message's expiry deadline passes:

1. The message is removed from the active chat list.
2. Its local Room database record is deleted.
3. An expiry notification received from the relay also causes the corresponding local message to be removed.

### Offline Queue Expiry

Messages waiting for a network connection have a separate maximum queue age of 60 seconds.

The application checks pending messages approximately every 500 milliseconds. If a message reaches this limit before being sent, it is marked as `DROPPED` and deleted from the local database.

This prevents an old offline message from being sent after its queue lifetime has ended.

## 10. Database and Data Management

The Android application uses Room Database to store chat messages locally.

The database contains a table named `chat_messages`, defined by `ChatMessageEntity.kt`.

### Stored Message Information

Each database record contains:

- Unique message ID
- Sender name and device ID
- Message text
- Sending timestamp
- Expiry timestamp
- Current message status
- Local message creation timestamp

The message ID is the primary key, allowing individual messages to be identified and updated.

### Local Storage and Deletion

The application saves outgoing messages locally before attempting network transmission. This allows messages to remain available in the offline queue.

The application performs automatic cleanup:

- Expired messages are deleted from the local database.
- Waiting messages that exceed the 60-second queue limit are marked as dropped and deleted.
- Messages removed through relay expiry notifications are also deleted locally.

The local creation timestamp is used to calculate the age of pending messages, while the shared expiry timestamp controls the lifetime of delivered messages.

## 11. Database Operations

`ChatMessageDao.kt` provides database operations for managing chat records:

- **Insert and Replace:** Saves new messages and replaces existing records with matching IDs.
- **Retrieve Active Messages:** Loads messages that have not reached their expiry deadline.
- **Retrieve Pending Messages:** Finds messages with `WAITING` or `SENT` status.
- **Update Message Status:** Changes the status of an individual message.
- **Update Delivery Information:** Stores delivery status and timestamps.
- **Delete Expired Messages:** Removes records whose expiry deadlines have passed.
- **Delete Individual Messages:** Removes a record using its unique message ID.
- **Clear Database:** Provides an operation to delete all chat records.

## 12. Database Architecture and Migration

The database is defined in `AppDatabase.kt` and contains two entities:

- `RecordEntity`: supports the existing record-synchronization functionality.
- `ChatMessageEntity`: stores chat messages locally.

The database uses version 2 and provides separate DAOs:

- `RecordDao` manages record-related operations.
- `ChatMessageDao` manages chat-message operations.

### Database Migration

The application includes a migration from database version 1 to version 2.

The migration creates the new `chat_messages` table while preserving existing records.

### Database Initialization

`DatabaseProvider.kt` initializes and provides access to the Room database.

The provider uses a singleton pattern so that the application can reuse one shared database instance.

The database is named `field_sync_database` and is created using Room's `databaseBuilder()` function.

The provider registers `MIGRATION_1_2`, allowing Room to upgrade an existing version 1 database to version 2.

## 13. Installation and Setup

### Requirements

Before running the application, make sure you have:

- Android Studio installed.
- Git installed.
- Android SDK configured through Android Studio.
- An Android device or emulator.
- An internet connection.
- Access to the shared WebSocket relay server.

### Step 1: Clone the Repository

Open a terminal and run:

```bash
git clone https://github.com/eishashfaq/VanishClassNetwork.git
```

### Step 2: Navigate to the Project

```bash
cd VanishClassNetwork
```

### Step 3: Open the Project in Android Studio

Open Android Studio, select **Open**, and choose the cloned `VanishClassNetwork` folder.

If the Android Studio command-line launcher is configured, you can also run:

```bash
studio .
```

### Step 4: Synchronize Gradle

After opening the project:

1. Allow Gradle to synchronize.
2. Wait for the required dependencies to download.
3. Make sure the project sync completes successfully.

### Step 5: Build the Application

Open the Android Studio terminal in the project root.

**Windows PowerShell:**

```powershell
.\gradlew.bat assembleDebug
```

**Linux/macOS:**

```bash
./gradlew assembleDebug
```

### Step 6: Run the Application

Connect an Android device using USB debugging or start an Android emulator.

In Android Studio, select the connected device and click the **Run** button.

Alternatively, install the generated debug APK using:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 7: Connect to the Class Network

1. Launch the application.
2. Enter your display name and connection information.
3. Connect to the shared class network.
4. Exchange messages with other connected members.

**Relay Server:** `wss://vanish-backend-mcls.onrender.com`

The relay server is hosted and maintained separately from this Android repository.

## 14. Testing and Verification

The Android application has been tested for the following behaviours:

- Sending and receiving messages between devices.
- Automatic message expiry after 30 seconds.
- Matching expiry countdowns between sender and receiver.
- Countdown behaviour after backgrounding the application.
- Countdown behaviour after screen rotation.
- Expiry of pending offline messages after 60 seconds.
- Prevention of expired pending messages from being sent after reconnection.
- Successful sending of new messages after reconnecting.
- Prevention of duplicate sending in the tested offline reconnection scenario.
- Removal of expired messages after closing and reopening the application.

These tests verify the tested application scenarios, but do not guarantee correct behaviour under every possible network interruption, device-clock difference, or relay-server failure.

## 15. Limitations and Security Considerations

- Message deletion from the Android Room database does not guarantee deletion of screenshots, copied text, notifications, or other copies outside the application.
- The Android application cannot guarantee that a recipient has not captured or copied a message before expiry.
- The countdown uses the device's system clock and does not currently correct clock differences using server time.
- The Android application stores pending outgoing messages locally, but recipient-side offline delivery depends on relay-server functionality.
- Automatic local expiry checks run while the chat screen is active. Expired records are also checked and cleaned up when active messages are loaded again.
- The relay server is maintained separately, so its internal storage and deletion behaviour are outside the scope of this Android repository.

## 16. Contributions and Acknowledgements

The Android application was developed as part of a paired Mobile Application Development semester project.

The WebSocket relay server is hosted and maintained separately from this Android repository.

External libraries and development resources used in the project should be acknowledged according to their respective documentation and licenses. Any AI assistance, tutorials, or external code references used during development should also be acknowledged where applicable.