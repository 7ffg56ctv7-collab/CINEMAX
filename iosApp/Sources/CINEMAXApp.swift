import SwiftUI
import CinemaxKit

@main
struct CINEMAXApp: App {
    var body: some Scene {
        WindowGroup {
            CINEMAXRootView()
                .preferredColorScheme(.dark)
        }
    }
}

struct CINEMAXRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
