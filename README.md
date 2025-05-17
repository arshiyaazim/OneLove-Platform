# OneLove Dating App

OneLove is a sophisticated Android dating and social discovery application leveraging advanced mobile technologies to create meaningful connections through intelligent, interactive social experiences.

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
- Gradle 8.2
- Firebase project with Firestore, Authentication, and Functions enabled
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

Please read CONTRIBUTING.md for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Firebase for backend services
- Stripe for payment processing
- WebRTC for audio and video calling
- Google Play Services for location and maps
- Material Design for UI guidelines