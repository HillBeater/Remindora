package com.hillbeater.remindora.uiScreens

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hillbeater.remindora.R
import com.hillbeater.remindora.database.UserInformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    scaffoldState: DrawerState,
    userInfo: UserInformation,
    selectedMenuItem: String,
    onMenuItemSelected: (String) -> Unit,
    coroutineScope: CoroutineScope,
    openActivity: (String) -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = scaffoldState,
        drawerContent = {
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = LocalConfiguration.current.screenWidthDp.dp / 2 + 70.dp)
                    .padding(vertical = 20.dp)
                    .clip(RoundedCornerShape(topEnd = 15.dp, bottomEnd = 15.dp))
                    .background(colorResource(id = R.color.colorPrimary))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(context, ProfileActivity::class.java)
                                context.startActivity(intent)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Color.LightGray,
                                modifier = Modifier.size(72.dp),
                                tonalElevation = 4.dp
                            ) {
                                val imageUrl = userInfo.userImageUrl

                                if (imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Profile Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    val firstLetter = userInfo.userName.firstOrNull()?.uppercase() ?: "?"
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White),
                                    ) {
                                        Text(
                                            text = firstLetter,
                                            color = colorResource(id = R.color.colorPrimary),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = userInfo.userName,
                                color = colorResource(id = R.color.colorBackground),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = colorResource(id = R.color.colorBackground).copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Drawer menu items
                    DrawerMenuItem(
                        title = "Home",
                        icon = Icons.Default.Home,
                        isSelected = selectedMenuItem == "Home"
                    ) {
                        onMenuItemSelected("Home")
                        coroutineScope.launch { scaffoldState.close() }
                        openActivity("Home")
                    }

                    DrawerMenuItem(
                        title = "Trash",
                        icon = Icons.Default.Delete,
                        isSelected = selectedMenuItem == "Trash"
                    ) {
                        onMenuItemSelected("Trash")
                        coroutineScope.launch { scaffoldState.close() }
                        openActivity("Trash")
                    }

                    DrawerMenuItem(
                        title = "Settings",
                        icon = Icons.Default.Settings,
                        isSelected = selectedMenuItem == "Settings"
                    ) {
                        onMenuItemSelected("Settings")
                        coroutineScope.launch { scaffoldState.close() }
                        openActivity("Settings")
                    }
                }
            }
        },
        gesturesEnabled = scaffoldState.isOpen,
        content = content
    )
}

@Composable
fun DrawerMenuItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) colorResource(id = R.color.colorBackground) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = if(isSelected) colorResource(id = R.color.colorPrimary) else colorResource(id = R.color.colorBackground))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = if(isSelected) colorResource(id = R.color.colorPrimary) else colorResource(id = R.color.colorBackground), style = MaterialTheme.typography.titleMedium)
    }
}

