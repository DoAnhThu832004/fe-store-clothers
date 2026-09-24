package org.example.project.view.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun GradientButton(
    onClick: () -> Unit,
    gradient: Brush,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        elevation = null,
        contentPadding = PaddingValues(0.dp)
    ) {
        val backgroundModifier = if (enabled) {
            Modifier.background(brush = gradient)
        } else {
            Modifier.background(color = Color.LightGray)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .then(backgroundModifier)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
fun GradientButtonPreview() {
    GradientButton(
        onClick = {},
        gradient = Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primaryContainer
            )
        ),
        shape = RoundedCornerShape(12.dp),
        content = {
            Text(
                text = "Button",
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    )
}