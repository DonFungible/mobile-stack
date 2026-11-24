@testable import AppCore
import Foundation
import Testing

struct APIClientTests {
    private struct Named: Codable, Equatable {
        let name: String
    }

    @Test func getBuildsAVersionedAuthenticatedRequest() async throws {
        let transport = StubTransport(StubTransport.json(#"{"name":"a"}"#))
        let client = makeClient(transport, tokens: InMemoryTokenStore(token: "secret"))

        let result: Named = try await client.get("items", query: ["page": "2"])

        let request = try #require(transport.requests.first)
        #expect(result == Named(name: "a"))
        #expect(request.url?.absoluteString == "https://api.example.com/v1/items?page=2")
        #expect(request.value(forHTTPHeaderField: "Authorization") == "Bearer secret")
        #expect(request.value(forHTTPHeaderField: "x-request-id")?.isEmpty == false)
        #expect(request.timeoutInterval == 30)
    }

    @Test func signedOutRequestsCarryNoAuthorizationHeader() async throws {
        let transport = StubTransport(StubTransport.json(#"{"name":"a"}"#))
        let _: Named = try await makeClient(transport).get("items")
        #expect(transport.requests.first?.value(forHTTPHeaderField: "Authorization") == nil)
    }

    @Test func postEncodesTheBodyAsJSON() async throws {
        let transport = StubTransport(StubTransport.json(#"{"name":"b"}"#))
        let _: Named = try await makeClient(transport).post("items", body: Named(name: "b"))

        let request = try #require(transport.requests.first)
        let body = try JSONDecoder().decode(Named.self, from: #require(request.httpBody))
        #expect(request.httpMethod == "POST")
        #expect(request.value(forHTTPHeaderField: "Content-Type") == "application/json")
        #expect(body == Named(name: "b"))
    }

    @Test func deleteAcceptsAnEmptyBody() async throws {
        let transport = StubTransport(.success(HTTPResponse(status: 204, body: Data())))
        try await makeClient(transport).delete("items/1")
        #expect(transport.requests.first?.httpMethod == "DELETE")
    }

    @Test func unauthorizedNotifiesTheSessionAndIsNotReported() async {
        let reporter = RecordingReporter()
        let notified = InMemoryTokenStore()
        let client = makeClient(
            StubTransport(StubTransport.json("{}", status: 401)),
            reporter: reporter,
            onUnauthorized: { notified.save("called") }
        )

        await #expect(throws: APIError.unauthorized) {
            let _: Named = try await client.get("items")
        }
        #expect(notified.token == "called")
        #expect(reporter.contexts.isEmpty)
    }

    @Test func serverErrorsAreReportedWithANormalizedRoute() async {
        let reporter = RecordingReporter()
        let client = makeClient(StubTransport(StubTransport.json("{}", status: 503)), reporter: reporter)

        await #expect(throws: APIError.self) {
            let _: Named = try await client.get("items/42")
        }
        #expect(reporter.contexts == [["method": "GET", "route": "items/:id"]])
    }

    @Test func clientErrorsAreNotReported() async {
        let reporter = RecordingReporter()
        let client = makeClient(StubTransport(StubTransport.json("{}", status: 404)), reporter: reporter)

        await #expect(throws: APIError.self) {
            let _: Named = try await client.get("items/42")
        }
        #expect(reporter.contexts.isEmpty)
    }

    @Test func aBrokenContractIsReportedAsADecodingError() async {
        let reporter = RecordingReporter()
        let client = makeClient(StubTransport(StubTransport.json(#"{"title":1}"#)), reporter: reporter)

        do {
            let _: Named = try await client.get("items")
            Issue.record("expected a decoding error")
        } catch {
            guard case .decoding = error as? APIError else {
                Issue.record("unexpected error \(error)")
                return
            }
        }
        #expect(reporter.contexts.count == 1)
    }

    @Test func transportFailuresBecomeNetworkErrors() async {
        let client = makeClient(StubTransport(.failure(URLError(.notConnectedToInternet))))
        await #expect(throws: APIError.network) {
            let _: Named = try await client.get("items")
        }
    }

    @Test func cancellationIsNotANetworkError() async {
        let client = makeClient(StubTransport(.failure(URLError(.cancelled))))
        await #expect(throws: APIError.cancelled) {
            let _: Named = try await client.get("items")
        }
    }

    @Test(arguments: [
        ("items/42", "items/:id"),
        ("/users/3F2504E0-4F89-11D3-9A0C-0305E82C3301/items", "users/:id/items"),
        ("min-version", "min-version"),
    ])
    func routePatternsCollapseIdentifiers(path: String, expected: String) {
        #expect(RoutePattern.normalize(path) == expected)
    }
}
