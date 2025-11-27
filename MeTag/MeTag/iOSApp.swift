import SwiftUI
import shared
import FirebaseCore
import FirebaseMessaging

@main
struct iOSApp: App {

	// Connect AppDelegate for push notifications
	@UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

	init() {
		// Initialize Firebase
		FirebaseApp.configure()
		print("iOSApp: Firebase initialized")

		// Initialize QR Scanner manager to observe notifications from Kotlin
		QRScannerManager.shared.setup()
	}

	var body: some Scene {
		WindowGroup {
			ContentView()
				.onOpenURL { url in
					handleDeepLink(url)
				}
		}
	}

	/**
	 * Extract QR token from deep link and store it for LoginViewModel.
	 * Supports both custom scheme (metagapp://login?token=xxx) and HTTPS URLs.
	 */
	private func handleDeepLink(_ url: URL) {
		print("iOSApp: Deep link received: \(url)")

		// Check if this is a QR login link
		if url.scheme == "metagapp" && url.host == "login" {
			// Custom scheme: metagapp://login?token=xxx
			if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
			   let queryItems = components.queryItems,
			   let tokenItem = queryItems.first(where: { $0.name == "token" }),
			   let token = tokenItem.value {
				print("iOSApp: QR token extracted from custom scheme")
				DeepLinkHandler().setQrToken(token: token)
			} else {
				print("iOSApp: No token found in deep link")
			}
		} else if url.scheme == "https" && url.path == "/qr-login" {
			// HTTPS URL: https://metag-analyze.test/qr-login?token=xxx
			if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
			   let queryItems = components.queryItems,
			   let tokenItem = queryItems.first(where: { $0.name == "token" }),
			   let token = tokenItem.value {
				print("iOSApp: QR token extracted from HTTPS URL")
				DeepLinkHandler().setQrToken(token: token)
			} else {
				print("iOSApp: No token found in HTTPS deep link")
			}
		} else {
			print("iOSApp: Deep link not recognized: \(url.scheme ?? "nil")://\(url.host ?? "nil")\(url.path)")
		}
	}
}