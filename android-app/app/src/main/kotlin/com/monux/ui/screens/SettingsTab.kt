package com.monux.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.monux.MainService
import com.monux.ui.components.HighlightCard
import com.monux.ui.components.HighlightCardTone
import com.monux.ui.state.ConnectionState
import com.monux.ui.theme.MonuxUi

@Composable
fun SettingsTab(
    state: ConnectionState,
    padding: PaddingValues,
) {
    val spacing = MonuxUi.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.pageHorizontal)
            .padding(top = spacing.pageTop + padding.calculateTopPadding(), bottom = spacing.bottomBarInset + padding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        HighlightCard(
            overline = "外观速览",
            title = if (state.uiPreferences.useDynamicColor) "Dynamic Color 已启用" else "正在使用自定义主色",
            subtitle = if (state.uiPreferences.useDynamicColor) {
                "跟随系统壁纸实时取色，保持整机一致性"
            } else {
                "当前主色可在下方继续微调"
            },
            icon = Icons.Rounded.Settings,
            badgeText = if (state.uiPreferences.useDynamicColor) "Material You" else "自定义",
            tone = HighlightCardTone.Settings,
        )
        SettingsScreen(
            preferences = state.uiPreferences,
            onPreferencesChange = MainService::updateUiPreferences,
            modifier = Modifier.weight(1f),
        )
    }
}
