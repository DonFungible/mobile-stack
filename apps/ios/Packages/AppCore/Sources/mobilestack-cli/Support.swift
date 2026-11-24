import AppCore
import Foundation
import IntrospectionKit

@MainActor
final class Host {
    private let fixturesDirectory: URL
    private var session: IntrospectionSession?

    init(fixturesDirectory: URL) {
        self.fixturesDirectory = fixturesDirectory
    }

    func boot(fixtures names: [String], appVersion: String?) throws {
        // Never resolved: the transport answers from fixtures and the guard refuses everything else.
        guard let fixtureHost = URL(string: "https://fixtures.invalid") else { return }
        let fixtures = try FixtureSet.load(names: names, directory: fixturesDirectory)
        let violations = FixtureViolations()
        NetworkGuard.install(recordingTo: violations)
        let core = MobileStackCore(platform: Platform(
            config: AppConfig(
                environment: .development,
                apiBaseURL: fixtureHost,
                useFixtures: true,
                appVersion: appVersion ?? "1.0.0",
                universalLinkHosts: ["mobilestack.example.com"]
            ),
            urlScheme: "mobilestack",
            storeURL: "https://apps.apple.com/app/id0000000000",
            transport: fixtures.transport(recorder: violations),
            tokens: fixtures.tokens,
            preferences: fixtures.preferences,
            flagProvider: fixtures.flagProvider,
            analytics: SilentAnalytics(),
            reporter: SilentReporter()
        ))
        session = IntrospectionSession(root: core, violations: { violations.all })
    }

    func handle(line: String) -> String {
        if let request = try? JSONDecoder().decode(JSONValue.self, from: Data(line.utf8)),
           request["command"]?.stringValue == "boot"
        {
            return boot(request)
        }
        return session?.handle(line: line) ?? Self.notBooted
    }

    private func boot(_ request: JSONValue) -> String {
        var names: [String] = []
        if case let .array(values)? = request["fixtures"] {
            names = values.compactMap(\.stringValue)
        }
        do {
            try boot(fixtures: names, appVersion: request["appVersion"]?.stringValue)
        } catch {
            return JSONValue.object([
                "id": request["id"] ?? .null,
                "ok": .bool(false),
                "error": .object([
                    "code": .string("bad_fixtures"),
                    "message": .string("cannot load \(names): \(error)"),
                ]),
            ]).text
        }
        return handle(line: JSONValue.object(["id": request["id"] ?? .null, "command": .string("ls")]).text)
    }

    private static let notBooted =
        #"{"ok":false,"error":{"code":"not_booted","message":"send boot, or start with --fixtures"}}"#
}

/// Zero network, enforced: any HTTP request that reaches the URL loading system is refused and recorded,
/// so a test fails even if the app caught the error and recovered.
final class NetworkGuard: URLProtocol {
    private nonisolated(unsafe) static var violations: FixtureViolations?

    static func install(recordingTo violations: FixtureViolations) {
        self.violations = violations
        URLProtocol.registerClass(NetworkGuard.self)
    }

    override static func canInit(with request: URLRequest) -> Bool {
        violations?.record("network request attempted: \(request.url?.absoluteString ?? "unknown")")
        return true
    }

    override static func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        client?.urlProtocol(self, didFailWithError: URLError(.notConnectedToInternet))
    }

    override func stopLoading() {}
}

struct SilentAnalytics: Analytics {
    func track(_: AnalyticsEvent) {}
    func identify(userID _: String) {}
    func reset() {}
}

struct SilentReporter: ErrorReporter {
    func report(_: Error, context _: [String: String]) {}
}
