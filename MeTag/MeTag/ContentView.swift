import SwiftUI
import shared
import UIKit

// Custom container that properly manages the Compose view
class ComposeContainerViewController: UIViewController {
	private var composeController: UIViewController?
	
	override func viewDidLoad() {
		super.viewDidLoad()
		
		// Create and add the Compose controller
		let controller = MainViewControllerKt.MainViewController()
		composeController = controller
		
		// Add as child view controller
		addChild(controller)
		view.addSubview(controller.view)
		controller.view.translatesAutoresizingMaskIntoConstraints = false
		
		// Set up constraints
		NSLayoutConstraint.activate([
			controller.view.topAnchor.constraint(equalTo: view.topAnchor),
			controller.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
			controller.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
			controller.view.bottomAnchor.constraint(equalTo: view.bottomAnchor)
		])
		
		controller.didMove(toParent: self)
		
		// Configure for better text input
		controller.view.isUserInteractionEnabled = true
		controller.view.isMultipleTouchEnabled = false
		
		// Fix keyboard input session
		self.view.becomeFirstResponder()
	}
	
	override var canBecomeFirstResponder: Bool {
		return true
	}
	
	override func viewDidAppear(_ animated: Bool) {
		super.viewDidAppear(animated)
		// Ensure we can become first responder after view appears
		becomeFirstResponder()
	}
}

struct ContentView: View {
	var body: some View {
		ComposeView()
			.ignoresSafeArea(.keyboard, edges: .all)
			.edgesIgnoringSafeArea(.all)
	}
}

struct ComposeView: UIViewControllerRepresentable {
	func makeUIViewController(context: Context) -> UIViewController {
		return ComposeContainerViewController()
	}
	
	func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}