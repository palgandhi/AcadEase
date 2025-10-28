# AcadEase – College Portal Android App

AcadEase is an Android application that streamlines college communication and productivity for students, faculty, and administrators. It features announcement feeds with filtering, schedule and user management (admin), and a polished, modern UI using Material components.

## Table of Contents
- [Features](#features)
- [Screens & UX](#screens--ux)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Firebase Setup](#firebase-setup)
- [Build & Run](#build--run)
- [Key Implementation Details](#key-implementation-details)
- [Roles & Permissions](#roles--permissions)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Features
- Unified announcement feed with category filters (Academic, Sports, Events, All)
- Role-based dashboards for Student, Faculty, and Admin
- Create announcements (Faculty, Admin) and Admin-only deletion
- Polished header with circular user avatar
- Profile page redesign: view and change profile image
- Image picker + Firebase Storage upload for profile images
- Auto-login without flashing the login screen when a session exists
- Modern Material design across layouts

## Screens & UX
- Login screen wrapped in a card with shadow and rounded borders
- Announcement feed with header greeting (dynamic user name) and filter chips
- Floating "New Post" button positioned 10dp from bottom and right edge
- Profile page: centered avatar, change image action, and user details

> Screenshots (placeholders):
- `/docs/screens/login.png`
- `/docs/screens/feed.png`
- `/docs/screens/profile.png`

## Tech Stack
- Language: Java (Android)
- UI: Material Components (Material3 theming)
- Backend: Firebase
  - Authentication (Email/Password)
  - Firestore (profiles, announcements)
  - Storage (profile images)

Minimum requirements (typical):
- Android Studio Giraffe+ (or current stable)
- Gradle plugin compatible with your project
- Min SDK 21+, Target SDK per project config

## Project Structure
```
app/
  src/main/java/com/example/acadease/
    adapters/AnnouncementAdapter.java
    data/
      AdminRepository.java
      AnnouncementRepository.java
      StorageRepository.java
      UserRepository.java
    fragments/
      HomeFragment.java                # Shared feed (Student/Faculty/Admin)
      FacultyAnnouncementFragment.java # Faculty feed wrapper
      UserManagementFragment.java      # Admin user registration & image upload
      ScheduleManagementFragment.java
      student/StudentHomeFragment.java
    util/ImageLoader.java              # Lightweight URL image loader
    AdminDashboardActivity.java
    FacultyDashboardActivity.java
    StudentDashboardActivity.java
    MainActivity.java                  # Login + auto-login
    ProfileActivity.java               # Profile view/edit
  src/main/res/
    layout/
      fragment_home.xml
      item_announcement_card.xml
      activity_profile.xml
      legacy_app_header.xml
      activity_*_dashboard.xml
      activity_main.xml
    values/
      themes.xml                       # Theme + ShapeAppearanceOverlay styles
      colors.xml
      styles.xml                        # intentionally minimal/empty
README.md
```

## Getting Started
1. Clone the repository
2. Open the project in Android Studio
3. Set up Firebase (see below)
4. Sync Gradle and run on an emulator or device

## Firebase Setup
1. Create a Firebase project and add an Android app with your package name
2. Download `google-services.json` and place it in `app/`
3. Enable Authentication (Email/Password)
4. Create Firestore (in Native mode) and set security rules (recommended to secure by role)
5. Enable Firebase Storage for profile image uploads

Recommended collections (example):
- `users/{uid}`: { name, email, role, profileImageUrl, studentId/facultyId, currentSemester }
- `announcements/{docId}`: { title, body, category, targetRole[], postedBy, createdAt }

## Build & Run
- Use Android Studio to build and deploy
- Ensure gradle sync succeeds and Firebase is configured
- First run: sign up/admin-seed then sign in (or pre-create accounts in Firebase)

## Key Implementation Details
- Login auto-login (no flash):
  - `MainActivity` checks `FirebaseAuth.getCurrentUser()` before inflating the login layout
  - If authenticated, it fetches the profile and navigates directly to the proper dashboard
- Header user avatar:
  - `legacy_app_header.xml` uses `ShapeableImageView` with a circular style overlay
  - `UserDashboardImageHelper` loads and caches the current user’s `profileImageUrl`
  - `ImageLoader` provides lightweight URL loading without extra deps
- Profile image change:
  - `ProfileActivity` uses `ActivityResultContracts.GetContent()` to pick an image
  - Uploads via `StorageRepository.uploadProfileImage()` → gets download URL
  - Updates Firestore with `UserRepository.updateProfileImageUrl()` and refreshes UI/cache
- Announcements feed:
  - `HomeFragment` handles filters and list binding with `AnnouncementAdapter`
  - Greeting uses dynamic user name fetched from Firestore
  - Deletion is admin-only (see Roles & Permissions)
- Admin portal:
  - `UserManagementFragment` supports user registration and image upload path: `profiles/{uid}/profile.jpg`

## Roles & Permissions
- Student:
  - View announcements, filters
  - No deletion of announcements
- Faculty:
  - View announcements, filters
  - Create announcements
  - No deletion of announcements
- Admin:
  - Full privileges including delete announcements
  - `AdminDashboardActivity` passes `CAN_DELETE=true` to `HomeFragment`

> Enforce security rules in Firestore/Storage to match these constraints.

## Troubleshooting
- App flashes login then navigates: ensure you’re on the version where `MainActivity` checks auth before inflating UI
- Styles linking errors for circular avatars:
  - `themes.xml` defines `ShapeAppearanceOverlay.AcadEase(.Corner/.Corner.Full)`; ensure no duplicates in `styles.xml`
- Images not loading:
  - Verify `profileImageUrl` is valid and Storage rules allow read
  - For uploads, ensure Storage is enabled and `google-services.json` is correct
- Announcement delete still visible:
  - Verify adapter is constructed with `canDelete=false` for Student/Faculty
  - Admin `HomeFragment` must receive `CAN_DELETE=true`

## Contributing
- Fork the repo and create a feature branch
- Use clear commit messages and small, focused PRs
- Follow existing code style and structure; avoid adding unused dependencies

## License
This project is provided for educational purposes. Add your preferred license here (e.g., MIT, Apache-2.0).
