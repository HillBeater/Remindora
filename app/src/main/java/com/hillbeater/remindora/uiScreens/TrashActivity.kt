package com.hillbeater.remindora.uiScreens

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.hillbeater.remindora.R
import com.hillbeater.remindora.database.Reminder
import com.hillbeater.remindora.database.RemindoraDatabase
import com.hillbeater.remindora.database.UserInformation
import com.hillbeater.remindora.ui.theme.RemindoraTheme
import com.hillbeater.remindora.viewModels.TrashViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TrashActivity : AppCompatActivity() {

    private val trashViewModel: TrashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemindoraTheme {
                TrashScreen(trashViewModel = trashViewModel, context = this)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TrashScreen(trashViewModel: TrashViewModel, context: TrashActivity) {
        val scaffoldState = rememberDrawerState(DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()

        // Observe StateFlows from the ViewModel
        val reminders by trashViewModel.reminders.collectAsState()
        val userInfo by trashViewModel.userInfo.collectAsState()
        val isSelectionMode by trashViewModel.isSelectionMode.collectAsState()
        val selectedItems by trashViewModel.selectedItems.collectAsState()

        var showRecoverDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        val selectedMenuItem by remember { mutableStateOf("Trash") }

        userInfo?.let {
            AppDrawer(
                scaffoldState = scaffoldState,
                userInfo = it,
                selectedMenuItem = selectedMenuItem,
                onMenuItemSelected = { /* No-op or handle if needed for drawer */ },
                coroutineScope = coroutineScope,
                openActivity = { screen ->
                    when (screen) {
                        "Home" -> {
                            val intent = Intent(context, HomeActivity::class.java)
                            context.startActivity(intent)
                        }

                        "Settings" -> {
                            val intent = Intent(context, SettingActivity::class.java)
                            context.startActivity(intent)
                        }
                    }
                }
            ) {
                Scaffold(
                    containerColor = colorResource(id = R.color.colorBackground),
                    topBar = {
                        Column {
                            TrashTopAppBar(
                                isSelectionMode = isSelectionMode,
                                onSelectionModeToggle = { trashViewModel.toggleSelectionMode() },
                                scaffoldState = scaffoldState,
                                coroutineScope = coroutineScope
                            )
                            Text(
                                text = if (reminders.size <= 1) "${reminders.size} item" else "${reminders.size} items",
                                modifier = Modifier
                                    .padding(start = 16.dp, bottom = 2.dp)
                                    .fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorResource(id = R.color.colorText),
                                textAlign = TextAlign.Start
                            )
                        }
                    },
                    bottomBar = {
                        if (isSelectionMode) {
                            TrashBottomBar(
                                selectedCount = selectedItems.size,
                                onRecoverClicked = { showRecoverDialog = true },
                                onDeleteClicked = { showDeleteDialog = true }
                            )
                        }
                    }
                ) { paddingValues ->
                    if (reminders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No item",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorResource(id = R.color.colorText)
                            )
                        }
                    } else {
                        TrashGrid(
                            items = reminders,
                            selectedItems = selectedItems.toList(), // Pass a copy
                            isSelectionMode = isSelectionMode,
                            onItemClick = { reminderId ->
                                if (isSelectionMode) {
                                    trashViewModel.toggleReminderSelection(reminderId)
                                } else {
                                    val intent = Intent(context, DetailActivity::class.java).apply {
                                        putExtra("REMINDER_ID", reminderId)
                                        putExtra("FROM_TRASH", true)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
        }

        if (showRecoverDialog) {
            RecoverDialog(
                selectedCount = selectedItems.size,
                onConfirm = {
                    showRecoverDialog = false
                    trashViewModel.recoverSelectedReminders()
                },
                onDismiss = { showRecoverDialog = false }
            )
        }

        if (showDeleteDialog) {
            DeleteDialog(
                selectedCount = selectedItems.size,
                onConfirm = {
                    showDeleteDialog = false
                    trashViewModel.deleteSelectedRemindersPermanently()
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TrashTopAppBar(
        isSelectionMode: Boolean,
        onSelectionModeToggle: (Boolean) -> Unit,
        scaffoldState: DrawerState,
        coroutineScope: CoroutineScope,
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(id = R.color.colorBackground)),
            title = {
                Text(
                    "Trash",
                    color = colorResource(id = R.color.colorText),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            navigationIcon = {
                IconButton(onClick = { coroutineScope.launch { scaffoldState.open() } }) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = colorResource(id = R.color.colorPrimary)
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        onSelectionModeToggle(!isSelectionMode)
                    },
                    modifier = Modifier
                        .padding(end = 8.dp, top = 8.dp, bottom = 8.dp)
                        .height(30.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = colorResource(id = R.color.colorText).copy(alpha = 0.2f),
                        contentColor = colorResource(id = R.color.colorPrimary)
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isSelectionMode) "Cancel" else "Select",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        )
    }

    @Composable
    fun TrashBottomBar(
        selectedCount: Int,
        onRecoverClicked: () -> Unit,
        onDeleteClicked: () -> Unit,
    ) {
        BottomAppBar(
            containerColor = colorResource(id = R.color.colorText).copy(alpha = 0.2f),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = when {
                        selectedCount == 0 -> "Select items"
                        selectedCount == 1 -> "1 Reminder Selected"
                        else -> "$selectedCount Reminders Selected"
                    },
                    color = colorResource(id = R.color.colorText),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            var menuExpanded by remember { mutableStateOf(false) }

            Box {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = colorResource(id = R.color.colorPrimary),
                            shape = CircleShape
                        )
                ) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Options",
                            tint = colorResource(id = R.color.colorPrimary)
                        )
                    }
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.width(190.dp)
                        .background(colorResource(id = R.color.colorSecondaryButton))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                Modifier.fillMaxWidth().height(30.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (selectedCount > 0) "Recover" else "Recover All",
                                    color = colorResource(id = R.color.colorBlack)
                                )
                                Icon(
                                    Icons.Default.Restore,
                                    contentDescription = "Recover",
                                    tint = colorResource(id = R.color.colorBlack)
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onRecoverClicked()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(
                                Modifier.fillMaxWidth().height(30.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (selectedCount > 0) "Delete" else "Delete All",
                                    color = Color.Red
                                )
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteClicked()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun TrashGrid(
        items: List<Reminder>,
        selectedItems: List<Int>,
        isSelectionMode: Boolean,
        onItemClick: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { reminder ->
                val isSelected = selectedItems.contains(reminder.id)
                val backgroundColor = Color(reminder.bgColor)
                val textColor = Color(reminder.textColor)

                val cardColors = if (isSelected) {
                    CardDefaults.cardColors(containerColor = Color.LightGray)
                } else {
                    CardDefaults.cardColors(containerColor = backgroundColor)
                }

                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentHeight()
                        .clickable { onItemClick(reminder.id) },
                    shape = RoundedCornerShape(15.dp),
                    colors = cardColors,
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = formatReminderFromStrings(reminder.date, reminder.time),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = reminder.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    private fun formatReminderFromStrings(dateStr: String, timeStr: String): String {
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val date = LocalDate.parse(dateStr, dateFormatter)
        val time = LocalTime.parse(timeStr, timeFormatter)

        val dateTime = LocalDateTime.of(date, time)

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
        val dateFormat = DateTimeFormatter.ofPattern("dd MMM")

        return when (dateTime.toLocalDate()) {
            today -> "Today ${dateTime.format(timeFormat)}"
            tomorrow -> "Tomorrow ${dateTime.format(timeFormat)}"
            else -> "${dateTime.format(dateFormat)} ${dateTime.format(timeFormat)}"
        }
    }

    @Composable
    fun RecoverDialog(
        selectedCount: Int,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Recover Reminders") },
            text = {
                Text(
                    text = if (selectedCount > 0)
                        "Do you want to recover the selected $selectedCount reminder${if (selectedCount > 1) "s" else ""}?"
                    else
                        "Do you want to recover all reminders in the trash?"
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.colorPrimary))
                ) {
                    Text(
                        text = if (selectedCount > 0) "Recover ($selectedCount)" else "Recover All",
                        color = colorResource(id = R.color.colorText)
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, colorResource(id = R.color.colorPrimary))
                ) {
                    Text("Cancel", color = colorResource(id = R.color.colorPrimary))
                }
            }
        )
    }

    @Composable
    fun DeleteDialog(
        selectedCount: Int,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Reminders") },
            text = {
                Text(
                    text = if (selectedCount > 0)
                        "Are you sure you want to permanently delete the selected $selectedCount reminder${if (selectedCount > 1) "s" else ""}? This action can't be undone."
                    else
                        "Are you sure you want to permanently delete all reminders in the trash? This action can't be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = colorResource(id = R.color.colorText))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, colorResource(id = R.color.colorPrimary))
                ) {
                    Text("Cancel", color = colorResource(id = R.color.colorPrimary))
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        trashViewModel.refreshUserInfo()
    }
}