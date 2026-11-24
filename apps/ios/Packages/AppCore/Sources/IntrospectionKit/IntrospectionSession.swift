import Foundation

/// Answers the two commands of the protocol, `ls` and `do`, against a tree of nodes.
///
/// A line that starts with `{` is a JSON request and gets a one-line JSON response. Anything else is the
/// plain form (`ls`, `do Action {"json": true}`) and gets plain text, which is what you type by hand.
@MainActor
public final class IntrospectionSession {
    private let root: any IntrospectionProviding
    private let violations: @MainActor () -> [String]

    public init(root: any IntrospectionProviding, violations: @escaping @MainActor () -> [String] = { [] }) {
        self.root = root
        self.violations = violations
    }

    public func handle(line: String) -> String {
        let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.hasPrefix("{") ? handleJSON(trimmed) : handlePlain(trimmed)
    }

    // MARK: Focus

    private struct Focus {
        let node: any IntrospectionNode
        let path: [String]
        let actions: [IntrospectionAction]
    }

    /// The deepest node, plus its own actions and those its ancestors lend it. Nothing else is reachable.
    private func focus() -> Focus {
        var node = root.introspectionNode
        var path = [node.name]
        var lent: [IntrospectionAction] = []
        while let child = node.child {
            lent = node.lentActions + lent
            node = child
            path.append(node.name)
        }
        return Focus(node: node, path: path, actions: node.actions + lent)
    }

    private enum Failure: Error {
        case unknownAction(String, available: [String])
        case invalidPayload(String)
        case badRequest(String)

        var code: String {
            switch self {
            case .unknownAction: "unknown_action"
            case .invalidPayload: "invalid_payload"
            case .badRequest: "bad_request"
            }
        }

        var message: String {
            switch self {
            case let .unknownAction(name, available):
                "\(name) is not available here. Available: \(available.joined(separator: ", "))"
            case let .invalidPayload(reason), let .badRequest(reason):
                reason
            }
        }
    }

    private func perform(action name: String, payload: JSONValue?) throws(Failure) {
        let focus = focus()
        guard let action = focus.actions.first(where: { $0.title == name }) else {
            throw .unknownAction(name, available: focus.actions.map(\.title))
        }
        do {
            try action.run(payload: payload)
        } catch let error as InvalidPayload {
            throw .invalidPayload(error.reason)
        } catch {
            throw .invalidPayload("\(name) rejected its payload")
        }
    }

    // MARK: JSON form

    private func handleJSON(_ line: String) -> String {
        guard let request = try? JSONDecoder().decode(JSONValue.self, from: Data(line.utf8)) else {
            return failureJSON(id: .null, .badRequest("request is not valid JSON"))
        }
        let id = request["id"] ?? .null
        do throws(Failure) {
            switch request["command"]?.stringValue {
            case "ls":
                break
            case "do":
                guard let action = request["action"]?.stringValue else {
                    throw .badRequest("do needs an action")
                }
                try perform(action: action, payload: request["payload"])
            default:
                throw .badRequest("command must be ls or do")
            }
        } catch {
            return failureJSON(id: id, error)
        }
        return JSONValue.object([
            "id": id,
            "ok": .bool(true),
            "node": nodeJSON(focus()),
            "violations": .array(violations().map(JSONValue.string)),
        ]).text
    }

    private func nodeJSON(_ focus: Focus) -> JSONValue {
        .object([
            "name": .string(focus.node.name),
            "path": .array(focus.path.map(JSONValue.string)),
            "state": .object(focus.node.state),
            "actions": .array(focus.actions.map { action in
                var fields: [String: JSONValue] = ["title": .string(action.title)]
                fields["input"] = action.inputSchema
                return .object(fields)
            }),
        ])
    }

    private func failureJSON(id: JSONValue, _ failure: Failure) -> String {
        var error: [String: JSONValue] = ["code": .string(failure.code), "message": .string(failure.message)]
        if case let .unknownAction(_, available) = failure {
            error["available"] = .array(available.map(JSONValue.string))
        }
        return JSONValue.object(["id": id, "ok": .bool(false), "error": .object(error)]).text
    }

    // MARK: Plain form

    private func handlePlain(_ line: String) -> String {
        let parts = line.split(separator: " ", maxSplits: 2).map(String.init)
        switch parts.first {
        case "ls":
            return describe(focus())
        case "do" where parts.count >= 2:
            var payload: JSONValue?
            if parts.count == 3 {
                guard let decoded = try? JSONDecoder().decode(JSONValue.self, from: Data(parts[2].utf8)) else {
                    return "error: payload is not valid JSON"
                }
                payload = decoded
            }
            do throws(Failure) {
                try perform(action: parts[1], payload: payload)
                return ""
            } catch {
                return "error: \(error.message)"
            }
        default:
            return "error: commands are `ls` and `do Action [json]`"
        }
    }

    private func describe(_ focus: Focus) -> String {
        let actions = focus.actions.map { action in
            guard let properties = action.inputSchema?["properties"], case let .object(fields) = properties else {
                return action.title
            }
            return "\(action.title) {\(fields.keys.sorted().joined(separator: ", "))}"
        }
        return """
        \(focus.node.name)
          state   \(JSONValue.object(focus.node.state).text)
          actions \(actions.joined(separator: ", "))
        """
    }
}
