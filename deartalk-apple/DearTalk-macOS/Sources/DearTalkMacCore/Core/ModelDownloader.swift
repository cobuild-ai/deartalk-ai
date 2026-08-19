import Foundation
import Combine

/// 앱 내장형 온디바이스 AI 모델 자동 다운로더 (실시간 진행률 및 백그라운드 다운로드 지원)
public final class ModelDownloader: NSObject, ObservableObject, URLSessionDownloadDelegate {
    public static let shared = ModelDownloader()

    @Published public var isDownloading: Bool = false
    @Published public var progress: Double = 0.0
    @Published public var downloadedSizeMB: Double = 0.0
    @Published public var totalSizeMB: Double = 0.0
    @Published public var statusMessage: String = ""
    @Published public var isCompleted: Bool = false
    @Published public var errorMessage: String? = nil

    private var downloadTask: URLSessionDownloadTask?
    private var session: URLSession?
    private var lastUpdateTime: Date = Date()
    private var lastBytesWritten: Int64 = 0

    // Google Gemma 2 2B Instruct Q4_K_M 양자화 모델 (공식 HuggingFace CDN)
    public let defaultModelUrl = URL(string: "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf")!

    private override init() {
        super.init()
        let config = URLSessionConfiguration.default
        self.session = URLSession(configuration: config, delegate: self, delegateQueue: OperationQueue.main)
    }

    public var destinationDirectory: URL {
        let appSupport = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let dir = appSupport.appendingPathComponent("DearTalk/models", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    public var destinationModelFile: URL {
        return destinationDirectory.appendingPathComponent("model.gguf")
    }

    public func startDownload(from url: URL? = nil) {
        guard !isDownloading else { return }

        let targetUrl = url ?? defaultModelUrl
        isDownloading = true
        isCompleted = false
        errorMessage = nil
        progress = 0.0
        statusMessage = UiStrings.downloaderConnecting
        DearTalkLogger.info("📥 On-device model download started: \(targetUrl.absoluteString)", category: "Downloader")

        let request = URLRequest(url: targetUrl)
        downloadTask = session?.downloadTask(with: request)
        downloadTask?.resume()
    }

    public func cancelDownload() {
        downloadTask?.cancel()
        isDownloading = false
        statusMessage = UiStrings.downloaderCancelled
        DearTalkLogger.info("🛑 On-device model download cancelled", category: "Downloader")
    }

    // MARK: - URLSessionDownloadDelegate

    public func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didWriteData bytesWritten: Int64,
        totalBytesWritten: Int64,
        totalBytesExpectedToWrite: Int64
    ) {
        let writtenMB = Double(totalBytesWritten) / (1024 * 1024)
        let totalMB = totalBytesExpectedToWrite > 0 ? Double(totalBytesExpectedToWrite) / (1024 * 1024) : 1630.0

        let calcProgress = totalBytesExpectedToWrite > 0 ? Double(totalBytesWritten) / Double(totalBytesExpectedToWrite) : 0.0

        DispatchQueue.main.async {
            self.downloadedSizeMB = writtenMB
            self.totalSizeMB = totalMB
            self.progress = max(0.0, min(1.0, calcProgress))
            self.statusMessage = String(format: UiStrings.isKo ? "다운로드 중: %.1f MB / %.1f MB (%.1f%%)" : "Downloading: %.1f MB / %.1f MB (%.1f%%)", writtenMB, totalMB, self.progress * 100.0)
        }
    }

    public func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didFinishDownloadingTo location: URL
    ) {
        DearTalkLogger.info("📦 Model download finished. Moving to local directory...", category: "Downloader")

        let targetUrl = destinationModelFile
        do {
            if FileManager.default.fileExists(atPath: targetUrl.path) {
                try FileManager.default.removeItem(at: targetUrl)
            }
            try FileManager.default.moveItem(at: location, to: targetUrl)

            DearTalkLogger.info("✅ On-device model installed: \(targetUrl.path)", category: "Downloader")

            DispatchQueue.main.async {
                self.isDownloading = false
                self.isCompleted = true
                self.progress = 1.0
                self.statusMessage = UiStrings.downloaderCompleted

                // Trigger engine to detect and load newly installed model
                DearTalkIntentEngine.shared.detectAndInitOnDeviceModel()
            }
        } catch {
            DearTalkLogger.error("❌ Failed to move downloaded model: \(error.localizedDescription)", category: "Downloader", error: error)
            DispatchQueue.main.async {
                self.isDownloading = false
                self.errorMessage = "Failed to move file: \(error.localizedDescription)"
            }
        }
    }

    public func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if let error = error {
            DearTalkLogger.error("❌ 모델 다운로드 실패: \(error.localizedDescription)", category: "Downloader", error: error)
            DispatchQueue.main.async {
                self.isDownloading = false
                self.errorMessage = "다운로드 실패: \(error.localizedDescription)"
            }
        }
    }
}
