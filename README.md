
# VanishClassNetwork

VanishClassNetwork is an Android-based real-time chat application developed as a semester project for the Mobile Application Development course.

The application allows members of a shared class network to exchange messages through a common WebSocket relay. Each message has a shared expiry deadline intended to make it disappear from the chat after 30 seconds.

## Project Information

- **Project Name:** VanishClassNetwork
- **Course:** Mobile Application Development
- **Semester:** BS Computer Science – 5th Semester
- **Platform:** Android
- **Development Language:** Kotlin

## Main Features

- Connects to a shared WebSocket relay server.
- Allows users to enter a display name and device ID.
- Supports real-time message sending and receiving.
- Uses a shared expiry timestamp for message expiration.
- Displays a countdown for messages.
- Supports sender-side offline message queuing.
- Prevents expired queued messages from being sent.
- Handles reconnection when internet connectivity returns.
- Maintains message expiry timing when the app is backgrounded or rotated.

## Technologies Used

- Kotlin
- Android Studio
- Jetpack Compose
- Room Database
- OkHttp WebSocket
- Gson
- Gradle

## Relay Server

The application connects to the group's shared relay server:

`wss://vanish-backend-mcls.onrender.com`

The relay forwards messages between connected group members.

## How to Run the Application

1. Install Android Studio.
2. Clone or download this repository.
3. Open the project folder in Android Studio.
4. Allow Gradle synchronization to complete.
5. Connect an Android phone or start an emulator.
6. Build and run the application.

### Build Using Terminal

On Windows, run:

```powershell
.\gradlew.bat assembleDebug
```

The generated debug APK is normally located at:

`app/build/outputs/apk/debug/app-debug.apk`

## Testing

The application should be tested using at least two Android devices connected to the same relay server.

Important tests include:

- Sending and receiving messages.
- Checking the 30-second expiry countdown.
- Confirming that messages disappear after expiry.
- Testing offline message queuing.
- Reconnecting to the relay.
- Checking that expired queued messages are not delivered.
- Checking that messages are not duplicated after reconnection.
- Testing countdown behavior after backgrounding or rotating the application.

## Message Expiry and Limitations

Messages use a shared expiry deadline intended to provide a 30-second lifetime.

Message disappearance inside the application does not guarantee that a recipient cannot preserve its content. Screenshots, copying, external recording, or other actions may preserve message content outside the application's control.

## Academic Project

This project was developed for the Mobile Application Development course as a semester project.
