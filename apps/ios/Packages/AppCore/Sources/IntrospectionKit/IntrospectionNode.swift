import Foundation

/// A view model, navigator or app root as seen from outside: a name, its state as JSON, and what a user could do.
@MainActor
public protocol IntrospectionNode {
    var name: String { get }
    /// Every field a user could see.
    var state: [String: JSONValue] { get }
    /// One entry per action, every time. Whether an action does anything is the view model's decision.
    var actions: [IntrospectionAction] { get }
    /// Actions a container lends to whichever descendant is focused, such as a stack's `goBack`.
    var lentActions: [IntrospectionAction] { get }
    /// The node below this one. Following `child` from the root finds the focused node.
    var child: (any IntrospectionNode)? { get }
}

public extension IntrospectionNode {
    var lentActions: [IntrospectionAction] {
        []
    }

    var child: (any IntrospectionNode)? {
        nil
    }
}

@MainActor
public protocol IntrospectionProviding {
    var introspectionNode: any IntrospectionNode { get }
}

/// The payload type of a parameterized action, with the schema the CLI shows for it.
public protocol IntrospectionInput: Decodable {
    static var schema: JSONValue { get }
}

public extension IntrospectionInput {
    /// Builds an object schema whose properties are all required.
    static func objectSchema(_ properties: [String: String]) -> JSONValue {
        .object([
            "type": .string("object"),
            "properties": .object(properties.mapValues { .object(["type": .string($0)]) }),
            "required": .array(properties.keys.sorted().map(JSONValue.string)),
        ])
    }
}

public struct InvalidPayload: Error {
    public let reason: String
}

@MainActor
public struct IntrospectionAction {
    public let title: String
    public let inputSchema: JSONValue?
    private let handler: @MainActor (JSONValue?) throws -> Void

    public init(title: String, _ run: @escaping @MainActor () -> Void) {
        self.title = title
        inputSchema = nil
        handler = { _ in run() }
    }

    /// The payload is decoded into `Input` before `run` is called, so a malformed one never reaches the view model.
    public init<Input: IntrospectionInput>(
        title: String,
        input: Input.Type,
        _ run: @escaping @MainActor (Input) -> Void
    ) {
        self.title = title
        inputSchema = Input.schema
        handler = { payload in
            guard let payload else { throw InvalidPayload(reason: "\(title) needs a JSON payload") }
            do {
                try run(JSONDecoder().decode(Input.self, from: payload.data))
            } catch {
                throw InvalidPayload(reason: "\(title) payload does not match its schema")
            }
        }
    }

    func run(payload: JSONValue?) throws {
        try handler(payload)
    }
}
