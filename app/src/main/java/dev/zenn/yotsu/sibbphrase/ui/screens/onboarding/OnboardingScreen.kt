package dev.zenn.yotsu.sibbphrase.ui.screens.onboarding

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
import androidx.compose.ui.res.stringResource
import dev.zenn.yotsu.sibbphrase.R

private data class OnboardingPage(
    val icon:        ImageVector,
    val titleRes:    Int,
    val descriptionRes: Int
)

private val pages = listOf(
    OnboardingPage(
        icon        = Icons.Outlined.Lock,
        titleRes    = R.string.onboarding_p1_title,
        descriptionRes = R.string.onboarding_p1_desc
    ),
    OnboardingPage(
        icon        = Icons.Outlined.Shield,
        titleRes    = R.string.onboarding_p2_title,
        descriptionRes = R.string.onboarding_p2_desc
    ),
    OnboardingPage(
        icon        = Icons.Outlined.FamilyRestroom,
        titleRes    = R.string.onboarding_p3_title,
        descriptionRes = R.string.onboarding_p3_desc
    ),
    OnboardingPage(
        icon        = Icons.Outlined.Key,
        titleRes    = R.string.onboarding_p4_title,
        descriptionRes = R.string.onboarding_p4_desc
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
                    Text(
                    text = stringResource(R.string.onboarding_skip),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    Text(stringResource(R.string.onboarding_back), fontSize = 17.sp)
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
                    if (isLastPage) stringResource(R.string.onboarding_finish) else stringResource(R.string.onboarding_next),
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
            text       = stringResource(page.titleRes),
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )

        Text(
            text      = stringResource(page.descriptionRes),
            fontSize  = 17.sp,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 28.sp
        )
    }
}
