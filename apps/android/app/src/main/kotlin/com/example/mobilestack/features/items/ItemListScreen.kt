package com.example.mobilestack.features.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobilestack.common.LoadStatusContent
import com.example.mobilestack.core.features.ItemListViewModel
import com.example.mobilestack.data.Item
import com.example.mobilestack.designsystem.AppIconButton
import com.example.mobilestack.designsystem.AppIcons
import com.example.mobilestack.designsystem.MessageState
import com.example.mobilestack.designsystem.ScreenContainer
import com.example.mobilestack.designsystem.generated.Spacing
import com.example.mobilestack.features.items.components.ItemRow

@Composable
fun ItemListScreen(viewModel: ItemListViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenContainer(
        title = "Items",
        actions = {
            AppIconButton(AppIcons.Settings, label = "Settings", modifier = Modifier.testTag("settings-button")) {
                viewModel.send(ItemListViewModel.Action.OpenSettings)
            }
        },
    ) {
        LoadStatusContent(
            status = state.status,
            errorMessage = state.errorMessage,
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.send(ItemListViewModel.Action.Refresh) },
        ) {
            if (state.items.isEmpty()) {
                MessageState(title = "Nothing here yet", message = "Items you add will show up here.")
            } else {
                ItemList(state.items, onSelect = { viewModel.send(ItemListViewModel.Action.SelectItem(it)) })
            }
        }
    }
}

@Composable
private fun ItemList(items: List<Item>, onSelect: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("items-list"),
        contentPadding = PaddingValues(Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.Md),
    ) {
        // Stable keys and content types let Compose reuse rows instead of recomposing the list.
        items(items, key = Item::id, contentType = { "item" }) { item ->
            ItemRow(item) { onSelect(item.id) }
        }
    }
}
