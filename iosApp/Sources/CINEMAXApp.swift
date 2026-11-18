import SwiftUI
import UIKit
import AVFoundation
import CinemaxKit

@main
struct CINEMAXApp: App {
    init() {
        // The default iOS audio category can silence video when the ring switch
        // is muted. RAIF is a media player, so opt into the playback category.
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .moviePlayback)
        } catch {
            NSLog("RAIF: unable to configure movie playback audio session: %@", error.localizedDescription)
        }
    }

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
