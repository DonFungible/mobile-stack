import IntrospectionKit

// One node per object in the tree. Each is a plain JSON view of its view model: state as the user sees
// it, every action every time, and nothing but `send(action:)` inside the closures. The app never calls
// any of this; delete the folder and it builds and runs the same.

extension MobileStackCore: IntrospectionProviding {
    public var introspectionNode: any IntrospectionNode {
        app.introspectionNode
    }
}

extension AppViewModel: IntrospectionProviding {
    public var introspectionNode: any IntrospectionNode {
        Node(viewModel: self)
    }

    private struct Node: IntrospectionNode {
        unowned let viewModel: AppViewModel
        let name = "App"

        var state: [String: JSONValue] {
            ["phase": .string(viewModel.state.phase.rawValue)]
        }

        var actions: [IntrospectionAction] {
            [
                IntrospectionAction(title: "openLink", input: OpenLinkInput.self) {
                    viewModel.send(action: .openLink($0.url))
                },
            ]
        }

        var lentActions: [IntrospectionAction] {
            actions
        }

        var child: (any IntrospectionNode)? {
            switch viewModel.state.phase {
            case .checking: nil
            case .updateRequired: viewModel.updateRequired.introspectionNode
            case .signedOut: viewModel.welcome.introspectionNode
            case .main: viewModel.stack.introspectionNode
            }
        }
    }
}

extension StackNavigator: IntrospectionProviding {
    public var introspectionNode: any IntrospectionNode {
        Node(viewModel: self)
    }

    private struct Node: IntrospectionNode {
        unowned let viewModel: StackNavigator
        let name = "Stack"

        var state: [String: JSONValue] {
            ["routes": .array(viewModel.state.entries.map { .string($0.route.name) })]
        }

        var actions: [IntrospectionAction] {
            [
                IntrospectionAction(title: "goBack") { viewModel.send(action: .goBack) },
                IntrospectionAction(title: "popTo", input: PopToInput.self) {
                    viewModel.send(action: .popTo(depth: $0.depth))
                },
            ]
        }

        var lentActions: [IntrospectionAction] {
            Array(actions.prefix(1))
        }

        var child: (any IntrospectionNode)? {
            switch viewModel.state.top?.screen {
            case let .itemList(screen): screen.introspectionNode
            case let .itemDetail(screen): screen.introspectionNode
            case let .settings(screen): screen.introspectionNode
            case let .about(screen): screen.introspectionNode
            case nil: nil
            }
        }
    }
}

extension WelcomeViewModel: IntrospectionProviding {
    public var introspectionNode: any IntrospectionNode {
        Node(viewModel: self)
    }

    private struct Node: IntrospectionNode {
        unowned let viewModel: WelcomeViewModel
        let name = "Welcome"
        let state: [String: JSONValue] = [:]

        var actions: [IntrospectionAction] {
            [IntrospectionAction(title: "ContinueAsGuest") { viewModel.send(action: .continueAsGuest) }]
        }
    }
}

extension UpdateRequiredViewModel: IntrospectionProviding {
    public var introspectionNode: any IntrospectionNode {
        Node(viewModel: self)
    }

    private struct Node: IntrospectionNode {
        unowned let viewModel: UpdateRequiredViewModel
        let name = "UpdateRequired"

        var state: [String: JSONValue] {
            ["storeUrl": .string(viewModel.state.storeURL)]
        }

        var actions: [IntrospectionAction] {
            [IntrospectionAction(title: "OpenStore") { viewModel.send(action: .openStore) }]
        }
    }
}

extension ItemListViewModel: IntrospectionProviding {
    public var introspectionNode: any IntrospectionNode {
        Node(viewModel: self)
    }

    private struct Node: IntrospectionNode {
        unowned let viewModel: ItemListViewModel
        let name = "ItemList"

        var state: [String: JSONValue] {
            [
                "status": .string(viewModel.state.status.rawValue),
                "items": .array(viewModel.state.items.map(\.json)),
                "isRefreshing": .bool(viewModel.state.isRefreshing),
                "errorMessage": .optional(viewModel.state.errorMessage),
            ]
        }

        var actions: [IntrospectionAction] {
            [
                IntrospectionAction(title: "Refresh") { viewModel.send(action: .refresh) },
                IntrospectionAction(title: "SelectItem", input: SelectItemInput.self) {
                    viewModel.send(action: .selectItem(id: $0.id))
                },
                IntrospectionAction(title: "OpenSettings") { viewModel.send(action: .openSettings) },
            ]
        }
    }
}

extension ItemDetailViewModel: IntrospectionProviding {
    public var introspectionNode: any IntrospectionNode {
        Node(viewModel: self)
    }

    private struct Node: IntrospectionNode {
        unowned let viewModel: ItemDetailViewModel
        let name = "ItemDetail"

        var state: [String: JSONValue] {
            [
                "itemId": .int(viewModel.state.itemID),
                "status": .string(viewModel.state.status.rawValue),
                "item": viewModel.state.item?.json ?? .null,
                "canShare": .bool(viewModel.state.canShare),
                "isRefreshing": .bool(viewModel.state.isRefreshing),
                "errorMessage": .optional(viewModel.state.errorMessage),
            ]
        }

        var actions: [IntrospectionAction] {
            [
                IntrospectionAction(title: "Refresh") { viewModel.send(action: .refresh) },
                IntrospectionAction(title: "Share") { viewModel.send(action: .share) },
            ]
        }
    }
}

extension SettingsViewModel: IntrospectionProviding {
    public var introspectionNode: any IntrospectionNode {
        Node(viewModel: self)
    }

    private struct Node: IntrospectionNode {
        unowned let viewModel: SettingsViewModel
        let name = "Settings"

        var state: [String: JSONValue] {
            ["hapticsEnabled": .bool(viewModel.state.hapticsEnabled)]
        }

        var actions: [IntrospectionAction] {
            [
                IntrospectionAction(title: "SetHaptics", input: SetHapticsInput.self) {
                    viewModel.send(action: .setHaptics(enabled: $0.enabled))
                },
                IntrospectionAction(title: "OpenAbout") { viewModel.send(action: .openAbout) },
                IntrospectionAction(title: "SignOut") { viewModel.send(action: .signOut) },
            ]
        }
    }
}

extension AboutViewModel: IntrospectionProviding {
    public var introspectionNode: any IntrospectionNode {
        Node(viewModel: self)
    }

    private struct Node: IntrospectionNode {
        unowned let viewModel: AboutViewModel
        let name = "About"
        let actions: [IntrospectionAction] = []

        var state: [String: JSONValue] {
            [
                "version": .string(viewModel.state.version),
                "environment": .string(viewModel.state.environment),
                "dataSource": .string(viewModel.state.dataSource),
            ]
        }
    }
}

private extension Item {
    var json: JSONValue {
        .object(["id": .int(id), "title": .string(title), "summary": .string(summary)])
    }
}

struct OpenLinkInput: IntrospectionInput {
    let url: String
    static let schema = objectSchema(["url": "string"])
}

struct PopToInput: IntrospectionInput {
    let depth: Int
    static let schema = objectSchema(["depth": "integer"])
}

struct SelectItemInput: IntrospectionInput {
    let id: Int
    static let schema = objectSchema(["id": "integer"])
}

struct SetHapticsInput: IntrospectionInput {
    let enabled: Bool
    static let schema = objectSchema(["enabled": "boolean"])
}
