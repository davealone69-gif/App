# Free App Maker

A fully offline-capable Android application built with Kotlin, Jetpack Compose, and Room Database. The Free App Maker allows users to create and run clean, no-code custom database apps instantly via manual screens or interactive text prompts powered by the Gemini API.

## 🚀 Features

*   **No-Code App Creation:** Build your own custom data-entry and management mini-apps directly from your phone.
*   **AI-Powered Generation:** Describe the app you want to build (e.g., "A workout tracker" or "A CRM for my clients") and the Gemini agent will automatically define the fields and structure for you.
*   **Dynamic Custom Fields:** Supports multiple field types for your custom apps, including:
    *   Text
    *   Number
    *   Date
    *   Checkbox
    *   Dropdown (with comma-separated custom options)
*   **Local Data Persistence:** All of your custom apps and the records you create inside them are saved securely and locally on your device using a Room SQLite database.
*   **Material Design 3:** Features a sleek, modern, adaptive UI built entirely with Jetpack Compose using standard Android M3 components seamlessly.

## 🛠️ Tech Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Local Database:** Room Database with Kotlin Coroutines and Flow
*   **AI Integration:** Ktor client for integrating with the Gemini API (REST)
*   **Architecture:** Clean Architecture with MVVM (Model-View-ViewModel)

## 📂 Project Structure

*   `data/`
    *   `AppDatabase.kt`: Contains Room entities (`AppDefinition`, `AppField`, `AppRecord`, `AppRecordValue`) and DAO queries.
    *   `AppMakerRepository.kt`: Repository layer managing data operations and mediating between the DB and ViewModel.
    *   `GeminiAgentHelper.kt`: Handles the connection with Google's Gemini Models, interpreting natural language prompts into app schemas.
*   `ui/`
    *   `AppMakerScreens.kt`: The main Compose UI containing navigations, app creation tools, prompt inputs, and the dynamic rendering engine to show user-generated apps.
    *   `AppMakerViewModel.kt`: Manages UI state, AI parsing, and database transactions for robust state management.
    *   `theme/`: Contains all Material 3 theming colors, typography, shapes, and theme definitions.
*   `MainActivity.kt`: Standard entry point bridging the compose app to the standard Android Activity lifecycle.

## 🧠 AI Agent Integration

This app features a custom Gemini Agent helper (`GeminiAgentHelper.kt`) which generates JSON payloads that dynamically define database schemas (tables and columns). 
Because of this generation process, you can create functional, permanent min-apps just by having a brief conversation. 

## 🏗️ Building and Exporting

You can compile this project into an APK right from Android Studio or via standard Gradle build commands.

```bash
# Build a debug APK
gradlew assembleDebug

# Build a release APK
gradlew assembleRelease
```

## 🔒 Permissions & Safety
The app requests no unnecessary permissions to ensure privacy. Any app definitions your AI generates are handled locally on device. Internet access is only temporarily required if you are generating schemas through the Gemini API prompt.
