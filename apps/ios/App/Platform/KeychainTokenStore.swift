import AppCore
import Foundation
import Security

/// Reads the Keychain once, then serves the token from memory: the request path never blocks on Keychain I/O.
final nonisolated class KeychainTokenStore: TokenStore, @unchecked Sendable {
    private let service: String
    private let account = "session-token"
    private let lock = NSLock()
    private var cached: String?
    private var loaded = false

    init(service: String) {
        self.service = service
    }

    var token: String? {
        lock.withLock {
            if !loaded {
                cached = read()
                loaded = true
            }
            return cached
        }
    }

    func save(_ token: String) {
        lock.withLock {
            cached = token
            loaded = true
            delete()
            var attributes = baseQuery
            attributes[kSecValueData as String] = Data(token.utf8)
            attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            SecItemAdd(attributes as CFDictionary, nil)
        }
    }

    func clear() {
        lock.withLock {
            cached = nil
            loaded = true
            delete()
        }
    }

    private var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }

    private func read() -> String? {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: AnyObject?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data
        else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private func delete() {
        SecItemDelete(baseQuery as CFDictionary)
    }
}
