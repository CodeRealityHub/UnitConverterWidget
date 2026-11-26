# Unit Converter Widget

A Kotlin + Jetpack Glance Android home-screen widget with India/World unit systems, multiple conversion categories, a keypad, quick presets, INR currency conversion, and appearance controls.

## Open and run
1. Install Android Studio (Ladybug or newer), Android SDK 35, and JDK 17.
2. Open this folder in Android Studio.
3. Allow Gradle sync to download dependencies.
4. Run the `app` configuration on an Android 8.0+ device/emulator.
5. Return to the launcher, long-press the home screen, open **Widgets**, choose **Unit Converter Widget**, and add it.
6. Resize the widget for best results (recommended roughly 4x4 cells).

## Project notes
- Package/application ID: `com.example.unitwidget` (change before publishing).
- Minimum Android: API 26.
- Currency data: uses `https://open.er-api.com/v6/latest/INR`; built-in fallback rates are estimates. Verify the provider's current terms/attribution requirements before release.
- Rates are held in process memory in this starter implementation; a process restart falls back to estimates until the next sync.
- The widget is Glance RemoteViews-based. Launcher sizing and visual appearance vary by device and Android version.

## Important
This project was generated from the supplied specification. It has not been compiled or tested on a physical Android device in this environment. Run Gradle sync/build in Android Studio and resolve any version-specific API/import issues before release. No Gradle wrapper JAR is included; Android Studio can import the project and use its configured Gradle installation, or generate a wrapper from the Gradle tool window.
