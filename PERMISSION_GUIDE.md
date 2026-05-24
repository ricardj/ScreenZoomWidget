# How to Grant WRITE_SECURE_SETTINGS Permission

This guide explains how to use the Android Debug Bridge (adb) to grant the necessary permission for the Screen Zoom Widget to function. This is a one-time setup.

## Prerequisites

*   A computer (Windows, macOS, or Linux).
*   The Android phone you will use the widget on.
*   A USB cable to connect your phone to your computer.

---

### Step 1: Install ADB on Your Computer

`adb` is a command-line tool for communicating with your Android device. It's part of the official Android SDK Platform-Tools.

1.  **Download the Tools:** Go to the official Android developer website and download the "SDK Platform-Tools" for your operating system (Windows, Mac, or Linux).
    *   **Link:** [https://developer.android.com/studio/releases/platform-tools](https://developer.android.com/studio/releases/platform-tools)
2.  **Unzip the File:** Extract the contents of the downloaded ZIP file to an easily accessible folder on your computer (for example, `C:\platform-tools` on Windows).

---

### Step 2: Enable USB Debugging on Your Phone

You need to enable developer options on your phone to allow `adb` to connect to it.

1.  Go to **Settings** > **About phone**.
2.  Scroll down and tap on the **Build number** field 7 times. You will see a message saying "You are now a developer!"
3.  Go back to the main **Settings** screen and find the new **Developer options** menu (it may be under **Settings** > **System** > **Developer options**).
4.  Open **Developer options**, scroll down, and turn on the **USB debugging** toggle.

---

### Step 3: Connect and Authorize

1.  Connect your phone to your computer using your USB cable.
2.  Your phone will display a pop-up with the title "Allow USB debugging?".
3.  Check the box that says **"Always allow from this computer"** and tap **Allow**.

---

### Step 4: Run the Permission Command

1.  **Open a Terminal:**
    *   **Windows:** Open Command Prompt or PowerShell.
    *   **macOS/Linux:** Open the Terminal app.
2.  **Navigate to the Tools Folder:** In the terminal, change the directory to the folder where you unzipped the platform-tools.
    *   *Example for Windows:* `cd C:\platform-tools`
3.  **Run the Command:** Copy and paste the following command exactly as it is and press Enter.

    ```sh
    adb shell pm grant com.gemini.zoomwidget android.permission.WRITE_SECURE_SETTINGS
    ```

    *If you are on macOS or Linux and the command fails, you may need to add `./` before `adb` like this:*
    ```sh
    ./adb shell pm grant com.gemini.zoomwidget android.permission.WRITE_SECURE_SETTINGS
    ```

If the command runs successfully, you will not see any message. It will simply move to the next line. The permission is now granted. You can disconnect your phone.

### Automated Setup Alternative
Instead of running `adb` commands manually, you can use the provided Python scripts if you have Python installed on your computer:
1. Open a terminal in the project's root folder.
2. Run `python scripts/grant_permission.py`. The script will automatically locate your ADB installation and grant the `WRITE_SECURE_SETTINGS` permission.
3. You can also run `python scripts/install_latest.py` to automatically fetch the latest release from GitHub, install it, and grant the permissions all in one go!
