// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "DearTalkMac",
    defaultLocalization: "en",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "DearTalkMacCore",
            targets: ["DearTalkMacCore"]
        ),
        .executable(
            name: "DearTalkMac",
            targets: ["DearTalkMac"]
        ),
        .executable(
            name: "DearTalkMacRunner",
            targets: ["DearTalkMacRunner"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "DearTalkMacCore",
            dependencies: [],
            path: "Sources/DearTalkMacCore"
        ),
        .executableTarget(
            name: "DearTalkMac",
            dependencies: ["DearTalkMacCore"],
            path: "Sources/DearTalkMac"
        ),
        .executableTarget(
            name: "DearTalkMacRunner",
            dependencies: ["DearTalkMacCore"],
            path: "Sources/DearTalkMacRunner"
        )
    ]
)
