import Foundation

public enum RoutePattern {
    /// Collapses identifiers so one failing endpoint groups as one issue instead of one per id.
    public static func normalize(_ path: String) -> String {
        path.split(separator: "/", omittingEmptySubsequences: true)
            .map { segment in
                if segment.allSatisfy(\.isNumber) {
                    return ":id"
                }
                if UUID(uuidString: String(segment)) != nil {
                    return ":id"
                }
                return String(segment)
            }
            .joined(separator: "/")
    }
}
