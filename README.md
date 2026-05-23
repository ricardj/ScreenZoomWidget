# Accessibility Zoom Widget

An Android application that provides a home screen widget to quickly toggle the screen's display size (zoom level) between the system default and a magnified setting.

## Core Functionality

*   A simple home screen widget with a single button.
*   Tapping the widget will cycle between the phone's default display size and a larger, "zoomed-in" display size.
*   The app itself will contain a simple instruction screen explaining how to grant the required permission.

## Technology Stack

*   **Language:** Kotlin
*   **UI Framework:**
    *   **Widget:** Standard Android `AppWidgetProvider` with `RemoteViews`.
    *   **Instruction Screen:** Jetpack Compose.
*   **Architecture:** A `BroadcastReceiver` will handle widget taps to execute the zoom change.

## CRITICAL: Special Permission Required

To change the screen zoom, the app needs the `WRITE_SECURE_SETTINGS` permission. This is a highly protected permission.

*   **You cannot grant this permission from your phone directly.**
*   You will need to connect your phone to a computer and run a single command using the **Android Debug Bridge (adb)**.

The main screen of the app will detect if the permission is granted and will show you the exact `adb` command you need to run. This is a one-time setup step.

## Implementation Steps

1.  **Project Setup:** Create a new, empty Android project configured for Kotlin and Jetpack Compose.
2.  **Instruction Screen:** Build a simple UI that checks for the `WRITE_SECURE_SETTINGS` permission and displays the necessary `adb` command if it's missing.
3.  **Widget UI:** Design a basic widget layout with a button and an icon.
4.  **Widget Logic:** Implement the `AppWidgetProvider` to place the widget on the home screen.
5.  **Zoom Logic:** Write the code that changes the `display_density_forced` system setting. This will be triggered by a `BroadcastReceiver` when the widget button is tapped.
6.  **State Tracking:** The widget will change its icon to indicate whether the zoom is "on" or "off".
