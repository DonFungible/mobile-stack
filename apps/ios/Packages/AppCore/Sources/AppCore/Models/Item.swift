import Foundation

public struct Item: Codable, Equatable, Identifiable, Sendable {
    public let id: Int
    public let title: String
    public let summary: String
    public let updatedAt: Date

    public init(id: Int, title: String, summary: String, updatedAt: Date) {
        self.id = id
        self.title = title
        self.summary = summary
        self.updatedAt = updatedAt
    }
}
