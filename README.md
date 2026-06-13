# Women_Safety

A Kotlin-based Android application focused on women safety and emergency assistance. The app brings together SOS alerts, location-based assistance, SMS support, voice-triggered help, nearby police station lookup, chat assistance, and safety-oriented navigation in a single mobile experience.

## Features

- **SOS emergency alert** for quick distress signaling
- **SMS support** to notify saved contacts in emergencies
- **Location access** to help share or use the current position
- **Nearby police station lookup** using location-based search
- **Voice command support** for emergency detection
- **Chatbot / assistant screen** for guidance and user support
- **Blogs section** for safety-related content
- **Login / registration flow** backed by Firebase Auth
- **Material bottom navigation** for smooth app navigation

## Screenshots

> Add your app screenshots in a folder such as `docs/screenshots/` and update the image names below to match your files.

| Home | SOS / Emergency | Nearby Police |
| --- | --- | --- |
| ![Home screen](docs/screenshots/home.png) | ![SOS screen](docs/screenshots/sos.png) | ![Nearby police screen](docs/screenshots/nearby-police.png) |

| Login | Chatbot | Blogs |
| --- | --- | --- |
| ![Login screen](docs/screenshots/login.png) | ![Chatbot screen](docs/screenshots/chatbot.png) | ![Blogs screen](docs/screenshots/blogs.png) |

## Tech Stack

- **Language:** Kotlin
- **UI:** Android XML layouts, Material Components
- **Architecture:** Fragment-based navigation
- **Authentication:** Firebase Authentication
- **Database:** Firebase Firestore
- **Maps / Location:** Google Maps, Play Services Location
- **Networking:** OkHttp
- **Media / Animation:** Lottie

## Project Structure

- `app/src/main/java/com/example/women_safety/` - Activities, fragments, adapters, and models
- `app/src/main/res/layout/` - XML layouts
- `app/src/main/res/navigation/` - Navigation graph
- `app/src/main/res/drawable/` - Icons, backgrounds, and UI assets
- `app/src/main/res/menu/` - Bottom navigation menu

## Setup Instructions

1. Clone the repository:

```bash
git clone https://github.com/VinitaPatil2005/Women_Safety.git
```

2. Open the project in **Android Studio**.
3. Sync Gradle and wait for dependencies to download.
4. Add your Firebase configuration file:
   - Place `google-services.json` in the `app/` directory.
5. Add your Google Maps API key in `res/values/google_maps_key.xml` if required.
6. Build and run the app on an Android device or emulator.

## Permissions Used

The app requests permissions such as:

- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- `SEND_SMS`
- `RECORD_AUDIO`
- `POST_NOTIFICATIONS`
- `INTERNET`

## Notes

- The screenshot placeholders above should be replaced with real images from your app.
- If you want, you can also store screenshots in the repository root and update the paths accordingly.
- Some features may require Firebase and Google Maps setup before they work correctly.

## Contributing

Pull requests and improvements are welcome. If you add new features, please update this README and include matching screenshots.

## License

Add your preferred license here if the project is intended for public reuse.
