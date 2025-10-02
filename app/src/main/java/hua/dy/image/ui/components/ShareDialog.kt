package hua.dy.image.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import hua.dy.image.bean.ImageBean
import hua.dy.image.bean.isWebp
import hua.dy.image.ui.gifImageLoader
import hua.dy.image.utils.FileType
import hua.dy.image.utils.Webp2GifUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import splitties.init.appCtx

@Composable
fun ShareDialog(
    modifier: Modifier = Modifier,
    imageBean: ImageBean,
    onDismiss: () -> Unit,
    onShareClick: (path: String) -> Unit,
) {
    var showProgress by remember { mutableStateOf(false) }
    var imageBean by remember { mutableStateOf(imageBean) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "分享表情包",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .align(Alignment.CenterHorizontally)
                ) {
                    AnimatedContent(
                        targetState = showProgress,
                        modifier = Modifier.fillMaxSize()
                    ) { state ->
                        if (state) {
                            CircularProgressIndicator()
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(appCtx)
                                    .data(imageBean.imagePath)
                                    .build(),
                                imageLoader = gifImageLoader,
                                contentScale = ContentScale.Fit,
                                contentDescription = null,
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (imageBean.isWebp) {
                        Button(onClick = {
                            showProgress = true
                            scope.launch(Dispatchers.IO) {
                                val file =
                                    appCtx.externalCacheDir!!.resolve("image_share/share.gif")
                                val result = Webp2GifUtils.convert(
                                    webpPath = imageBean.imagePath,
                                    gifPath = file.path
                                )
                                if (result) {
                                    imageBean =
                                        ImageBean(imagePath = file.path, fileType = FileType.GIF)
                                }
                                showProgress = false
                            }
                        }) {
                            Text(text = "转为GIF")
                        }
                    }

                    Button(onClick = {
                        onShareClick(imageBean.imagePath)
                    }) {
                        Text(text = "分享到其他APP")
                    }
                }
            }
        }
    }
}