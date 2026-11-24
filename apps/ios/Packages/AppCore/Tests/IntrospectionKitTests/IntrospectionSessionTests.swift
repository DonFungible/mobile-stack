import Foundation
@testable import IntrospectionKit
import Testing

@MainActor
struct IntrospectionSessionTests {
    private final class Leaf: IntrospectionNode {
        let name = "Leaf"
        var count = 0
        var state: [String: JSONValue] {
            ["count": .int(count)]
        }

        var actions: [IntrospectionAction] {
            [
                IntrospectionAction(title: "Increment") { self.count += 1 },
                IntrospectionAction(title: "Set", input: SetInput.self) { self.count = $0.value },
            ]
        }
    }

    private struct SetInput: IntrospectionInput {
        let value: Int
        static let schema = objectSchema(["value": "integer"])
    }

    private final class Container: IntrospectionNode, IntrospectionProviding {
        let name = "Container"
        let leaf = Leaf()
        var closed = false
        var state: [String: JSONValue] {
            [:]
        }

        var actions: [IntrospectionAction] {
            [
                IntrospectionAction(title: "close") { self.closed = true },
                IntrospectionAction(title: "hidden") {},
            ]
        }

        var lentActions: [IntrospectionAction] {
            Array(actions.prefix(1))
        }

        var child: (any IntrospectionNode)? {
            closed ? nil : leaf
        }

        var introspectionNode: any IntrospectionNode {
            self
        }
    }

    private func response(_ session: IntrospectionSession, _ request: String) throws -> JSONValue {
        try JSONDecoder().decode(JSONValue.self, from: Data(session.handle(line: request).utf8))
    }

    @Test func lsDescribesTheDeepestNodeWithLentActions() throws {
        let session = IntrospectionSession(root: Container())
        let node = try #require(response(session, #"{"id":1,"command":"ls"}"#)["node"])

        #expect(node["name"] == .string("Leaf"))
        #expect(node["path"] == .array([.string("Container"), .string("Leaf")]))
        #expect(node["state"] == .object(["count": .number(0)]))
        #expect(node["actions"] == .array([
            .object(["title": .string("Increment")]),
            .object(["title": .string("Set"), "input": SetInput.schema]),
            .object(["title": .string("close")]),
        ]))
    }

    @Test func doRunsAnActionAndAnswersWithFreshState() throws {
        let session = IntrospectionSession(root: Container())
        let answer = try response(session, #"{"id":7,"command":"do","action":"Set","payload":{"value":5}}"#)

        #expect(answer["id"] == .number(7))
        #expect(answer["node"]?["state"] == .object(["count": .number(5)]))
    }

    @Test func anActionThatIsNotLentIsRefusedWithWhatIsAvailable() throws {
        let session = IntrospectionSession(root: Container())
        let error = try #require(response(session, #"{"id":1,"command":"do","action":"hidden"}"#)["error"])

        #expect(error["code"] == .string("unknown_action"))
        #expect(error["available"] == .array([.string("Increment"), .string("Set"), .string("close")]))
    }

    @Test(arguments: [#"{"value":"five"}"#, "{}", "null"])
    func aMalformedPayloadNeverReachesTheNode(payload: String) throws {
        let root = Container()
        let session = IntrospectionSession(root: root)
        let error = try response(session, #"{"id":1,"command":"do","action":"Set","payload":\#(payload)}"#)["error"]

        #expect(error?["code"] == .string("invalid_payload"))
        #expect(root.leaf.count == 0)
    }

    @Test func focusFollowsTheTreeAsItChanges() {
        let session = IntrospectionSession(root: Container())
        _ = session.handle(line: "do close")
        #expect(session.handle(line: "ls").hasPrefix("Container\n"))
    }

    @Test func thePlainFormReadsLikeATerminal() {
        let session = IntrospectionSession(root: Container())
        #expect(session.handle(line: "do Increment").isEmpty)
        #expect(session.handle(line: "ls") == """
        Leaf
          state   {"count":1}
          actions Increment, Set {value}, close
        """)
        #expect(session
            .handle(line: "do Nope") == "error: Nope is not available here. Available: Increment, Set, close")
    }

    @Test func violationsRideAlongWithEveryAnswer() throws {
        let session = IntrospectionSession(root: Container(), violations: { ["no fixture for GET items"] })
        #expect(try response(session, #"{"command":"ls"}"#)["violations"] ==
            .array([.string("no fixture for GET items")]))
    }
}
