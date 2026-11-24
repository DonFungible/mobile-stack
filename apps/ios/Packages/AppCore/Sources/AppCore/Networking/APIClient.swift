import Foundation

public struct APIClient: Sendable {
    public static let apiVersion = "v1"
    public static let timeout: TimeInterval = 30

    private let baseURL: URL
    private let transport: HTTPTransport
    private let tokens: TokenStore
    private let reporter: ErrorReporter
    private let onUnauthorized: @Sendable () async -> Void

    public init(
        baseURL: URL,
        transport: HTTPTransport,
        tokens: TokenStore,
        reporter: ErrorReporter,
        onUnauthorized: @escaping @Sendable () async -> Void = {}
    ) {
        self.baseURL = baseURL
        self.transport = transport
        self.tokens = tokens
        self.reporter = reporter
        self.onUnauthorized = onUnauthorized
    }

    public func get<Response: Decodable>(_ path: String, query: [String: String] = [:]) async throws -> Response {
        try await send("GET", path, query: query, body: nil)
    }

    public func post<Response: Decodable>(_ path: String, body: some Encodable & Sendable) async throws -> Response {
        try await send("POST", path, body: Self.encoder.encode(body))
    }

    public func put<Response: Decodable>(_ path: String, body: some Encodable & Sendable) async throws -> Response {
        try await send("PUT", path, body: Self.encoder.encode(body))
    }

    public func patch<Response: Decodable>(_ path: String, body: some Encodable & Sendable) async throws -> Response {
        try await send("PATCH", path, body: Self.encoder.encode(body))
    }

    public func delete(_ path: String) async throws {
        let _: Empty = try await send("DELETE", path, body: nil)
    }

    private func send<Response: Decodable>(
        _ method: String,
        _ path: String,
        query: [String: String] = [:],
        body: Data?
    ) async throws -> Response {
        let requestID = UUID().uuidString
        let request = makeRequest(method, path, query: query, body: body, requestID: requestID)
        do {
            let response = try await perform(request, requestID: requestID)
            return try decode(response.body, requestID: requestID)
        } catch let error as APIError {
            if error.isReportable {
                reporter.report(error, context: ["method": method, "route": RoutePattern.normalize(path)])
            }
            throw error
        }
    }

    private func makeRequest(
        _ method: String,
        _ path: String,
        query: [String: String],
        body: Data?,
        requestID: String
    ) -> URLRequest {
        var url = baseURL.appending(path: Self.apiVersion).appending(path: path)
        if !query.isEmpty {
            let items = query.sorted { $0.key < $1.key }.map { URLQueryItem(name: $0.key, value: $0.value) }
            url.append(queryItems: items)
        }
        var request = URLRequest(url: url, timeoutInterval: Self.timeout)
        request.httpMethod = method
        request.httpBody = body
        request.setValue(requestID, forHTTPHeaderField: "x-request-id")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if body != nil {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        if let token = tokens.token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        return request
    }

    private func perform(_ request: URLRequest, requestID: String) async throws -> HTTPResponse {
        let response: HTTPResponse
        do {
            response = try await transport.send(request)
        } catch is CancellationError {
            throw APIError.cancelled
        } catch let error as URLError where error.code == .cancelled {
            throw APIError.cancelled
        } catch {
            throw APIError.network
        }
        switch response.status {
        case 200 ..< 300:
            return response
        case 401:
            await onUnauthorized()
            throw APIError.unauthorized
        default:
            throw APIError.http(status: response.status, requestID: requestID)
        }
    }

    private func decode<Response: Decodable>(_ data: Data, requestID: String) throws -> Response {
        if Response.self == Empty.self, let empty = Empty() as? Response {
            return empty
        }
        do {
            return try Self.decoder.decode(Response.self, from: data)
        } catch {
            throw APIError.decoding(requestID: requestID)
        }
    }

    private struct Empty: Decodable {}

    private static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()

    private static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }()
}
