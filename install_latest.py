import os
import sys
import subprocess
import urllib.request
import json
import shutil
import grant_permission

REPO = "ricardj/ScreenZoomWidget"
API_URL = f"https://api.github.com/repos/{REPO}/releases/latest"

def main():
    print(f"Fetching latest release info for {REPO}...")
    req = urllib.request.Request(API_URL, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req) as response:
            release_data = json.loads(response.read().decode())
    except Exception as e:
        print(f"Failed to fetch release info: {e}")
        sys.exit(1)
    
    apk_url = None
    apk_name = None
    
    for asset in release_data.get("assets", []):
        if asset["name"].endswith(".apk"):
            apk_url = asset["browser_download_url"]
            apk_name = asset["name"]
            break
            
    if not apk_url:
        print("No APK found in the latest release.")
        sys.exit(1)
        
    print(f"Downloading {apk_name} from {apk_url}...")
    apk_path = os.path.join(os.getcwd(), apk_name)
    try:
        req_apk = urllib.request.Request(apk_url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req_apk) as response, open(apk_path, 'wb') as out_file:
            shutil.copyfileobj(response, out_file)
    except Exception as e:
        print(f"Failed to download APK: {e}")
        sys.exit(1)
                
    print(f"Downloaded to {apk_path}")
    
    print("Searching for ADB...")
    adb_path = grant_permission.find_adb()
    
    if not adb_path:
        print("Error: 'adb' command not found and could not be located in standard SDK paths.")
        print("Please ensure Android SDK Platform-Tools are installed.")
        sys.exit(1)
        
    print(f"Using ADB at: {adb_path}")
    
    print("Installing APK...")
    try:
        subprocess.run([adb_path, "install", "-r", apk_path], check=True)
        print("Installation successful.")
    except subprocess.CalledProcessError as e:
        print(f"Failed to install APK. Error: {e}")
        sys.exit(1)
        
    print("\nExecuting grant_permission.py...")
    try:
        subprocess.run([sys.executable, "grant_permission.py"], check=True)
    except subprocess.CalledProcessError as e:
        print(f"Failed to execute grant_permission.py. Error: {e}")
        sys.exit(1)
        
    print("\nOpening the app...")
    try:
        subprocess.run([adb_path, "shell", "monkey", "-p", "com.gemini.zoomwidget", "-c", "android.intent.category.LAUNCHER", "1"], check=True, capture_output=True)
        print("App opened successfully.")
    except subprocess.CalledProcessError as e:
        print(f"Failed to open the app. Error: {e}")
        
if __name__ == "__main__":
    main()
