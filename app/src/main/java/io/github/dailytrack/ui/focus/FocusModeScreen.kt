package io.github.dailytrack.ui.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dailytrack.ui.theme.*

data class AmbientSound(
    val name: String,
    val icon: String,
    val isPremium: Boolean = false
)

@Composable
fun FocusModeScreen(
    isFocusActive: Boolean,
    selectedSound: String?,
    onToggleFocus: () -> Unit,
    onSelectSound: (String) -> Unit
) {
    val sounds = listOf(
        AmbientSound("Rain", "🌧️"),
        AmbientSound("Forest", "🌲"),
        AmbientSound("Ocean", "🌊"),
        AmbientSound("Cafe", "☕"),
        AmbientSound("Fire", "🔥"),
        AmbientSound("Wind", "💨"),
        AmbientSound("Night", "🌙"),
        AmbientSound("Birds", "🐦"),
        AmbientSound("Piano", "🎹", isPremium = true),
        AmbientSound("Lo-fi", "🎵", isPremium = true),
        AmbientSound("White Noise", "📻", isPremium = true),
        AmbientSound("Thunder", "⛈️", isPremium = true)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isFocusActive) ProductiveColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFocusActive) "🎯 Focus Mode Active" else "🎯 Focus Mode",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isFocusActive) "Stay focused and productive!" else "Enable focus mode to block distractions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onToggleFocus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFocusActive) WastedColor else ProductiveColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (isFocusActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFocusActive) "Stop Focus" else "Start Focus")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ambient Sounds",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose background sounds to help you focus",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sounds) { sound ->
                SoundCard(
                    sound = sound,
                    isSelected = selectedSound == sound.name,
                    onSelect = { onSelectSound(sound.name) }
                )
            }
        }
    }
}

@Composable
fun SoundCard(
    sound: AmbientSound,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .scale(scale)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ProductiveColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) ProductiveColor else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sound.icon,
                    fontSize = 24.sp,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = sound.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) ProductiveColor else MaterialTheme.colorScheme.onSurface
            )
            if (sound.isPremium) {
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExerciseColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
