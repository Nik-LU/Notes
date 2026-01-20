package com.practicum.noteslu.presentation.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.practicum.noteslu.domain.ContentItem

@Composable
fun Content(
    modifier: Modifier = Modifier,
    content: List<ContentItem>,
    onDeleteImageClick: (Int) -> Unit,
    onTextChanged: (Int, String) -> Unit
) {

    LazyColumn(
        modifier = modifier
    ) {
        content.forEachIndexed { index, contentItem ->
            item(key = index) {
                when (contentItem) {
                    is ContentItem.Image -> {
                        val isAlreadyDisplayed =
                            index > 0 && content[index - 1] is ContentItem.Image

                        content.takeIf { !isAlreadyDisplayed }
                            ?.drop(index)
                            ?.takeWhile { it is ContentItem.Image }
                            ?.map { (it as ContentItem.Image).url }
                            ?.let { urls ->
                                ImageGroup(
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp),
                                    imageUrls = urls,
                                    onDeleteImageClick = { imageIndex ->
                                        onDeleteImageClick(index + imageIndex)
                                    }
                                )
                            }
                    }

                    is ContentItem.Text -> {
                        TextContent(
                            text = contentItem.content,
                            onTextChanged = {
                                onTextChanged(index, it)
                            }
                        )

                    }
                }
            }
        }
    }
}

@Composable
private fun ImageContent(
    modifier: Modifier = Modifier,
    imageUrl: String,
    onDeleteImageClick: () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            model = imageUrl,
            contentDescription = "",
            contentScale = ContentScale.FillWidth
        )
        Icon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                .clickable {
                    onDeleteImageClick()
                },
            imageVector = Icons.Default.Close,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ImageGroup(
    modifier: Modifier = Modifier,
    imageUrls: List<String>,
    onDeleteImageClick: (Int) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imageUrls.forEachIndexed { index, uri ->
            ImageContent(
                modifier = Modifier.weight(1f),
                imageUrl = uri,
                onDeleteImageClick = {
                    onDeleteImageClick(index)
                }
            )
        }
    }
}

@Composable
private fun TextContent(
    modifier: Modifier = Modifier,
    text: String,
    onTextChanged: (String) -> Unit
) {
    TextField(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        value = text,
        onValueChange = onTextChanged,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        placeholder = {
            Text(
                text = "Введите что-то ниже",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }
    )
}

