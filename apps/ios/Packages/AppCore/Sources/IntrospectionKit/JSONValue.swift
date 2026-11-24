import Foundation

public enum JSONValue: Codable, Equatable, Sendable {
    case null
    case bool(Bool)
    case number(Double)
    case string(String)
    case array([JSONValue])
    case object([String: JSONValue])

    public static func int(_ value: Int) -> JSONValue {
        .number(Double(value))
    }

    public static func optional(_ value: String?) -> JSONValue {
        value.map(JSONValue.string) ?? .null
    }

    public subscript(key: String) -> JSONValue? {
        if case let .object(fields) = self {
            fields[key]
        } else {
            nil
        }
    }

    public var stringValue: String? {
        if case let .string(value) = self {
            value
        } else {
            nil
        }
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([JSONValue].self) {
            self = .array(value)
        } else {
            self = try .object(container.decode([String: JSONValue].self))
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .null: try container.encodeNil()
        case let .bool(value): try container.encode(value)
        // Whole numbers print as integers so both platforms emit identical state.
        case let .number(value) where value.rounded() == value && abs(value) < 1e15: try container.encode(Int64(value))
        case let .number(value): try container.encode(value)
        case let .string(value): try container.encode(value)
        case let .array(value): try container.encode(value)
        case let .object(value): try container.encode(value)
        }
    }

    public var data: Data {
        (try? JSONValue.encoder.encode(self)) ?? Data("null".utf8)
    }

    public var text: String {
        String(bytes: data, encoding: .utf8) ?? "null"
    }

    private static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys, .withoutEscapingSlashes]
        return encoder
    }()
}
