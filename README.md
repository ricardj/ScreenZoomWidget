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
5.  **Zoom Logic:** Implement the density change via reflection (see Technical Details below).
6.  **State Tracking:** The widget will change its icon to indicate whether the zoom is "on" or "off".

## How the Zoom Change Works (Technical Details)

Changing the system display density from a third-party app process is heavily restricted in modern Android versions. The working solution uses a combination of techniques:

### The Problem

1. **`Settings.Secure.display_density_forced`**: Writing to this setting requires the `WRITE_SECURE_SETTINGS` permission. While the app can successfully write to the setting, the change *does not take effect immediately*. It only acts as persistence for the next reboot.
2. **`wm density` via Shell**: Executing `Runtime.getRuntime().exec("wm density ...")` from an app process fails on many devices (like Samsung) because SELinux policies block third-party app processes from accessing the WindowManager service.
3. **Hidden APIs**: The Android framework's internal method to change density is `IWindowManager.setForcedDisplayDensityForUser`. However, this is a "hidden API" restricted by Android (`max-target-o`).

### The Solution

To bypass these restrictions and apply the density change immediately without rebooting or root access:

1. **`targetSdk` Downshift**: The app's `targetSdkVersion` in `build.gradle.kts` is intentionally set to `26` (Android O). Because the `IWindowManager` reflection call is restricted with `max-target-o`, lowering the target SDK to 26 lifts the hidden API restriction, allowing our app to use it.
2. **IWindowManager Reflection**: We use Java Reflection to get the `IWindowManager` binder interface and call `setForcedDisplayDensityForUser(0, dpi, userId)`. This is the exact same method that the `wm density` command calls internally, but we execute it directly from the app process.
3. **User ID Resolution**: The `userId` parameter cannot be `-2` (`USER_CURRENT`) because that requires the system-level `INTERACT_ACROSS_USERS_FULL` permission. Instead, we calculate the actual user ID from the app's UID (`Process.myUid() / 100000`), which allows the call to succeed for the current user.
4. **WRITE_SECURE_SETTINGS**: The WindowManager still checks if the caller has the `WRITE_SECURE_SETTINGS` permission, which the app possesses thanks to the one-time ADB grant.
