import UIKit
import VisionKit
import shared

/**
 * QR Code Scanner wrapper using DataScannerViewController (iOS 16+).
 * Provides a modern, Apple-native scanning experience with automatic QR detection.
 *
 * When a valid MeTag QR code is scanned, the token is extracted and passed
 * to DeepLinkHandler for automatic login.
 */
class QRScannerCoordinator: NSObject, DataScannerViewControllerDelegate {

    private weak var presentingController: UIViewController?
    private var dataScanner: DataScannerViewController?
    private var hasProcessedCode = false

    /// Present the QR scanner from the topmost view controller
    func presentScanner() {
        guard DataScannerViewController.isSupported else {
            print("QRScannerCoordinator: DataScanner not supported on this device")
            showError("QR scanning is not supported on this device")
            return
        }

        guard DataScannerViewController.isAvailable else {
            print("QRScannerCoordinator: DataScanner not available (camera restricted?)")
            showError("Camera is not available. Please check permissions in Settings.")
            return
        }

        DispatchQueue.main.async { [weak self] in
            self?.presentScannerOnMainThread()
        }
    }

    private func presentScannerOnMainThread() {
        guard let topViewController = topViewController() else {
            print("QRScannerCoordinator: Could not find top view controller")
            return
        }

        hasProcessedCode = false

        // Configure for QR code scanning only
        let scanner = DataScannerViewController(
            recognizedDataTypes: [.barcode(symbologies: [.qr])],
            qualityLevel: .balanced,
            recognizesMultipleItems: false,
            isHighFrameRateTrackingEnabled: false,
            isHighlightingEnabled: true
        )

        scanner.delegate = self
        scanner.modalPresentationStyle = .fullScreen

        // Add cancel button overlay
        let cancelButton = UIButton(type: .system)
        cancelButton.setTitle("Cancel", for: .normal)
        cancelButton.setTitleColor(.white, for: .normal)
        cancelButton.titleLabel?.font = UIFont.systemFont(ofSize: 18, weight: .medium)
        cancelButton.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        cancelButton.layer.cornerRadius = 25
        cancelButton.translatesAutoresizingMaskIntoConstraints = false
        cancelButton.addTarget(self, action: #selector(cancelTapped), for: .touchUpInside)

        // Add instruction label
        let instructionLabel = UILabel()
        instructionLabel.text = "Point camera at MeTag QR code"
        instructionLabel.textColor = .white
        instructionLabel.textAlignment = .center
        instructionLabel.font = UIFont.systemFont(ofSize: 16, weight: .medium)
        instructionLabel.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        instructionLabel.layer.cornerRadius = 10
        instructionLabel.clipsToBounds = true
        instructionLabel.translatesAutoresizingMaskIntoConstraints = false

        scanner.view.addSubview(instructionLabel)
        scanner.view.addSubview(cancelButton)

        NSLayoutConstraint.activate([
            instructionLabel.topAnchor.constraint(equalTo: scanner.view.safeAreaLayoutGuide.topAnchor, constant: 20),
            instructionLabel.centerXAnchor.constraint(equalTo: scanner.view.centerXAnchor),
            instructionLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 280),
            instructionLabel.heightAnchor.constraint(equalToConstant: 44),

            cancelButton.bottomAnchor.constraint(equalTo: scanner.view.safeAreaLayoutGuide.bottomAnchor, constant: -30),
            cancelButton.centerXAnchor.constraint(equalTo: scanner.view.centerXAnchor),
            cancelButton.widthAnchor.constraint(equalToConstant: 120),
            cancelButton.heightAnchor.constraint(equalToConstant: 50)
        ])

        dataScanner = scanner
        presentingController = topViewController

        topViewController.present(scanner, animated: true) { [weak scanner] in
            do {
                try scanner?.startScanning()
                print("QRScannerCoordinator: Scanning started")
            } catch {
                print("QRScannerCoordinator: Failed to start scanning: \(error)")
            }
        }
    }

    @objc private func cancelTapped() {
        dismissScanner()
    }

    private func dismissScanner() {
        dataScanner?.stopScanning()
        dataScanner?.dismiss(animated: true)
        dataScanner = nil
        presentingController = nil
    }

    // MARK: - DataScannerViewControllerDelegate

    func dataScanner(_ dataScanner: DataScannerViewController, didTapOn item: RecognizedItem) {
        processItem(item)
    }

    func dataScanner(_ dataScanner: DataScannerViewController, didAdd addedItems: [RecognizedItem], allItems: [RecognizedItem]) {
        // Auto-process the first QR code detected
        guard !hasProcessedCode, let item = addedItems.first else { return }
        processItem(item)
    }

    private func processItem(_ item: RecognizedItem) {
        guard !hasProcessedCode else { return }

        switch item {
        case .barcode(let barcode):
            guard let payload = barcode.payloadStringValue else {
                print("QRScannerCoordinator: Barcode has no payload")
                return
            }

            print("QRScannerCoordinator: QR code detected: \(payload)")

            if isValidMeTagQRCode(payload) {
                if let token = extractToken(from: payload) {
                    hasProcessedCode = true

                    // Haptic feedback
                    let generator = UINotificationFeedbackGenerator()
                    generator.notificationOccurred(.success)

                    print("QRScannerCoordinator: Token extracted, passing to DeepLinkHandler")
                    DeepLinkHandler().setQrToken(token: token)
                    dismissScanner()
                } else {
                    showInvalidQRError()
                }
            } else {
                showInvalidQRError()
            }

        default:
            break
        }
    }

    // MARK: - Validation

    private func isValidMeTagQRCode(_ value: String) -> Bool {
        // Check for metagapp:// scheme
        if value.hasPrefix("metagapp://login") {
            return true
        }
        // Check for HTTPS URL with qr-login path
        if value.contains("/qr-login") {
            return true
        }
        return false
    }

    private func extractToken(from urlString: String) -> String? {
        guard let url = URL(string: urlString),
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let queryItems = components.queryItems,
              let tokenItem = queryItems.first(where: { $0.name == "token" }),
              let token = tokenItem.value else {
            return nil
        }
        return token
    }

    // MARK: - Helpers

    private func topViewController() -> UIViewController? {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first(where: { $0.isKeyWindow }),
              var topController = window.rootViewController else {
            return nil
        }

        while let presented = topController.presentedViewController {
            topController = presented
        }

        return topController
    }

    private func showError(_ message: String) {
        DispatchQueue.main.async { [weak self] in
            guard let topVC = self?.topViewController() else { return }

            let alert = UIAlertController(title: "Scanner Error", message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default))
            topVC.present(alert, animated: true)
        }
    }

    private func showInvalidQRError() {
        DispatchQueue.main.async { [weak self] in
            let alert = UIAlertController(
                title: "Invalid QR Code",
                message: "This is not a valid MeTag login QR code. Please scan a QR code from the MeTag web portal.",
                preferredStyle: .alert
            )
            alert.addAction(UIAlertAction(title: "Try Again", style: .default))
            self?.dataScanner?.present(alert, animated: true)
        }
    }
}

// MARK: - Global Scanner Manager

/**
 * Singleton to manage QR scanner presentation.
 * Observes notification from Kotlin to trigger scanner.
 */
@MainActor
class QRScannerManager {
    static let shared = QRScannerManager()

    private let coordinator = QRScannerCoordinator()

    /// Notification name for triggering QR scanner from Kotlin
    static let showScannerNotification = Notification.Name("com.metag.showQRScanner")

    private init() {
        // Observe notification from Kotlin
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleShowScanner),
            name: QRScannerManager.showScannerNotification,
            object: nil
        )
        print("QRScannerManager: Initialized and observing notifications")
    }

    @objc private func handleShowScanner() {
        print("QRScannerManager: Received show scanner notification")
        coordinator.presentScanner()
    }

    /// Call this to ensure the manager is initialized early
    func setup() {
        // Just accessing shared triggers init
        print("QRScannerManager: Setup complete")
    }
}
