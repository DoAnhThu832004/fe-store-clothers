package org.example.project.view.screen.intro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.domain.model.IntroPageData
import org.example.project.view.components.DotsIndicator
import org.example.project.view.components.GradientButton
import org.example.project.view.components.OutlinedButtonCommon
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import store_clother.shared.generated.resources.Res
import store_clother.shared.generated.resources.already_have_account
import store_clother.shared.generated.resources.intro_analytics
import store_clother.shared.generated.resources.intro_inventory
import store_clother.shared.generated.resources.intro_store
import store_clother.shared.generated.resources.sign_up_for_free

private val introPages = listOf(
    IntroPageData(
        image = Res.drawable.intro_store,
        title = "Quản lý bán hàng",
        subtitle = "Dễ dàng quản lý sản phẩm, đơn hàng\nvà khách hàng mọi lúc mọi nơi"
    ),
    IntroPageData(
        image = Res.drawable.intro_inventory,
        title = "Kiểm soát kho hàng",
        subtitle = "Theo dõi tồn kho theo thời gian thực\nCảnh báo hàng sắp hết tự động"
    ),
    IntroPageData(
        image = Res.drawable.intro_analytics,
        title = "Báo cáo thông minh",
        subtitle = "Phân tích doanh thu, lợi nhuận\nvà xu hướng kinh doanh rõ ràng"
    )
)

@Composable
fun SplashScreen(
    onRegisterClicked: () -> Unit = {},
    onLoginClicked: () -> Unit = {}
) {
    val pagerState = rememberPagerState { introPages.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Store Clother",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                IntroPageContent(page = introPages[page])
            }
            DotsIndicator(
                pageCount = introPages.size,
                currentPage = pagerState.currentPage
            )
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientButton(
                    onClick = onRegisterClicked,
                    gradient = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    ),
                    shape = RoundedCornerShape(12.dp),
                    content = {
                        Text(
                            text = stringResource(Res.string.sign_up_for_free),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                )
                OutlinedButtonCommon(
                    onClick = onLoginClicked,
                    text = stringResource(Res.string.already_have_account),
                    gradient = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
