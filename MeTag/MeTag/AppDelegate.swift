import UIKit
import FirebaseCore
import FirebaseMessaging
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(_ application: UIApplication,
                    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

        // Set delegates
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        // Request notification permissions
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            print("AppDelegate: Notification permission granted: \(granted)")
            if let error = error {
                print("AppDelegate: Error requesting notification permission: \(error)")
            }
        }

        // Register for remote notifications
        application.registerForRemoteNotifications()

        return true
    }

    // MARK: - Firebase Messaging Delegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("AppDelegate: Firebase registration token: \(fcmToken ?? "nil")")

        // Store token in UserDefaults for Kotlin to access
        if let token = fcmToken {
            UserDefaults.standard.set(token, forKey: "fcm_token")
            UserDefaults.standard.synchronize()
            print("AppDelegate: Token stored in UserDefaults")
        }
    }

    // MARK: - Remote Notification Registration

    func application(_ application: UIApplication,
                    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        print("AppDelegate: APNs device token received")
        // Forward to Firebase Messaging
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication,
                    didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("AppDelegate: Failed to register for remote notifications: \(error)")
    }

    // MARK: - UNUserNotificationCenterDelegate

    // Handle notifications when app is in foreground
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                               willPresent notification: UNNotification,
                               withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        print("AppDelegate: Notification received in foreground")
        // Show notification even when app is in foreground
        completionHandler([.banner, .sound, .badge])
    }

    // Handle notification tap
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                               didReceive response: UNNotificationResponse,
                               withCompletionHandler completionHandler: @escaping () -> Void) {
        print("AppDelegate: Notification tapped")
        completionHandler()
    }
}
