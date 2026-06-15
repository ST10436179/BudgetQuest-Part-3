# BudgetQuest

BudgetQuest is a gamified personal budget tracker for Android built with Kotlin, MVVM, Room, and Navigation Component. It helps users track spending in ZAR, stay within monthly goals, and earn XP, levels, and badges for consistent budgeting.

## Team Members
- `ST10446898`
- `ST10436179`
- `ST10445385`
- `ST1045679`

## Part 3 (Final POE) — New Features

These features extend the Part 2 prototype to meet the final POE requirements:

### Required final features
1. **Category spending graph with goals** — The Graph tab shows a bar chart of spend per category for a user-selectable date range, plus a daily trend line. Monthly minimum and maximum goals appear as dashed reference lines and in a summary card.
2. **Visual budget progress dashboard** — The Home tab shows total spend against min/max goals, colour-coded status, live stats, and a highlighted list of overspent categories.
3. **Gamification** — XP on each new expense, 5 levels, rank titles, and three badges (First Entry, Week Warrior, Budget Hero) with toast notifications when earned.

### Custom features (for marking — at least 2 required)
1. **Security-question password recovery** — Users can reset their password from the Forgot Password screen by answering the security question set during registration.
2. **One-tap demo mode** — The login screen can seed and open a demo account (`demo` / `Demo1234`) with realistic categories, goals, and expenses for quick demonstration on a physical device.

### Additional enhancements
- **Receipt photo viewer** — Tap the camera icon on an expense in the list to open the saved receipt full-screen.
- **Adaptive app icon** — Final branded launcher icon with chart motif.

## Architecture
- **UI:** Single-activity, fragment-based navigation with bottom tabs
- **Pattern:** MVVM with a shared `AppViewModel`
- **Storage:** Room (SQLite) for users, categories, expenses, goals, limits, and badges
- **Charts:** MPAndroidChart for category bars and daily trend lines
- **CI:** GitHub Actions runs unit tests and `assembleDebug` on every push

## Tech Stack
- Kotlin 2.0, Android SDK 26–35
- Room + KSP, Navigation Component, View Binding
- Timber logging, JUnit unit tests

## Setup
1. Install Android Studio (JDK 17+)
2. Open this project folder
3. Ensure `ANDROID_HOME` points to your Android SDK (or use `local.properties`)
4. Sync Gradle
5. Run on API 26+ physical device or emulator

### Build & install on a connected device
```powershell
.\gradlew.bat installDebug
```

## Testing
```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

## Demo Video
- [Watch Part 2 demo on YouTube](https://youtu.be/NvN7KHka2zE?si=5-48Ifirfwk0joWu)
- *Update this link with your Part 3 phone demo video before submission.*

## Screen Guide

### Login / Register / Forgot Password
Authentication with validation, security-question reset, and demo login.

### Dashboard (Home)
Monthly spend vs min/max goals, overspend highlights, rank, and recent expenses.

### Expense List
Filter by date range, paginate entries, tap to edit, tap 📷 to view receipt photos.

### Category Totals
Per-category spend vs limits for a selectable period with progress bars and overspend warnings.

### Spending Graph
Bar chart (category spend) + line chart (daily totals) with min/max goal reference lines.

### Budget Goals / Category Manager
Set monthly min/max and per-category limits; manage category emoji and colours.

### Profile
XP, level, rank, earned badges, and links to goals and category management.

## GitHub Actions
The workflow in `.github/workflows/build.yml` runs:
- `./gradlew test`
- `./gradlew assembleDebug`

## Submission checklist (Part 3)
- [ ] App runs on a physical Android phone (not emulator only)
- [ ] Graph shows category spend with min/max goals
- [ ] Dashboard shows visual progress and overspend highlights
- [ ] Gamification (XP, levels, badges) demonstrated
- [ ] Two custom features documented above are shown in demo video
- [ ] README updated with phone demo video link
- [ ] APK built and submitted
- [ ] Research and design PDFs included
