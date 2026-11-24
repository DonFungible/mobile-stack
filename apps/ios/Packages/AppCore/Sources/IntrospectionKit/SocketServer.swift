import Foundation
import Network

/// The same protocol over a local TCP port, for driving a running debug build. Never part of a release build.
@MainActor
public final class IntrospectionSocketServer {
    private let session: IntrospectionSession
    private let listener: NWListener?

    public init(session: IntrospectionSession, port: UInt16) {
        self.session = session
        let parameters = NWParameters.tcp
        parameters.requiredInterfaceType = .loopback
        parameters.allowLocalEndpointReuse = true
        listener = NWEndpoint.Port(rawValue: port).flatMap { try? NWListener(using: parameters, on: $0) }
    }

    public func start() {
        listener?.newConnectionHandler = { [weak self] connection in
            connection.start(queue: .main)
            Task { @MainActor in self?.receive(on: connection, buffer: Data()) }
        }
        listener?.start(queue: .main)
    }

    private func receive(on connection: NWConnection, buffer: Data) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 65536) { [weak self] data, _, isComplete, error in
            Task { @MainActor in
                guard let self, error == nil else { return connection.cancel() }
                var buffer = buffer + (data ?? Data())
                while let newline = buffer.firstIndex(of: 0x0A) {
                    let line = String(bytes: buffer[buffer.startIndex ..< newline], encoding: .utf8) ?? ""
                    buffer = Data(buffer[buffer.index(after: newline)...])
                    let response = self.session.handle(line: line)
                    connection.send(content: Data((response + "\n").utf8), completion: .contentProcessed { _ in })
                }
                if isComplete {
                    return connection.cancel()
                }
                self.receive(on: connection, buffer: buffer)
            }
        }
    }
}
