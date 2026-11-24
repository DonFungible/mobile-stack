@testable import AppCore
import Foundation

final class StubTransport: HTTPTransport, @unchecked Sendable {
    private let lock = NSLock()
    private var responses: [Result<HTTPResponse, Error>]
    private var recorded: [URLRequest] = []

    init(_ responses: Result<HTTPResponse, Error>...) {
        self.responses = responses
    }

    var requests: [URLRequest] {
        lock.withLock { recorded }
    }

    func send(_ request: URLRequest) async throws -> HTTPResponse {
        let next = lock.withLock {
            recorded.append(request)
            return responses.isEmpty ? .success(HTTPResponse(status: 200, body: Data("{}".utf8))) : responses
                .removeFirst()
        }
        return try next.get()
    }

    static func json(_ body: String, status: Int = 200) -> Result<HTTPResponse, Error> {
        .success(HTTPResponse(status: status, body: Data(body.utf8)))
    }
}

final class RecordingReporter: ErrorReporter, @unchecked Sendable {
    private let lock = NSLock()
    private var recorded: [[String: String]] = []

    var contexts: [[String: String]] {
        lock.withLock { recorded }
    }

    func report(_: Error, context: [String: String]) {
        lock.withLock { recorded.append(context) }
    }
}

enum RepoPaths {
    static func contracts(_ name: String, filePath: String = #filePath) -> URL {
        var url = URL(fileURLWithPath: filePath)
        for _ in 0 ..< 7 {
            url.deleteLastPathComponent()
        }
        return url.appending(path: "packages/contracts").appending(path: name)
    }
}

func makeClient(
    _ transport: StubTransport,
    tokens: TokenStore = InMemoryTokenStore(),
    reporter: ErrorReporter = RecordingReporter(),
    onUnauthorized: @escaping @Sendable () async -> Void = {}
) -> APIClient {
    APIClient(
        baseURL: URL(string: "https://api.example.com")!,
        transport: transport,
        tokens: tokens,
        reporter: reporter,
        onUnauthorized: onUnauthorized
    )
}
