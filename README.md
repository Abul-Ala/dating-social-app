# Aura — Dating & Social Networking Application

A full-stack, privacy-first **Dating and Social Networking Application** featuring a **Java 21 / Spring Boot 3.3 backend** and a native **Android (Kotlin + Jetpack Compose) mobile client**.

---

## 📱 Architecture Overview

```
dating-social-app/
├── backend/                  # Java 21 & Spring Boot 3.3 REST API + STOMP WebSocket
│   ├── src/main/java/com/datingsocial/
│   │   ├── config/           # Security, WebSocket STOMP, CORS, DataInitializer
│   │   ├── controller/       # Auth, Discovery, Matches, Chat, Profile, Safety
│   │   ├── dto/              # Request / Response records
│   │   ├── model/            # JPA Entities (User, Profile, Like, Match, Chat, Block, Report)
│   │   ├── repository/       # Spring Data JPA Repositories
│   │   ├── security/         # Stateless JWT filter, UserPrincipal, BCrypt
│   │   └── service/          # Transactional Matching, Compatibility, Chat, Moderation
│   ├── src/main/resources/   # application.yml (H2 in-memory dev + PostgreSQL prod)
│   ├── pom.xml
│   └── docker-compose.yml    # PostgreSQL 16 & Redis 7 containers
└── android/                  # Native Android (Kotlin + Jetpack Compose + Material 3)
    ├── app/src/main/java/com/datingsocial/app/
    │   ├── data/             # Retrofit API client, data models & DemoDataProvider
    │   ├── ui/components/    # SwipeableCard, ActionButtons, MatchDialog, ReportDialog
    │   ├── ui/screens/       # Discover, Matches, Chat, Profile, EditProfile
    │   ├── ui/navigation/    # AppNavHost and bottom navigation bar
    │   └── ui/theme/         # Dark Obsidian theme with radiant coral/rose gradients
    └── build.gradle.kts
```

---

## 🚀 1. Backend Quickstart

The backend comes pre-configured with **zero-setup in-memory H2 database** and **automatic seed data** for immediate out-of-the-box operation.

### Running with Maven:
```bash
cd backend
mvn spring-boot:run
```
- **REST API Base URL**: `http://localhost:8080/api/v1`
- **H2 Interactive Database Console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:datingdb`, User: `sa`, Password: empty)
- **WebSocket STOMP Broker**: `ws://localhost:8080/ws`

### Running with PostgreSQL & Docker (Optional):
```bash
cd backend
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

---

## 🔑 Pre-Seeded Test Accounts

| Account | Email | Password | Role / Details |
| :--- | :--- | :--- | :--- |
| **Aarav Mehta** | `user@datingsocial.com` | `password123` | Main Demo User (25, Software Architect, Mumbai) |
| **Ananya Sharma** | `ananya@datingsocial.com` | `password123` | Matched Profile (24, Lead UX Designer, Bandra) |
| **Rhea Kapoor** | `rhea@datingsocial.com` | `password123` | Candidate Profile (26, Product Lead, Juhu) |
| **Zoya Khan** | `zoya@datingsocial.com` | `password123` | Candidate Profile (23, Indie Filmmaker, Colaba) |
| **Pooja Verma** | `pooja@datingsocial.com` | `password123` | Candidate Profile (27, Neuroscience, Powai) |

---

## 📡 Core API Endpoints

### Authentication
- `POST /api/v1/auth/register` — Create account with 18+ age verification
- `POST /api/v1/auth/login` — Authenticate and receive JWT Bearer token
- `GET /api/v1/auth/me` — Fetch currently authenticated user profile

### Discovery & Swiping
- `GET /api/v1/discovery?minAge=20&maxAge=30&gender=FEMALE&maxDistanceKm=50` — Get ranked discovery feed
- `POST /api/v1/discovery/swipe` — Send swipe (`targetUserId`, `isLike`). Automatically creates mutual match and conversation when mutual like occurs.

### Mutual Matches & Chat
- `GET /api/v1/matches` — List all active mutual matches
- `GET /api/v1/chat/conversations` — List active conversations with unread counts
- `GET /api/v1/chat/conversations/{id}/messages` — Fetch message history
- `POST /api/v1/chat/conversations/{id}/messages` — Send message (also published to WebSocket `/topic/conversation/{id}`)

### Safety & Moderation
- `POST /api/v1/safety/block` — Instantly block user and deactivate any mutual matches
- `POST /api/v1/safety/unmatch/{targetUserId}` — Unmatch user
- `POST /api/v1/safety/report` — Submit abuse/scam report with automatic auto-block
- `GET /api/v1/safety/blocked` — List all blocked users

---

## 📲 2. Android App Quickstart

### Build Debug APK:
```bash
cd android
./gradlew assembleDebug
```
The generated APK will be at:
`android/app/build/outputs/apk/debug/app-debug.apk`

### Open in Android Studio:
1. Open Android Studio.
2. Select **Open** and choose `/Users/abul/projects lists/intruder-selfie-app/dating-social-app/android`.
3. Press **Run** (`Shift + F10`) to launch on an Android Emulator or connected physical device.
