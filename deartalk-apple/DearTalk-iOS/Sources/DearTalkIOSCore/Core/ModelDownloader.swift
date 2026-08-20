import Foundation

public final class ModelDownloader: NSObject, ObservableObject, URLSessionDownloadDelegate {
    public static let shared = ModelDownloader()

    @Published public private(set) var isDownloading: Bool = false
    @Published public private(set) var progress: Double = 0.0
    @Published public private(set) var statusMessage: String = ""

    private var downloadTask: URLSessionDownloadTask?
    private var session: URLSession?
    private var completionHandler: ((Result<URL, Error>) -> Void)?

    public override init() {
        super.init()
        let config = URLSessionConfiguration.default
        self.session = URLSession(configuration: config, delegate: self, delegateQueue: .main)
    }

    public func downloadModel(from url: URL, to destinationURL: URL, completion: @escaping (Result<URL, Error>) -> Void) {
        guard !isDownloading else { return }
        self.isDownloading = true
        self.progress = 0.0
        self.statusMessage = UiStrings.isKo ? "모델 다운로드 준비 중..." : "Preparing model download..."
        self.completionHandler = completion

        let task = session?.downloadTask(with: url)
        self.downloadTask = task
        task?.resume()
    }

    public func cancelDownload() {
        downloadTask?.cancel()
        isDownloading = false
        progress = 0.0
        statusMessage = UiStrings.isKo ? "다운로드 취소됨" : "Download cancelled"
    }

    public func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        isDownloading = false
        progress = 1.0
        statusMessage = UiStrings.isKo ? "다운로드 완료!" : "Download complete!"
        completionHandler?(.success(location))
    }

    public func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didWriteData bytesWritten: Int64, totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64) {
        if totalBytesExpectedToWrite > 0 {
            let p = Double(totalBytesWritten) / Double(totalBytesExpectedToWrite)
            self.progress = p
            let mbWritten = Double(totalBytesWritten) / (1024 * 1024)
            let mbTotal = Double(totalBytesExpectedToWrite) / (1024 * 1024)
            self.statusMessage = String(format: "%.1f MB / %.1f MB (%.0f%%)", mbWritten, mbTotal, p * 100)
        }
    }

    public func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if let error = error {
            isDownloading = false
            statusMessage = UiStrings.isKo ? "다운로드 실패: \(error.localizedDescription)" : "Download failed: \(error.localizedDescription)"
            completionHandler?(.failure(error))
        }
    }
}
