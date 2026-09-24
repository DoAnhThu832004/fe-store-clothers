package org.example.project.view.screen.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import org.example.project.view.components.GradientButton
import org.jetbrains.compose.resources.stringResource
import store_clother.shared.generated.resources.Res
import store_clother.shared.generated.resources.log_out

@Composable
fun POSScreen(
    onLogout: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GradientButton(
            onClick = onLogout,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primaryContainer
                )
            ),
            shape = RoundedCornerShape(12.dp),
            content = {
                Text(
                    text = stringResource(Res.string.log_out),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        )
    }
}