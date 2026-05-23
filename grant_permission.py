import subprocess
import sys
import os
import shutil

def find_adb():
    # 1. Try if it's already in PATH
    adb_in_path = shutil.which("adb")
    if adb_in_path:
        return adb_in_path

    # 2. Check standard Android SDK installation paths using environment variables
    possible_paths = []
    
    if sys.platform == "win32":
        # Windows standard path: %LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
        local_app_data = os.environ.get("LOCALAPPDATA")
        if local_app_data:
            possible_paths.append(os.path.join(local_app_data, "Android", "Sdk", "platform-tools", "adb.exe"))
        
        # Also check %ANDROID_HOME% or %ANDROID_SDK_ROOT% if they are set
        for env_var in ["ANDROID_HOME", "ANDROID_SDK_ROOT"]:
            sdk_home = os.environ.get(env_var)
            if sdk_home:
                possible_paths.append(os.path.join(sdk_home, "platform-tools", "adb.exe"))
    else:
        # macOS/Linux standard paths
        home = os.path.expanduser("~")
        possible_paths.append(os.path.join(home, "Library/Android/sdk/platform-tools/adb")) # macOS
        possible_paths.append(os.path.join(home, "Android/Sdk/platform-tools/adb"))         # Linux
        
        # Also check environment variables
        for env_var in ["ANDROID_HOME", "ANDROID_SDK_ROOT"]:
            sdk_home = os.environ.get(env_var)
            if sdk_home:
                possible_paths.append(os.path.join(sdk_home, "platform-tools", "adb"))

    for path in possible_paths:
        if os.path.exists(path):
            return path

    return None

def run_command(command):
    try:
        result = subprocess.run(command, capture_output=True, text=True, check=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error executing command: {' '.join(command)}")
        print(f"Error output: {e.stderr}")
        return None

def main():
    package_name = "com.gemini.zoomwidget"
    
    # Permissions to grant
    # 1. WRITE_SECURE_SETTINGS (standard pm grant)
    # 2. WRITE_SETTINGS (via appops for the Samsung refresh workaround)
    
    permissions_to_grant = [
        {"name": "android.permission.WRITE_SECURE_SETTINGS", "method": "pm"},
        {"name": "WRITE_SETTINGS", "method": "appops"}
    ]

    print("Searching for ADB...")
    adb_path = find_adb()
    
    if not adb_path:
        print("Error: 'adb' command not found and could not be located in standard SDK paths.")
        print("Please ensure Android SDK Platform-Tools are installed.")
        sys.exit(1)
    
    print(f"Using ADB at: {adb_path}")

    print("\nChecking for connected Android devices...")
    devices_output = run_command([adb_path, "devices"])
    
    if not devices_output:
        return

    lines = devices_output.splitlines()
    devices = [line.split()[0] for line in lines[1:] if line.strip() and "device" in line]

    if not devices:
        print("No devices found. Please connect your Android device and enable USB Debugging.")
        return

    print(f"Found {len(devices)} device(s): {', '.join(devices)}")

    for device_id in devices:
        print(f"\nProcessing device: {device_id}")
        
        for perm in permissions_to_grant:
            print(f"Granting {perm['name']} to {package_name}...")
            
            if perm["method"] == "pm":
                cmd = [adb_path, "-s", device_id, "shell", "pm", "grant", package_name, perm["name"]]
            else:
                # AppOps method
                cmd = [adb_path, "-s", device_id, "shell", "appops", "set", package_name, perm["name"], "allow"]
            
            result = run_command(cmd)
            
            if result == "":
                print(f"Successfully granted {perm['name']}!")
            else:
                print(f"Grant attempted for {perm['name']}.")

    print("\nDone! You can now use the Screen Zoom Widget on your device.")

if __name__ == "__main__":
    main()
