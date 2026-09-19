package io.github.dailytrack.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dailytrack.ui.theme.GradientEnd
import io.github.dailytrack.ui.theme.GradientMid
import io.github.dailytrack.ui.theme.GradientStart
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
    val isQuote: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.FormatQuote,
            title = "Parkinson's Law",
            subtitle = "\"The less time you give a task, the more quickly you will finish it.\"",
            description = "Parkinson's Law states that work expands to fill the time available. By setting tight deadlines, you focus your energy and complete tasks faster.",
            isQuote = true
        ),
        OnboardingPage(
            icon = Icons.Default.Timer,
            title = "Timer & Pomodoro",
            subtitle = "Track every moment",
            description = "Use the Timer for open-ended tracking or Pomodoro for structured focus sessions. Both record your time automatically to build your activity history."
        ),
        OnboardingPage(
            icon = Icons.Default.CheckBox,
            title = "Smart Todos",
            subtitle = "Prioritize what matters",
            description = "Add tasks with priority levels (High/Medium/Low), set deadlines, and break them into subtasks. Link todos to your Pomodoro sessions for focused execution."
        ),
        OnboardingPage(
            icon =             Icons.Default.ShowChart,
            title = "Growth Tracking",
            subtitle = "See your progress",
            description = "Your growth score updates automatically based on how you spend your time. Learning, productivity, exercise, and rest all contribute to your overall score."
        ),
        OnboardingPage(
            icon = Icons.Default.Lightbulb,
            title = "Smart Insights",
            subtitle = "Understand your patterns",
            description = "Get AI-powered insights about your habits, detect routine loops, and discover your comfort zone. The more you track, the smarter the insights become."
        ),
        OnboardingPage(
            icon = Icons.Default.Widgets,
            title = "Home Widgets",
            subtitle = "Control at a glance",
            description = "Add Timer, Pomodoro, or Quote widgets to your home screen. Start, stop, and reset directly from the widget without opening the app."
        ),
        OnboardingPage(
            icon = Icons.Default.PhoneAndroid,
            title = "Device Sync",
            subtitle = "Private & secure",
            description = "Sync data between your devices using Syncthing - no cloud, no accounts, no data leaves your control. Your privacy is guaranteed."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientStart.copy(alpha = 0.3f),
                        GradientMid.copy(alpha = 0.5f),
                        GradientEnd.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(
                title = {
                    Text(
                        "Welcome to Soul Track",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.3f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "indicator_scale"
                    )

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .scale(scale)
                            .size(if (isSelected) 12.dp else 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected)
                                    GradientStart
                                else
                                    Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Text("Back", color = Color.White.copy(alpha = 0.8f))
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                if (pagerState.currentPage < pages.size - 1) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GradientStart
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GradientStart
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text("Get Started")
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            tint = GradientStart,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (page.isQuote) {
            Text(
                text = page.subtitle,
                color = GradientStart,
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        } else {
            Text(
                text = page.subtitle,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = page.description,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp),
                lineHeight = 22.sp
            )
        }
    }
}
