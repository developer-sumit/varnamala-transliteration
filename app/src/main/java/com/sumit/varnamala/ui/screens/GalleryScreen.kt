package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.MediaPermissionHandler

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private data class GalleryImage(
    val id: Long, val uri: Uri, val date: Long, val width: Int?, val height: Int?
)

@Composable
fun GalleryScreen(navController: NavController) {
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val context = LocalContext.current
    val resolver = context.contentResolver

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyStaggeredGridState()
    val showScrollToTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 10 } }

    var isLoading by remember { mutableStateOf(false) }
    var images by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var showGalleryInfoDialog by remember { mutableStateOf(false) }

    suspend fun loadImages() {
        isLoading = true
        val maxItems = 500
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val result = mutableListOf<GalleryImage>()
        withContext(Dispatchers.IO) {
            try {
                val cursor: Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val args = Bundle().apply {
                        putStringArray(
                            ContentResolver.QUERY_ARG_SORT_COLUMNS,
                            arrayOf(MediaStore.Images.Media.DATE_ADDED)
                        )
                        putInt(
                            ContentResolver.QUERY_ARG_SORT_DIRECTION,
                            ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                        )
                        putInt(ContentResolver.QUERY_ARG_LIMIT, maxItems)
                    }
                    resolver.query(collection, projection, args, null)
                } else {
                    // For older versions, don't use LIMIT in the query string as it's not supported
                    // Instead, we'll limit the results after fetching
                    resolver.query(
                        collection,
                        projection,
                        null,
                        null,
                        MediaStore.Images.Media.DATE_ADDED + " DESC"
                    )
                }

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val wCol = c.getColumnIndex(MediaStore.Images.Media.WIDTH)
                    val hCol = c.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                    var count = 0
                    while (c.moveToNext() && count < maxItems) {
                        val id = c.getLong(idCol)
                        val date = c.getLong(dateCol)
                        val width = if (wCol != -1) c.getInt(wCol) else null
                        val height = if (hCol != -1) c.getInt(hCol) else null
                        val contentUri = ContentUris.withAppendedId(collection, id)
                        result += GalleryImage(id, contentUri, date, width, height)
                        count++
                    }
                }
            } catch (sec: SecurityException) {
                // Permission denied or revoked; leave list empty
            } catch (iae: IllegalArgumentException) {
                // Some OEMs may still reject query args; fallback to simplest query
                try {
                    resolver.query(collection, projection, null, null, null)?.use { c ->
                        val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                        val wCol = c.getColumnIndex(MediaStore.Images.Media.WIDTH)
                        val hCol = c.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                        var count = 0
                        while (c.moveToNext() && count < maxItems) {
                            val id = c.getLong(idCol)
                            val date = c.getLong(dateCol)
                            val width = if (wCol != -1) c.getInt(wCol) else null
                            val height = if (hCol != -1) c.getInt(hCol) else null
                            val contentUri = ContentUris.withAppendedId(collection, id)
                            result += GalleryImage(id, contentUri, date, width, height)
                            count++
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
        images = result
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, topInset + 10.dp, 16.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(
                icon = painterResource(R.drawable.ic_info),
                onClick = { showGalleryInfoDialog = true })
            Text(
                text = stringResource(R.string.gallery),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            GlassButton(
                icon = painterResource(R.drawable.ic_settings_outline),
                onClick = { navController.navigate(Screen.Settings.route) })
        }

        MediaPermissionHandler(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface), onGranted = {

                LaunchedEffect(Unit) {
                    loadImages()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Grid
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(minSize = 128.dp),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp, 16.dp, 16.dp, 0.dp)
                            .clip(RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)),
                        verticalItemSpacing = 10.dp,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(images, key = { it.id }) { img ->
                            Surface(
                                tonalElevation = 2.dp,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        placementSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                    .fillMaxWidth()
                                    .border(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(20.dp),
                                        width = 2.dp
                                    )
                                    .clickable {
                                        navController.navigate(Screen.ImageDetail.create(img.id))
                                    }) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(img.uri)
                                        .crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(
                                            (img.width ?: 0).toFloat() / (img.height ?: 0).toFloat()
                                        )
                                )
                            }
                        }
                    }

                    // Scroll to top button
                    this@Column.AnimatedVisibility(
                        visible = showScrollToTop,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp)
                    ) {
                        GlassButton(
                            icon = painterResource(R.drawable.ic_chevron_up), onClick = {
                                scope.launch { gridState.animateScrollToItem(0) }
                            })
                    }
                }
            })
    }

    if (showGalleryInfoDialog) {
        AlertDialog(
            onDismissRequest = { showGalleryInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showGalleryInfoDialog = false }) {
                    Text(text = stringResource(R.string.close))
                }
            },
            title = { Text(text = stringResource(R.string.gallery_info_title)) },
            text = { Text(text = stringResource(R.string.gallery_info_message)) }
        )
    }
}
