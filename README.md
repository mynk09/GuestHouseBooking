# Guest House Booking App

Welcome to the Guest House Booking app, a modern Android application built with the latest technologies to provide a seamless room booking experience. This app demonstrates a clean, scalable architecture and the best practices in Android development.

## 📋 Features

*   **📝 Booking Form:** An intuitive form to book a room for specific dates.
*   **📅 Calendar View:** A visual calendar to see all existing bookings at a glance.
*   **💾 Local Data Persistence:** All bookings are saved locally on the device using a robust database.
*   **🎨 Modern UI:** A sleek and responsive user interface built entirely with Jetpack Compose.

## 📸 Screenshots

*(Placeholder for screenshots of the booking form and calendar view)*

| Booking Form | Calendar View |
| :---: | :---: |
| *Your screenshot here* | *Your screenshot here* |

## 🛠️ Tech Stack & Architecture

This project follows the official Android-recommended MVVM (Model-View-ViewModel) architecture.

*   **Language:** [Kotlin](https://kotlinlang.org/) (100%)
*   **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) for a declarative and modern UI.
*   **Architecture:** [MVVM](https://developer.android.com/jetpack/guide) (ViewModel, Repository, DataSource)
*   **Asynchronous Operations:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) for managing background threads.
*   **Database:** [Room](https://developer.android.com/training/data-storage/room) for robust local data storage.
*   **Annotation Processing:** [KSP (Kotlin Symbol Processing)](https://kotlinlang.org/docs/ksp-overview.html) for faster and more efficient code generation for Room.
*   **ViewModels:** `androidx.lifecycle.ViewModel` to store and manage UI-related data.
*   **Build System:** [Gradle](https://gradle.org/) with [Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html).

## 🚀 Getting Started

Follow these instructions to get the project up and running on your local machine.

### Prerequisites

*   [Android Studio](https://developer.android.com/studio) (latest stable version recommended)
*   JDK 17 or higher

### Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone https://your-repository-url/guest-house-booking.git
    ```

2.  **Open in Android Studio:**
    *   Launch Android Studio.
    *   Select `File > Open` or `Open an Existing Project`.
    *   Navigate to the directory where you cloned the repository and select it.

3.  **Sync Gradle:**
    *   Android Studio will automatically start syncing the project with its Gradle files. This might take a few minutes.
    *   If the sync doesn't start automatically, you can trigger it manually by clicking `File > Sync Project with Gradle Files`.

## ⚙️ Building and Running

Once the project is successfully synced, you can run the application.

*   **Select a Target:** Choose an available Android emulator or connect a physical device.
*   **Run the App:** Click the `Run 'app'` button (▶️) in the Android Studio toolbar or use the `Shift + F10` shortcut.

## 🏗️ Project Structure

The project is organized into the following main packages:

*   `com.gzone.guesthousebooking.data`: Contains the Room database components (Database, DAO, Entities), type converters, and a database initializer.
*   `com.gzone.guesthousebooking.ui`: Holds all the Jetpack Compose UI components, including screens and themes.
*   `com.gzone.guesthousebooking.viewmodel`: Contains the `BookingViewModel` which prepares and manages data for the UI.
*   `com.gzone.guesthousebooking`: The root package containing the `MainActivity`.

## ✅ KSP Configuration

This project uses **KSP** instead of the older `kapt` for annotation processing, which results in significantly faster build times. The KSP plugin and the Room compiler dependency are configured in the `app/build.gradle.kts` file.
