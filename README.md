# 🐾 PetCare - Collaborative Pet Care Management

**PetCare** is a collaborative Android application that helps multiple users coordinate and manage pet care responsibilities through task management, gamification, and real-time synchronization.

## 📱 Features

### 🏠 **Multi-User Pet Management**
- **Shared Pet Profiles**: Multiple users can connect to the same pet using a secure pet password
- **Admin System**: Pet creators become administrators with full management privileges
- **User Roles**: Leaders can transfer admin rights and manage connected users
- **Easy Connection**: Join existing pets or create new ones seamlessly

### ✅ **Advanced Task Management**
- **Task Types**: Create one-time tasks or set up recurring schedules (daily, weekly)
- **Smart Assignment**: Assign tasks to specific users or leave unassigned for volunteers
- **Due Date & Time**: Set precise deadlines with reminder notifications
- **Task Filtering**: Filter by "My Tasks", "All Tasks", "Unassigned", "Overdue", or "Recurring"
- **Status Tracking**: Mark tasks as completed and track progress over time
- **Bulk Operations**: Delete all overdue tasks with one action

### 🏆 **Gamification & Leaderboard**
- **Task Counter**: Track completed tasks for each user
- **Real-time Leaderboard**: See who's contributing most to pet care
- **Motivation System**: Encourage engagement through friendly competition

### 🔔 **Smart Notifications**
- **Task Reminders**: Get notified when tasks are due
- **Daily Summaries**: Customizable notification time for daily task overviews
- **Recurring Alerts**: Automatic notifications for recurring tasks
- **Unassigned Task Alerts**: Reminders for tasks that need volunteers

### 🐕 **Pet Profile Management**
- **Pet Information**: Store name, type, and photos
- **Image Upload**: Camera or gallery integration with cloud storage
- **Profile Customization**: Update pet details and images anytime
- **Admin Controls**: Full profile management for pet administrators

### 👤 **User Authentication & Security**
- **Secure Registration**: Email-based account creation
- **Password Management**: Secure login with visibility toggles
- **Remember Me**: Optional automatic login
- **Data Privacy**: All user data encrypted and securely stored

## 🛠️ Technologies Used

- **Language**: Java
- **Platform**: Android (API 26+)
- **Database**: Firebase Realtime Database
- **Authentication**: Firebase Auth
- **Storage**: Firebase Storage + Local Storage
- **UI Framework**: Material Design Components
- **Image Loading**: Glide
- **Notifications**: Android Notification API
- **Architecture**: Fragment-based with MVVM patterns

## 📋 Prerequisites

- Android Studio Arctic Fox or later
- Android SDK API 26 (Android 8.0) or higher
- Google Services JSON file (Firebase configuration)
- Internet connection for Firebase services

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/PetCare.git
cd PetCare
```

### 2. Firebase Configuration
1. Create a new project in [Firebase Console](https://console.firebase.google.com/)
2. Enable the following services:
   - **Authentication** (Email/Password)
   - **Realtime Database**
   - **Storage**
3. Download `google-services.json` and place it in the `app/` directory
4. Update Firebase rules as needed for your security requirements

### 3. Build and Run
1. Open the project in Android Studio
2. Sync project with Gradle files
3. Run the app on an emulator or physical device

## 📱 Usage Guide

### Getting Started
1. **Sign Up**: Create an account with email and password
2. **Choose Pet Option**:
   - **Create New Pet**: Set up a new pet profile and become the admin
   - **Join Existing Pet**: Connect to a pet using the pet password from an admin

### Managing Tasks
1. **Create Tasks**: Use the + button in the Tasks tab
2. **Set Details**: Add task name, due date, time, and assignee
3. **Recurring Tasks**: Set up daily or weekly recurring schedules
4. **Complete Tasks**: Tap the checkmark when tasks are finished
5. **Filter & Search**: Use filters to find specific tasks quickly

### Notifications
1. **Set Notification Time**: Go to Settings → Change Notification Time
2. **Task Reminders**: Automatic alerts 30 minutes before due time
3. **Daily Summary**: Evening notification with tomorrow's unassigned tasks

### Pet Management (Admin Only)
1. **Edit Pet Profile**: Update name, type, and photo
2. **Manage Users**: View connected users and transfer admin rights
3. **Pet Password**: Share the pet password with family/friends
4. **Disconnect**: Leave the pet or delete it entirely

## 📁 Project Structure

```
PetCare/
├── app/src/main/java/com/example/petcare/
│   ├── models/
│   │   ├── User.java              # User data model
│   │   ├── Pet.java               # Pet data model
│   │   └── Task.java              # Task data model
│   ├── activities/
│   │   ├── MainActivity.java      # App entry point
│   │   ├── MainHomeUser.java      # Main home with navigation
│   │   ├── MainSignIn.java        # Authentication screen
│   │   ├── NewPet.java           # Pet creation workflow
│   │   └── NoInternetActivity.java # Offline handling
│   ├── fragments/
│   │   ├── TasksFragment.java     # Task management UI
│   │   ├── SettingFragment.java   # User settings & pet profile
│   │   ├── LeaderboardFragment.java # User rankings
│   │   ├── GeneratePetFragment.java # New pet creation
│   │   ├── ConnectPetFragment.java  # Join existing pet
│   │   ├── NewTaskFragment.java     # Task creation
│   │   └── SignIn/SignUpFragments.java # Authentication
│   ├── adapters/
│   │   ├── TaskAdapter.java       # RecyclerView for tasks
│   │   └── UserAdapter.java       # ListView for leaderboard
│   ├── utils/
│   │   ├── FirebaseFunctions.java # Database operations
│   │   ├── ImagePickerHelper.java # Camera/Gallery integration
│   │   ├── TaskNotificationScheduler.java # Notification system
│   │   └── RecurringTaskGenerator.java # Recurring task logic
│   └── receivers/
│       ├── TaskReminderReceiver.java # Notification handling
│       └── InternetReceiver.java     # Network monitoring
├── app/src/main/res/
│   ├── layout/                    # XML layout files
│   ├── values/                    # Colors, strings, styles
│   └── drawable/                  # App icons and images
└── google-services.json          # Firebase configuration
```

## 🎯 Key Features Breakdown

### Task Management System
- **One-time Tasks**: Perfect for vet appointments, grooming sessions
- **Recurring Tasks**: Daily feeding, weekly walks, monthly check-ups
- **Smart Filtering**: Find exactly what you need quickly
- **Assignment Flexibility**: Assign to specific users or keep open

### Real-time Collaboration
- **Live Updates**: Changes sync instantly across all connected devices
- **Conflict Resolution**: Smart handling of simultaneous edits
- **Offline Support**: App works offline with sync when reconnected

### Notification System
- **Intelligent Reminders**: Context-aware notifications
- **Customizable Timing**: Set your preferred notification schedule
- **Batch Notifications**: Daily summaries to reduce notification fatigue

## 🔒 Privacy & Security

- All user data is encrypted in transit and at rest
- Firebase security rules prevent unauthorized access
- Local data is stored securely on device
- No personal data is shared with third parties


## 🚀 Future Enhancements

- [ ] Pet health tracking and medical records
- [ ] Integration with veterinary services
- [ ] Social sharing of pet achievements
- [ ] Advanced analytics and insights
- [ ] Multi-pet support for single users
- [ ] Voice command integration
- [ ] Wearable device compatibility


**Made with ❤️ for pet lovers everywhere**

*PetCare - Because every pet deserves the best care, together.* 