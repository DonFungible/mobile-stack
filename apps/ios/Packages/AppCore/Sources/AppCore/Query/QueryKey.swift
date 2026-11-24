import Foundation

/// A cache key that doubles as a prefix: trailing `nil`s are dropped, so `QueryKey("items", nil)`
/// matches every cached item when invalidating.
public struct QueryKey: Hashable, Sendable, CustomStringConvertible {
    public let parts: [String]

    public init(_ parts: String?...) {
        var trimmed = parts
        while let last = trimmed.last, last == nil {
            trimmed.removeLast()
        }
        self.parts = trimmed.map { $0 ?? "" }
    }

    public func hasPrefix(_ prefix: QueryKey) -> Bool {
        parts.starts(with: prefix.parts)
    }

    public var description: String {
        parts.joined(separator: "/")
    }
}
