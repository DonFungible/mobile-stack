import AppCore

#if DEBUG
    import IntrospectionKit

    /// Lets the CLI drive this running build over a local port. Debug only: it is not compiled into anything we ship.
    enum DebugIntrospection {
        static let port: UInt16 = 4723
        private static var server: IntrospectionSocketServer?

        static func start(core: MobileStackCore) {
            guard server == nil else { return }
            let server = IntrospectionSocketServer(session: IntrospectionSession(root: core), port: port)
            server.start()
            self.server = server
        }
    }
#else
    enum DebugIntrospection {
        static func start(core _: MobileStackCore) {}
    }
#endif
