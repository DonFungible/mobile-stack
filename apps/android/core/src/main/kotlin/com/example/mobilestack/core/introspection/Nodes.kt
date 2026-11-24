package com.example.mobilestack.core.introspection

import com.example.mobilestack.core.app.AppViewModel
import com.example.mobilestack.core.app.MobileStackCore
import com.example.mobilestack.core.app.StackNavigator
import com.example.mobilestack.core.features.AboutViewModel
import com.example.mobilestack.core.features.ItemDetailViewModel
import com.example.mobilestack.core.features.ItemListViewModel
import com.example.mobilestack.core.features.SettingsViewModel
import com.example.mobilestack.core.features.UpdateRequiredViewModel
import com.example.mobilestack.core.features.WelcomeViewModel
import com.example.mobilestack.data.Item
import com.example.mobilestack.introspection.IntrospectionAction
import com.example.mobilestack.introspection.IntrospectionNode
import com.example.mobilestack.introspection.objectSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// One node per object in the tree. Each is a plain JSON view of its view model: state as the user sees
// it, every action every time, and nothing but `send(action)` inside the lambdas. The app never calls
// any of this; delete the file and it builds and runs the same.
//
// Kotlin extensions cannot make a type implement an interface, so each node is a class beside its view model.

val MobileStackCore.introspectionNode: IntrospectionNode get() = AppViewModelIntrospectionNode(app)

class AppViewModelIntrospectionNode(private val viewModel: AppViewModel) : IntrospectionNode {
    override val name = "App"

    override val state get() = buildJsonObject { put("phase", viewModel.state.value.phase.value) }

    override val actions
        get() = listOf(
            IntrospectionAction.withInput("openLink", OpenLinkInput.serializer(), objectSchema("url" to "string")) {
                viewModel.send(AppViewModel.Action.OpenLink(it.url))
            },
        )

    override val lentActions get() = actions

    override val child
        get() = when (viewModel.state.value.phase) {
            AppViewModel.Phase.Checking -> null
            AppViewModel.Phase.UpdateRequired -> UpdateRequiredViewModelIntrospectionNode(viewModel.updateRequired)
            AppViewModel.Phase.SignedOut -> WelcomeViewModelIntrospectionNode(viewModel.welcome)
            AppViewModel.Phase.Main -> StackNavigatorIntrospectionNode(viewModel.stack)
        }
}

class StackNavigatorIntrospectionNode(private val viewModel: StackNavigator) : IntrospectionNode {
    override val name = "Stack"

    override val state
        get() = buildJsonObject {
            put(
                "routes",
                buildJsonArray {
                    viewModel.state.value.entries.forEach { add(JsonPrimitive(it.route.name)) }
                },
            )
        }

    override val actions
        get() = listOf(
            IntrospectionAction("goBack") { viewModel.send(StackNavigator.Action.GoBack) },
            IntrospectionAction.withInput("popTo", PopToInput.serializer(), objectSchema("depth" to "integer")) {
                viewModel.send(StackNavigator.Action.PopTo(it.depth))
            },
        )

    override val lentActions get() = actions.take(1)

    override val child
        get() = when (val screen = viewModel.state.value.top?.screen) {
            is StackNavigator.Screen.ItemList -> ItemListViewModelIntrospectionNode(screen.viewModel)
            is StackNavigator.Screen.ItemDetail -> ItemDetailViewModelIntrospectionNode(screen.viewModel)
            is StackNavigator.Screen.Settings -> SettingsViewModelIntrospectionNode(screen.viewModel)
            is StackNavigator.Screen.About -> AboutViewModelIntrospectionNode(screen.viewModel)
            null -> null
        }
}

class WelcomeViewModelIntrospectionNode(private val viewModel: WelcomeViewModel) : IntrospectionNode {
    override val name = "Welcome"
    override val state = JsonObject(emptyMap())

    override val actions
        get() = listOf(
            IntrospectionAction("ContinueAsGuest") {
                viewModel.send(WelcomeViewModel.Action.ContinueAsGuest)
            },
        )
}

class UpdateRequiredViewModelIntrospectionNode(private val viewModel: UpdateRequiredViewModel) : IntrospectionNode {
    override val name = "UpdateRequired"

    override val state get() = buildJsonObject { put("storeUrl", viewModel.state.value.storeUrl) }

    override val actions
        get() = listOf(IntrospectionAction("OpenStore") { viewModel.send(UpdateRequiredViewModel.Action.OpenStore) })
}

class ItemListViewModelIntrospectionNode(private val viewModel: ItemListViewModel) : IntrospectionNode {
    override val name = "ItemList"

    override val state
        get() = viewModel.state.value.let { state ->
            buildJsonObject {
                put("status", state.status.value)
                put("items", buildJsonArray { state.items.forEach { add(it.json) } })
                put("isRefreshing", state.isRefreshing)
                put("errorMessage", state.errorMessage)
            }
        }

    override val actions
        get() = listOf(
            IntrospectionAction("Refresh") { viewModel.send(ItemListViewModel.Action.Refresh) },
            IntrospectionAction.withInput("SelectItem", SelectItemInput.serializer(), objectSchema("id" to "integer")) {
                viewModel.send(ItemListViewModel.Action.SelectItem(it.id))
            },
            IntrospectionAction("OpenSettings") { viewModel.send(ItemListViewModel.Action.OpenSettings) },
        )
}

class ItemDetailViewModelIntrospectionNode(private val viewModel: ItemDetailViewModel) : IntrospectionNode {
    override val name = "ItemDetail"

    override val state
        get() = viewModel.state.value.let { state ->
            buildJsonObject {
                put("itemId", state.itemId)
                put("status", state.status.value)
                put("item", state.item?.json ?: JsonNull)
                put("canShare", state.canShare)
                put("isRefreshing", state.isRefreshing)
                put("errorMessage", state.errorMessage)
            }
        }

    override val actions
        get() = listOf(
            IntrospectionAction("Refresh") { viewModel.send(ItemDetailViewModel.Action.Refresh) },
            IntrospectionAction("Share") { viewModel.send(ItemDetailViewModel.Action.Share) },
        )
}

class SettingsViewModelIntrospectionNode(private val viewModel: SettingsViewModel) : IntrospectionNode {
    override val name = "Settings"

    override val state get() = buildJsonObject { put("hapticsEnabled", viewModel.state.value.hapticsEnabled) }

    override val actions
        get() = listOf(
            IntrospectionAction.withInput(
                "SetHaptics",
                SetHapticsInput.serializer(),
                objectSchema("enabled" to "boolean"),
            ) {
                viewModel.send(SettingsViewModel.Action.SetHaptics(it.enabled))
            },
            IntrospectionAction("OpenAbout") { viewModel.send(SettingsViewModel.Action.OpenAbout) },
            IntrospectionAction("SignOut") { viewModel.send(SettingsViewModel.Action.SignOut) },
        )
}

class AboutViewModelIntrospectionNode(private val viewModel: AboutViewModel) : IntrospectionNode {
    override val name = "About"
    override val actions = emptyList<IntrospectionAction>()

    override val state
        get() = viewModel.state.value.let { state ->
            buildJsonObject {
                put("version", state.version)
                put("environment", state.environment)
                put("dataSource", state.dataSource)
            }
        }
}

private val Item.json: JsonObject
    get() = buildJsonObject {
        put("id", id)
        put("title", title)
        put("summary", summary)
    }

@Serializable
internal data class OpenLinkInput(val url: String)

@Serializable
internal data class PopToInput(val depth: Int)

@Serializable
internal data class SelectItemInput(val id: Int)

@Serializable
internal data class SetHapticsInput(val enabled: Boolean)
