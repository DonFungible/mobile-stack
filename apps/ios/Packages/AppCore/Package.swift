// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "AppCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "AppCore", targets: ["AppCore"]),
        .library(name: "IntrospectionKit", targets: ["IntrospectionKit"]),
        .executable(name: "mobilestack-cli", targets: ["mobilestack-cli"]),
    ],
    targets: [
        // The contract between view models and the CLI. The app never calls it.
        .target(name: "IntrospectionKit"),
        // Everything that isn't UI. Must build on macOS: no UIKit, no SwiftUI.
        .target(name: "AppCore", dependencies: ["IntrospectionKit"]),
        // A host that constructs the core with fixtures instead of platform services.
        .executableTarget(name: "mobilestack-cli", dependencies: ["AppCore", "IntrospectionKit"]),
        .testTarget(name: "AppCoreTests", dependencies: ["AppCore"]),
        .testTarget(name: "IntrospectionKitTests", dependencies: ["IntrospectionKit"]),
    ],
    swiftLanguageModes: [.v6]
)
