# 🌊 Flow — Task & Team Workflow Management App

**Flow** is a mobile + backend workflow management system designed to improve how teams assign tasks, track progress, and share updates in real time. Built with **Android (Kotlin)** and powered by **PHP REST APIs** with a **MySQL** database.

---

## ✨ Features

### 🔐 Authentication
- **Dual authentication** — Firebase Auth (email/password) + PHP backend for local user records
- **Sign up** with profile photo upload (Base64 encoded)
- **FCM token** registration on login for push notifications
- Persistent sessions via `SharedPreferences`

### 📁 Project Management
- **Create projects** with name, description, and auto-generated join codes
- **Join projects** using invite codes
- **Delete projects** (owner-only)
- **Project detail view** with cover image (Base64), member list, and active tasks
- **Invite users** to projects with notification support

### ✅ Task Management
- **Create tasks** with title, description, priority level, assignee, and deadline (date picker)
- **Track completion percentage** and status (`pending`, `in-progress`, `completed`)
- **Manager controls** — request updates from assignees, mark tasks as completed
- **Member controls** — submit progress updates with text + photo attachments
- **Update timeline** — chronological history of all task updates with images
- **Role-based UI** — managers and members see different action buttons

### 💬 Real-Time Chat
- **1-on-1 messaging** between team members
- **Image messages** — send photos from gallery (Base64 encoded)
- **Edit & delete messages** for both parties
- **Vanish mode** — auto-delete messages for privacy
- **Auto-refresh** — messages poll at regular intervals
- **Chat previews** — list of all conversations with last message preview
- **Profile pictures** in chat headers (Base64 decoded)

### ⏱️ Attendance & Check-In/Out
- **Project-based check-in/out** — select a project, then start/end a session
- **Live timer** — real-time ticking display of active session time
- **Total worked time** — cumulative hours tracked per project
- **Attendance history** — view past check-in records

### 🔔 Notifications & Invitations
- **Push notifications** via Firebase Cloud Messaging (FCM)
- **In-app notification center** — view pending project invitations
- **Accept/decline invitations** directly from the notification list

### 📶 Offline-First Architecture
- **Room Database** for local caching with 7 entity types:
  - `ProjectEntity`, `PendingProjectEntity`
  - `TaskEntity`, `PendingTaskEntity`
  - `TaskCacheEntity`, `PendingTaskUpdateEntity`
  - `TaskUpdateCacheEntity`
- **Offline queue** — projects, tasks, and task updates are saved locally when offline
- **Auto-sync on reconnect** — `NetworkReceiver` (BroadcastReceiver) detects connectivity changes and syncs all pending data via `NetworkSyncHelper`
- **Graceful degradation** — app remains functional without internet

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| **Mobile** | Android (Kotlin) |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 |
| **UI** | XML Layouts, RecyclerView, Material Components, CircleImageView |
| **Networking** | Volley (HTTP client) |
| **Image Loading** | Glide + manual Base64 encoding/decoding |
| **Auth** | Firebase Authentication |
| **Push Notifications** | Firebase Cloud Messaging (FCM) |
| **Local DB** | Room (SQLite ORM) |
| **Concurrency** | Kotlin Coroutines |
| **Backend** | PHP REST APIs |
| **Database** | MySQL |
| **API Format** | JSON |

---

## 📂 Project Structure

```
Flow/
├── app/
│   ├── build.gradle.kts          # Dependencies & build config
│   ├── google-services.json      # Firebase configuration
│   └── src/main/
│       ├── AndroidManifest.xml   # Activities, services, permissions
│       ├── java/com/ahmedprojects/flow/
│       │   ├── 🔐 Auth & Session
│       │   │   ├── splash_screen.kt
│       │   │   ├── login.kt
│       │   │   ├── sign_up_page.kt
│       │   │   └── IP_String.kt          # Backend URL config
│       │   │
│       │   ├── 📁 Projects
│       │   │   ├── projects.kt
│       │   │   ├── create_project_page.kt
│       │   │   ├── manage_project_page.kt
│       │   │   ├── project_members_page.kt
│       │   │   ├── invite_users_page.kt
│       │   │   ├── Select_project.kt
│       │   │   ├── Project.kt / ProjectAdapter.kt
│       │   │   ├── ProjectMembers.kt / ProjectMembersAdapter.kt
│       │   │   └── ProjectEntity.kt / ProjectDAO.kt
│       │   │
│       │   ├── ✅ Tasks
│       │   │   ├── tasks_page.kt / tasks_you_page.kt
│       │   │   ├── all_tasks_tab.kt
│       │   │   ├── create_task.kt
│       │   │   ├── Task_Details.kt
│       │   │   ├── TaskModel.kt / TaskAdapter.kt
│       │   │   ├── TaskUpdatesAdapter.kt
│       │   │   └── TaskEntity.kt / TaskDao.kt / TaskCacheEntity.kt
│       │   │
│       │   ├── 💬 Chat
│       │   │   ├── all_chats_page.kt / add_chats_page.kt
│       │   │   ├── chat_page.kt
│       │   │   ├── ChatAdapter.kt / MessageAdapter.kt
│       │   │   ├── ChatPreview.kt / Message.kt
│       │   │   └── ChatCacheDB.kt
│       │   │
│       │   ├── ⏱️ Attendance
│       │   │   ├── Check_In_out.kt
│       │   │   └── view_attendance.kt
│       │   │
│       │   ├── 🔔 Notifications
│       │   │   ├── notifications_page.kt
│       │   │   ├── NotificationAdapter.kt
│       │   │   ├── NotificationItem.kt / NotificationModel.kt
│       │   │   └── MyFirebaseMessagingService.kt
│       │   │
│       │   ├── 📶 Offline Sync
│       │   │   ├── AppDatabase.kt
│       │   │   ├── NetworkReceiver.kt
│       │   │   ├── NetworkSyncHelper.kt
│       │   │   ├── PendingProjectEntity.kt / PendingProjectDAO.kt
│       │   │   ├── PendingTaskEntity.kt / PendingTaskDao.kt
│       │   │   └── PendingTaskUpdateEntity.kt / PendingTaskUpdateDao.kt
│       │   │
│       │   └── 👤 User & Misc
│       │       ├── home_page.kt
│       │       ├── people.kt
│       │       ├── announcements_tab.kt
│       │       ├── manage_organisation.kt
│       │       ├── User.kt / UserModel.kt
│       │       ├── UserAdapter.kt / UserSearchAdapter.kt
│       │       └── MyApplication.kt
│       │
│       └── res/
│           ├── layout/           # 43 XML layout files
│           ├── drawable/         # Icons, backgrounds, shapes
│           ├── font/             # Custom fonts
│           ├── values/           # Colors, strings, themes
│           └── mipmap-*/         # App launcher icons
│
├── build.gradle.kts              # Root build file
├── settings.gradle.kts           # Project settings
└── gradle/                       # Gradle wrapper
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Ladybug or later
- **JDK 11+**
- **PHP 7.4+** with a web server (XAMPP, WAMP, or LAMP)
- **MySQL 5.7+**

### Backend Setup
1. Set up a PHP server with MySQL (e.g., XAMPP)
2. Import the database schema into MySQL
3. Place the PHP API files in your web server's root directory (e.g., `htdocs/Flow/`)
4. Note your server's local IP address

### Android Setup
1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/Flow.git
   ```
2. Open the project in Android Studio
3. Update the backend URL in [`IP_String.kt`](app/src/main/java/com/ahmedprojects/flow/IP_String.kt):
   ```kotlin
   val IP: String = "http://YOUR_SERVER_IP/Flow/"
   ```
4. Add your `google-services.json` from Firebase Console
5. Build and run on an Android device/emulator (API 24+)

---

## 🔌 API Endpoints

The Android app communicates with the following PHP endpoints:

| Endpoint | Purpose |
|----------|---------|
| `login.php` | Authenticate user |
| `signup.php` | Register new user |
| `save_fcm_token.php` | Store FCM token for push notifications |
| `get_profile_pic.php` | Fetch user profile photo |
| `get_profile_username.php` | Fetch user display name |
| `create_project.php` | Create a new project |
| `create_task.php` | Create a new task |
| `submit_update.php` | Submit task progress update |
| `get_user_invites.php` | Fetch pending project invitations |

---

## 🏗️ Architecture Highlights

- **Offline-first design** — All critical operations (create project, create task, submit update) can be performed offline and are queued in Room DB. A `BroadcastReceiver` listens for connectivity changes and automatically syncs pending data.
- **Role-based access** — The UI dynamically adapts based on whether the logged-in user is a **manager** or **member** for each project, showing/hiding relevant action buttons.
- **Base64 image pipeline** — Profile photos, task update images, and chat images are all encoded as Base64 strings and transmitted via JSON payloads to the PHP backend.
- **Coroutine-based sync** — Network synchronization uses Kotlin Coroutines with `suspendCancellableCoroutine` for clean async-to-sync bridging with Volley.

---

## 🧠 Key Learnings

- Designing clean API contracts between Android & PHP backend
- Handling Base64 image encoding/decoding safely in JSON
- Managing role-based UI behavior (manager vs. member views)
- Debugging real-world crashes from malformed JSON & large payloads
- Writing safer server-side endpoints with prepared statements
- Structuring scalable task/update database models
- Improving UX with card layouts, status chips, and update timelines

---

## 📄 License

This project is for educational and portfolio purposes.
