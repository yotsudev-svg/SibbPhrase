package dev.zenn.yotsu.famipass.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(
    val icon:        ImageVector,
    val title:       String,
    val description: String
)

private val pages = listOf(
    OnboardingPage(
        icon        = Icons.Outlined.Lock,
        title       = "FamiPassへようこそ",
        description = "パスワードや電話番号など\n大切な情報を安全に家族と\n共有するためのアプリです"
    ),
    OnboardingPage(
        icon        = Icons.Outlined.Shield,
        title       = "暗号化で守る",
        description = "送信前に情報を暗号化するので\nメールやLINEに残っても\n他人には読めません"
    ),
    OnboardingPage(
        icon        = Icons.Outlined.FamilyRestroom,
        title       = "家族だけが復元できる",
        description = "家族共通の「合言葉」を\nQRコードで共有するだけで\n同じアプリで元に戻せます"
    ),
    OnboardingPage(
        icon        = Icons.Outlined.Key,
        title       = "まず合言葉を設定しよう",
        description = "最初に家族共通の合言葉を\n決めてください\n設定画面からいつでも変更できます"
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }
    val isLastPage = currentPage == pages.lastIndex

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(modifier = Modifier.fillMaxWidth()) {
            if (!isLastPage) {
                TextButton(
                    onClick  = onFinish,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text("スキップ", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        AnimatedContent(
            targetState   = currentPage,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) togetherWith
                            slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300))
                } else {
                    slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) togetherWith
                            slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                }
            },
            label = "onboarding_page"
        ) { page ->
            PageContent(pages[page])
        }

        Spacer(Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.padding(bottom = 32.dp)
        ) {
            pages.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (i == currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .size(if (i == currentPage) 10.dp else 8.dp)
                )
            }
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (currentPage > 0) {
                OutlinedButton(
                    onClick  = { currentPage-- },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("戻る", fontSize = 17.sp)
                }
            }

            Button(
                onClick  = {
                    if (isLastPage) onFinish()
                    else currentPage++
                },
                modifier = Modifier.weight(2f).height(56.dp)
            ) {
                Text(
                    if (isLastPage) "合言葉を設定する 🔑" else "次へ",
                    fontSize = 17.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier            = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier        = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = page.icon,
                contentDescription = null,
                modifier           = Modifier.size(64.dp),
                tint               = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text       = page.title,
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )

        Text(
            text      = page.description,
            fontSize  = 17.sp,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 28.sp
        )
    }
}
