# Skill: Pull and Install Latest Release

**Description:** Use this skill to download the latest release of the app from GitHub, install it on a connected device, and grant the necessary permissions.

**Steps:**
1. Execute the Python script from the project root: `python install_latest.py` (or `python3 install_latest.py`).
2. This script will automatically:
   - Fetch the latest APK from the GitHub releases page for `ricardj/ScreenZoomWidget`.
   - Download the APK to the local workspace.
   - Install the APK on the connected device using ADB.
   - Run `grant_permission.py` to grant the required `WRITE_SECURE_SETTINGS` permission.
   - Launch the application on the device.
3. Monitor the script's output in the terminal and verify that it reports "Installation successful" and "App opened successfully."
