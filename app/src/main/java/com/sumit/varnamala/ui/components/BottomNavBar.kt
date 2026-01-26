package com.sumit.varnamala.ui.components

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun BottomNavBar(
    navController: NavController, currentRoute: String?
) {
    val items = listOf(
        Triple(
            Screen.Home.route, R.drawable.ic_camera, R.string.bottom_nav_camera
        ), Triple(
            Screen.TextInput.route, R.drawable.ic_keyboard, R.string.bottom_nav_text
        ), Triple(
            Screen.Gallery.route, R.drawable.ic_image_outline, R.string.bottom_nav_gallery
        )
    )

    // Calculate the index of the selected item
    val selectedIndex = items.indexOfFirst { it.first == currentRoute }.coerceAtLeast(0)

    // Animate the horizontal offset for the sliding background
    val animatedOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(), animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow
        ), label = "bottomNavOffset"
    )

    val density = LocalDensity.current

    var containerWidth by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(
                horizontal = 24.dp, vertical = 10.dp
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .onSizeChanged { containerWidth = it.width }) {
            // Background container with sliding indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .height(70.dp)
            )

            // Sliding background indicator
            if (containerWidth > 0) {
                val itemWidth = containerWidth / items.size
                Box(
                    modifier = Modifier
                        .width(with(density) { itemWidth.toDp() })
                        .height(70.dp)
                        .offset(x = with(density) { (animatedOffset * itemWidth).toDp() })
                        .clip(RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                )
            }

            // Navigation items on top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (route, iconRes, labelRes) ->
                    val selected = currentRoute == route
                    val iconTint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val labelColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(vertical = 2.dp)
                            .height(70.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = stringResource(labelRes),
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(labelRes),
                            color = labelColor,
                            fontSize = 11.sp,
                            fontWeight = if (selected) {
                                FontWeight.Medium
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }
                }
            }
        }
    }
}
