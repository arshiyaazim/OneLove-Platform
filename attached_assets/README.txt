
# OneLove Dating App

OneLove is a sophisticated Android dating and social discovery application 
leveraging advanced mobile technologies to create meaningful connections 
through intelligent, interactive social experiences.

A sophisticated Android dating and social discovery app that combines 
cutting-edge mobile technologies with intelligent social interaction design.

## Features
- **Advanced Matching Algorithm**: Matches users based on preferences, location, and interests
- **Real-time Chat**: Instant messaging with emoji reactions and typing indicators
- **Audio/Video Calls**: High-quality WebRTC-based calling
- **AI-Powered Profiles**: Enhanced user experience with AI-generated responses
- **Premium Subscription**: Multiple subscription tiers with Stripe payment integration
- **Multi-tier Verification**: ID, photo, and social media verification
- **Points-based Reward System**: Gamification to encourage user engagement
- **Admin Panel**: In-app administration dashboard

## Technology Stack
- **UI**: Jetpack Compose with Material 3 design
- **Architecture**: MVVM with Clean Architecture principles
- **Database**: Room for local persistence, Firestore for cloud storage
- **Authentication**: Firebase Authentication
- **Networking**: Firebase Cloud Functions, Firestore
- **Dependency Injection**: Hilt
- **Asynchronous Operations**: Kotlin Coroutines and Flow
- **Payments**: Stripe SDK
- **Media Handling**: Coil for image loading, WebRTC for calls
- **Maps/Location**: Google Maps and Play Services Location

## Project Structure
- **app/src/main/java/com/kilagee/onelove/**
  - **data/**: Data layer with models, repositories implementations, and local/remote sources
  - **domain/**: Domain layer with repository interfaces and use cases
  - **ui/**: Presentation layer with screens, components, and ViewModels
  - **di/**: Dependency injection modules
  - **services/**: Background services for notifications, etc.
  - **util/**: Utility classes and extensions

## Requirements
- Android Studio Iguana | 2023.2.1 or newer
- Kotlin 1.9.20 or newer
- Java 17
- Android Gradle Plugin 8.2.0
- **User Authentication**: Email, phone, and social media login options
- **Comprehensive Profiles**: Detailed user profiles with photos, preferences, and interests
- **AI-Powered Matching**: Intelligent recommendation engine that learns from user behavior
- **Real-time Chat**: Instant messaging with reactions and read receipts
- **Video/Audio Calls**: High-quality communications using Agora SDK
- **Premium Subscriptions**: Tiered membership plans (Free, Premium, Gold) with Stripe integration
- **Admin Panel**: Built-in administration capabilities
- **Offline Support**: Local caching for using the app without internet connection
- **Notifications**: Push notifications for matches, messages, and updates
- **Multi-tier Verification**: User identity verification system

## Prerequisites
### Development Environment
- Android Studio Arctic Fox (2021.3.1) or newer
- Java Development Kit (JDK) 17
- Gradle 8.2
- Firebase project with Firestore, Authentication, and Functions enabled
- Android SDK 34
- Kotlin 1.9.20

### External Services
- Firebase account for authentication, database, and cloud functions
- Stripe account for payment processing

## Setup
1. Clone the repository
2. Open the project in Android Studio
3. Set up a Firebase project and download the `google-services.json` file
4. Place the `google-services.json` file in the app/ directory
5. Set up a Stripe account and add your publishable key to the app
6. Build and run the project

## Admin Panel
The app includes an in-app admin panel accessible only to users with admin privileges. This panel allows:
- User management (view, edit, ban)
- Content moderation
- Verification requests approval
- Analytics and reporting
- Configuration management

## Contributing
Please read CONTRIBUTING.md for details on our code of conduct and the process 
for submitting pull requests.
- Agora account for video/audio calls
- Google Maps API key (optional, for location-based features)

## Setting Up the Project

### Clone from Git
```bash
git clone https://github.com/yourusername/onelove-dating-app.git
cd onelove-dating-app
```
### Configuration
1. Create a `local.properties` file in the project root with:
   ```
   sdk.dir=/path/to/your/android/sdk
   ```
2. Create a Firebase project at [https://console.firebase.google.com/](https://console.firebase.google.com/)
   - Add an Android app to your Firebase project
   - Download the `google-services.json` file and place it in the `app/` directory
   - Enable Authentication, Firestore, Storage, and Functions
3. Set up Stripe API keys:
   - Create a Stripe account at [https://stripe.com/](https://stripe.com/)
   - Get your publishable key and secret key
   - Add your publishable key to `app/src/main/res/values/strings.xml`:
     ```xml
     <string name="stripe_publishable_key">pk_test_YOUR_KEY</string>
     ```
4. Configure Agora SDK:
   - Create an Agora account at [https://www.agora.io/](https://www.agora.io/)
   - Create a project and get your App ID
   - Add your App ID to `app/src/main/res/values/strings.xml`:
     ```xml
     <string name="agora_app_id">YOUR_APP_ID</string>
     ```
### Building the Project
1. Open the project in Android Studio
2. Sync Gradle files
3. Build the project (Run > Build Project)
4. Run on an emulator or device (Run > Run 'app')
## Deployment on VPS (Hostinger)
### Prerequisites
- Hostinger VPS with root access
- Domain name pointing to your VPS
- SSH access to your VPS
### Backend Deployment Steps
1. **Set up your VPS**:
   ```bash
   # Update system
   apt update && apt upgrade -y
   
   # Install required packages
   apt install -y curl wget unzip git nginx certbot python3-certbot-nginx
   ```
2. **Install Firebase Tools**:
   ```bash
   # Install Node.js and npm
   curl -sL https://deb.nodesource.com/setup_16.x | bash -
   apt install -y nodejs
   
   # Install Firebase CLI
   npm install -g firebase-tools
   ```
3. **Clone your Firebase Functions**:
   ```bash
   # Clone repository (if you have backend code in Git)
   git clone https://github.com/yourusername/onelove-backend.git
   cd onelove-backend
   
   # Login to Firebase
   firebase login
   
   # Deploy Firebase Functions
   firebase deploy --only functions
   ```
4. **Set up SSL for your domain**:
   ```bash
   # Configure Nginx for your domain
   nano /etc/nginx/sites-available/yourdomain.com
   
   # Add the following configuration:
   server {
       listen 80;
       server_name yourdomain.com www.yourdomain.com;
       
       location / {
           proxy_pass http://localhost:5000;
           proxy_http_version 1.1;
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection 'upgrade';
           proxy_set_header Host $host;
           proxy_cache_bypass $http_upgrade;
       }
   }
   
   # Create symbolic link
   ln -s /etc/nginx/sites-available/yourdomain.com /etc/nginx/sites-enabled/
   
   # Test Nginx configuration
   nginx -t
   
   # Restart Nginx
   systemctl restart nginx
   
   # Set up SSL certificate
   certbot --nginx -d yourdomain.com -d www.yourdomain.com
   ```
5. **Configure Firebase for Custom Domain**:
   - Go to Firebase Console > Hosting
   - Add your custom domain
   - Verify domain ownership
   - Configure DNS settings as instructed
### Continuous Deployment from Git
1. **Set up GitHub Actions for Firebase Functions**:
   Create a file `.github/workflows/firebase-deploy.yml` in your repository:
   ```yaml
   name: Deploy to Firebase
   
   on:
     push:
       branches:
         - main
       paths:
         - 'functions/**'
   
   jobs:
     deploy:
       runs-on: ubuntu-latest
       steps:
       - uses: actions/checkout@v3
       - uses: actions/setup-node@v3
         with:
           node-version: 16
       - name: Install Dependencies
         run: cd functions && npm ci
       - name: Deploy to Firebase
         uses: w9jds/firebase-action@master
         with:
           args: deploy --only functions
         env:
           FIREBASE_TOKEN: ${{ secrets.FIREBASE_TOKEN }}
   ```
2. **Set up Secret in GitHub**:
   - Generate Firebase token: `firebase login:ci`
   - Go to GitHub repository > Settings > Secrets
   - Add a new secret named `FIREBASE_TOKEN` with the token value
3. **Configure VPS to Pull from Git**:
   ```bash
   # On your VPS, set up a directory for your backend
   mkdir -p /var/www/backend
   cd /var/www/backend
   
   # Initialize Git repository
   git init
   git remote add origin https://github.com/yourusername/onelove-backend.git
   
   # Create a deploy script
   cat > /var/www/deploy.sh << 'EOL'
   #!/bin/bash
   cd /var/www/backend
   git pull origin main
   cd functions
   npm install
   EOL
   
   # Make the script executable
   chmod +x /var/www/deploy.sh
   
   # Set up a cron job to pull changes periodically
   crontab -e
   # Add this line to check for updates every hour:
   # 0 * * * * /var/www/deploy.sh > /var/log/deploy.log 2>&1
   ```
## Android Studio Project Creation
If you're starting a new project similar to OneLove:
1. **Open Android Studio** and click "New Project"
2. Select "Empty Activity" template
3. Configure your project:
   - Name: OneLove
   - Package name: com.yourdomain.onelove
   - Save location: Your preferred directory
   - Language: Kotlin
   - Minimum SDK: API 24 (Android 7.0)
   - Select "Use legacy android.support libraries" if needed
4. **Add dependencies** to your `build.gradle` files:
   - Update the project-level `build.gradle` with Firebase, Hilt, and other plugins
   - Update the app-level `build.gradle` with Jetpack Compose, Firebase, Room, etc.
5. **Set up Firebase**:
   - Use the Firebase Assistant in Android Studio (Tools > Firebase)
   - Connect your app to Firebase
   - Add Firebase services (Authentication, Firestore, etc.)
6. **Structure your project** following MVVM architecture:
   - data (models, repositories, sources)
   - domain (use cases, business logic)
   - ui (screens, viewmodels, components)
   - di (dependency injection)
   - utils (helper classes)
## Handling Version Conflicts
To handle version conflicts and build issues in the OneLove app:
### Automated Resolution (Version Conflict Helper)
1. **Create a Gradle task** to identify and fix common version conflicts:
Add this to your app-level `build.gradle.kts`:
```kotlin
tasks.register("fixVersionConflicts") {
    doLast {
        println("Analyzing dependencies for version conflicts...")
        
        // Enforce consistent versions
        configurations.all {
            resolutionStrategy {
                // Force specific versions for common libraries
                force("androidx.core:core-ktx:1.12.0")
                force("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
                force("androidx.compose.ui:ui:2023.10.01")
                force("androidx.compose.material3:material3:2023.10.01")
                
                // Fail if the same module appears with different versions
                failOnVersionConflict()
                
                // Cache for 24 hours
                cacheDynamicVersionsFor(24, "hours")
            }
        }
        
        println("Version conflict resolution applied!")
    }
}
```
2. **Add a version catalog** in `settings.gradle.kts` for consistent dependency management:
```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "1.9.20")
            version("agp", "8.2.0")
            version("compose-bom", "2023.10.01")
            version("room", "2.6.0")
            version("hilt", "2.48.1")
            
            library("core-ktx", "androidx.core", "core-ktx").version("1.12.0")
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef("kotlin")
            // Add more libraries as needed
        }
    }
}
```
3. **Create a custom ...
[truncated]
[truncated]
[truncated]