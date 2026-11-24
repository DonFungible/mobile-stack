@testable import AppCore
import Foundation
import Testing

@MainActor
struct ViewModelTests {
    private final class Counter: ViewModel<Counter.State, Counter.Action, Counter.Message> {
        struct State: Equatable {
            var count = 0
            var log: [String] = []
        }

        enum Action {
            case increment
            case incrementTwice
            case load
        }

        enum Message: ViewMessage {
            case viewAction(Action)
            case loaded(Result<Int, APIError>)
        }

        var work: @Sendable () async throws -> Int = { 10 }

        init() {
            super.init(state: State())
        }

        override func update(state: inout State, message: Message) {
            switch message {
            case .viewAction(.increment):
                state.count += 1
                state.log.append("increment")
            case .viewAction(.incrementTwice):
                send(action: .increment)
                send(action: .increment)
                state.log.append("twice")
            case .viewAction(.load):
                run(work, then: Message.loaded)
            case let .loaded(.success(value)):
                state.count = value
            case .loaded(.failure):
                state.log.append("failed")
            }
        }
    }

    @Test func anActionChangesStateThroughUpdate() {
        let viewModel = Counter()
        viewModel.send(action: .increment)
        #expect(viewModel.state.count == 1)
    }

    @Test func messagesSentDuringAnUpdateRunAfterItInOrder() {
        let viewModel = Counter()
        viewModel.send(action: .incrementTwice)
        #expect(viewModel.state.count == 2)
        #expect(viewModel.state.log == ["twice", "increment", "increment"])
    }

    @Test func serviceResultsComeBackAsMessages() async throws {
        let viewModel = Counter()
        viewModel.send(action: .load)
        #expect(viewModel.state.count == 0)
        try await waitUntil { viewModel.state.count == 10 }
    }

    @Test func anyFailureReachesTheReducerAsAnAPIError() async throws {
        let viewModel = Counter()
        viewModel.work = { throw URLError(.badURL) }
        viewModel.send(action: .load)
        try await waitUntil { viewModel.state.log == ["failed"] }
    }

    @Test func fixturesMergeInOrderAndReportWhatTheyCannotAnswer() async throws {
        let fixtures = try FixtureSet.load(
            names: ["App.default", "Items.three", "Items.empty", "Session.signedIn"],
            directory: RepoPaths.contracts("fixtures")
        )
        let violations = FixtureViolations()
        let transport = fixtures.transport(recorder: violations)
        let url = try #require(URL(string: "https://fixtures.invalid/v1/items"))

        let items = try await transport.send(URLRequest(url: url))
        let missing = try await transport.send(URLRequest(url: url.appending(path: "nope")))

        #expect(String(bytes: items.body, encoding: .utf8) == "[]")
        #expect(missing.status == 404)
        #expect(violations.all == ["no fixture for GET items/nope"])
        #expect(fixtures.tokens.token == "fixture-token")
    }

    private func waitUntil(_ condition: () -> Bool) async throws {
        for _ in 0 ..< 200 where !condition() {
            try await Task.sleep(for: .milliseconds(5))
        }
        #expect(condition())
    }
}
