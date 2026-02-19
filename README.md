# Goop Creatures

A Pokemon GO-style AR creature collection game for Android. Players use their phone camera to scan real-world colors and catch elemental creatures called "Goops", which can be evolved and fused into powerful hybrids.

## Features

- **AR Color Scanning** - Point your camera at real-world colors to discover and catch creatures (Blue = Water, Red = Fire, Green = Nature, Yellow = Electric, Dark = Shadow)
- **Creature Collection** - Catch, nickname, and favorite over 19 unique creatures across 9 types
- **Evolution System** - Merge 3 identical creatures to evolve them into stronger forms
- **Fusion System** - Combine 2 different creature types to create rare hybrids (e.g., Water + Fire = Steam)
- **GPS Habitat Zones** - Location-based habitats affect which creature types spawn nearby
- **Achievements & Daily Challenges** - Track progress and earn XP through 10 achievements and rotating daily challenges
- **Player Progression** - Level up by catching, evolving, and completing challenges

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** 24 (Android 7.0)
- **Database:** Room (6 tables, 7 DAOs)
- **Camera:** CameraX with real-time ImageAnalysis
- **Location:** Google Play Services FusedLocationProvider
- **Async:** Kotlin Coroutines & Flow
- **UI:** Material Design 3, ViewBinding, RecyclerView with DiffUtil
- **Architecture:** Repository pattern with DAO layer

## Mobile Features Used

| Feature | Implementation |
|---------|---------------|
| Database | Room with 6 entities, reactive Flow queries |
| Camera | CameraX color detection for AR scanning |
| GPS | Habitat zones, catch location tracking |
| Animations | ObjectAnimator for catch, evolution, and fusion effects |
| Multi-threading | Coroutines with IO/Main dispatchers |

## Building

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle and run on a device with camera and GPS

## CI/CD

This project uses GitHub Actions for continuous integration. Every push and pull request automatically:
- Builds the debug APK
- Runs unit tests
- Uploads build artifacts

## Project Structure

```
app/src/main/java/com/example/gooponthego/
├── GoopApplication.kt          # App initialization
├── models/
│   └── GoopType.kt             # Creature type definitions
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt      # Room database with seeding
│   │   ├── Converters.kt       # Type converters
│   │   ├── entities/           # 6 entity classes
│   │   └── dao/                # 7 DAO interfaces
│   └── repository/
│       └── GameRepository.kt   # Central data repository
└── ui/
    ├── home/                   # Home screen & daily challenges
    ├── ar/                     # AR scanning & overlay
    ├── collection/             # Creature list & detail views
    ├── evolution/              # Evolution & fusion screens
    ├── map/                    # Habitat map
    └── achievements/           # Achievement tracking
```

## Team

CSD3156 Mobile and Cloud Computing - Spring 2026
