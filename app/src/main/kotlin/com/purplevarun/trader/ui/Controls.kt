package com.purplevarun.trader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** A − or + in a circle. The circle is drawn smaller than the 48 dp area that takes the tap. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoundButton(
    symbol: String,
    label: String,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .size(UiDimens.MinTouchTarget)
            .semantics { contentDescription = label }
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val face = if (filled) UiColors.Ink else UiColors.Paper
        Box(
            Modifier
                .size(UiDimens.RoundButtonSize)
                .alpha(if (enabled) 1f else UiType.DISABLED_ALPHA)
                .clip(CircleShape)
                .background(face)
                .border(UiDimens.RoundButtonBorder, UiColors.Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = symbol,
                style = UiType.RoundButtonSymbol.copy(color = if (filled) UiColors.Paper else UiColors.Ink),
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/** A full-width filled button for the main action on a screen. Grows taller rather than cutting its label off. */
@Composable
fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(UiDimens.ButtonCorner)
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = UiDimens.MinTouchTarget)
            .clip(shape)
            .background(UiColors.Accent.copy(alpha = if (enabled) 1f else UiType.DISABLED_ALPHA))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = UiDimens.ButtonPaddingHorizontal, vertical = UiDimens.ButtonPaddingVertical),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = UiType.Button)
    }
}

/** A text-only button for secondary actions. */
@Composable
fun TextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = UiDimens.MinTouchTarget)
            .widthIn(min = UiDimens.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = UiDimens.TextButtonPaddingHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = UiType.Link)
    }
}

/** An outlined chip for one amount or one choice, as in the facility sheets. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AmountChip(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(UiDimens.ButtonCorner)
    Box(
        modifier
            .heightIn(min = UiDimens.MinTouchTarget)
            .widthIn(min = UiDimens.MinTouchTarget)
            .clip(shape)
            .alpha(if (enabled) 1f else UiType.DISABLED_ALPHA)
            .background(UiColors.Paper)
            .border(UiDimens.ChipBorder, UiColors.Line, shape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(
                horizontal = UiDimens.ChipPaddingHorizontal,
                vertical = UiDimens.ChipPaddingVertical,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = UiType.ChipLabel)
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .size(height = UiDimens.DividerThickness, width = UiDimens.DividerThickness)
            .background(UiColors.Line),
    )
}
