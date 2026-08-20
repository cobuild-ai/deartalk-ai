// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "DearTalkIOS",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v17),
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "DearTalkIOSCore",
            targets: ["DearTalkIOSCore"]
        ),
        .executable(
            name: "DearTalkIOSRunner",
            targets: ["DearTalkIOSRunner"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "DearTalkIOSCore",
            dependencies: [],
            path: "Sources/DearTalkIOSCore"
        ),
        .executableTarget(
            name: "DearTalkIOSRunner",
            dependencies: ["DearTalkIOSCore"],
            path: "Sources/DearTalkIOSRunner"
        )
    ]
)
