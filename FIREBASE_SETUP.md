# Firebase Setup Guide for OneLove Dating App

This guide provides detailed instructions for setting up Firebase for the OneLove Dating App. Follow these steps to configure all required Firebase services.

## 1. Firebase Project Creation

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter "OneLove Dating App" as the project name
4. Enable Google Analytics for the project
5. Select or create a Google Analytics account
6. Create the project

## 2. Android App Registration

1. In the Firebase console, click the Android icon to add an Android app
2. Enter package name: `com.kilagee.onelove`
3. Enter app nickname: "OneLove"
4. Enter signing certificate SHA-1 (optional for development, required for production)
5. Register the app
6. Download the `google-services.json` file
7. Place the file in the `app/` directory of your Android project

## 3. Firebase Authentication Setup

1. In the Firebase console, navigate to "Authentication"
2. Click "Get started"
3. Enable the following sign-in methods:
   - Email/Password
   - Google
   - Phone
4. Configure each provider:
   - Email/Password: Enable email verification
   - Google: Configure OAuth client ID
   - Phone: Add test phone numbers for development

## 4. Firestore Database Setup

1. Navigate to "Firestore Database"
2. Click "Create database"
3. Select "Start in production mode"
4. Choose the closest location to your target audience
5. Create the database
6. Deploy the security rules from the project's `firebase/firestore.rules` file:
   ```bash
   firebase deploy --only firestore:rules
   ```

### Initial Collections Setup

Create the following collections with initial documents:

#### App Settings Document
```
Collection: settings
Document ID: app_settings
Fields:
- appName: "OneLove Dating App"
- version: "1.0.0"
- minSupportedVersion: "1.0.0"
- maintenanceMode: false
- featuresEnabled: { "video_calls": true, "ai_profiles": true, "offers": true }
- supportEmail: "support@onelove.kilagee.com"
- termsUrl: "https://onelove.kilagee.com/terms"
- privacyUrl: "https://onelove.kilagee.com/privacy"
```

#### Initial Admin User
```
Collection: admins
Document ID: [your-admin-uid]
Fields:
- email: "[your-admin-email]"
- role: "SUPER_ADMIN"
- permissions: ["MANAGE_USERS", "APPROVE_VERIFICATION", "REJECT_VERIFICATION", "MANAGE_SUBSCRIPTIONS", "MANAGE_AI_PROFILES", "VIEW_LOGS", "MANAGE_APP_SETTINGS", "VIEW_FLAGGED_CONTENT", "REMOVE_CONTENT", "SEND_NOTIFICATIONS", "VIEW_ANALYTICS", "DELETE_USERS", "EDIT_USERS", "MANAGE_POINTS"]
- name: "Admin User"
- createdAt: [timestamp]
- updatedAt: [timestamp]
```

## 5. Firebase Storage Setup

1. Navigate to "Storage"
2. Click "Get started"
3. Select "Start in production mode"
4. Choose the closest location to your target audience
5. Create the storage bucket
6. Deploy the security rules from the project's `firebase/storage.rules` file:
   ```bash
   firebase deploy --only storage:rules
   ```

7. Set up the following directory structure:
   - `/profile_images/`
   - `/verification/`
   - `/chat_media/`
   - `/payment_proof/`
   - `/ai_profiles/`
   - `/notifications/`

## 6. Firebase Cloud Functions Setup

1. Navigate to "Functions"
2. Click "Upgrade project" if required (Cloud Functions requires Blaze plan)
3. Click "Get started"
4. Install Firebase CLI if not already installed:
   ```bash
   npm install -g firebase-tools
   ```
5. Login to Firebase CLI:
   ```bash
   firebase login
   ```
6. Initialize Cloud Functions in your project:
   ```bash
   firebase init functions
   ```
7. Deploy the functions from the project's `functions/` directory:
   ```bash
   firebase deploy --only functions
   ```

### Set up Stripe Integration

1. Create a [Stripe](https://stripe.com/) account
2. Get your Stripe API keys (publishable and secret)
3. Add the secret key to Firebase Functions environment variables:
   ```bash
   firebase functions:config:set stripe.secret_key="sk_your_secret_key"
   ```
4. Add the publishable key to your Android app's `local.properties` file:
   ```
   STRIPE_PUBLISHABLE_KEY=pk_your_publishable_key
   ```

## 7. Firebase Cloud Messaging Setup

1. Navigate to "Cloud Messaging"
2. Note your Server key for backend integration
3. Configure FCM in the Android app (already set up in the code)
4. Test sending notifications from Firebase console

## 8. Firebase Crashlytics Setup

1. Navigate to "Crashlytics"
2. Click "Set up Crashlytics"
3. Crashlytics is already integrated in the app code
4. Complete the setup by building and running the app

## 9. Firebase Analytics Setup

1. Navigate to "Analytics"
2. Review default events
3. Set up custom events as needed
4. Create audiences for key user segments:
   - Premium Users
   - Active Users
   - New Users
   - Dormant Users

## 10. Firebase Remote Config Setup

1. Navigate to "Remote Config"
2. Create the following parameters:

| Parameter Key | Default Value | Description |
|---------------|---------------|-------------|
| min_version_code | 1 | Minimum app version code that is supported |
| maintenance_mode | false | Enable/disable maintenance mode |
| premium_features | ["video_calls", "ai_chat", "unlimited_likes", "see_who_likes", "profile_boost", "priority_matching"] | Features that require premium |
| ai_response_rate | 5 | Speed of AI profile responses (1-10) |
| offer_expiration_hours | 48 | Hours before an offer expires |
| verification_levels | ["UNVERIFIED", "EMAIL", "PHONE", "ID", "SELFIE"] | Available verification levels |
| max_photos | 9 | Maximum photos per profile |
| max_message_length | 500 | Maximum characters per message |
| max_bio_length | 300 | Maximum characters for bio |

3. Publish your changes

## 11. Firebase App Distribution (Optional)

For beta testing:

1. Navigate to "App Distribution"
2. Follow setup instructions
3. Add testers and groups
4. Distribute test builds before production release

## 12. Firebase Performance Monitoring

1. Navigate to "Performance"
2. Performance monitoring is already integrated in the app code
3. Review performance metrics after app usage

## 13. Firebase Security Verification

1. Use the Firebase Local Emulator Suite to test security rules:
   ```bash
   firebase emulators:start
   ```
2. Verify all security rules with different user roles
3. Run security tests to ensure proper data access controls

## 14. Production Checklist

Before launching to production, verify:

- [ ] Firebase Blaze plan activated (required for Cloud Functions)
- [ ] All security rules properly configured and tested
- [ ] Authentication providers thoroughly tested
- [ ] Cloud Functions deployed and tested
- [ ] Proper Firebase project selected in `google-services.json`
- [ ] Analytics event logging verified
- [ ] Crashlytics properly receiving crash reports
- [ ] FCM notifications working end-to-end
- [ ] Remote Config synchronized with app code expectations
- [ ] All admin users properly created with correct permissions

## Troubleshooting

### Common Issues and Solutions

1. **Missing google-services.json**
   - Error: `File google-services.json is missing`
   - Solution: Download the file from Firebase console and place in app directory

2. **Authentication Failed**
   - Error: User creation or login fails
   - Solution: Verify authentication providers are enabled and configured

3. **Cloud Functions Deployment Fails**
   - Error: `HTTP Error: 403, The caller does not have permission`
   - Solution: Upgrade to Blaze plan and verify proper permissions

4. **Security Rules Deny Access**
   - Error: `PERMISSION_DENIED`
   - Solution: Review security rules and check user authentication state

5. **FCM Notifications Not Received**
   - Error: App not receiving notifications
   - Solution: Verify FCM token registration and proper channel setup

For additional help, refer to [Firebase Documentation](https://firebase.google.com/docs).