package com.hillbeater.remindora.uiScreens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hillbeater.remindora.R
import com.hillbeater.remindora.ui.theme.RemindoraTheme
import com.hillbeater.remindora.viewModels.DetailViewModel
import java.util.Calendar


class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        val fromTrash = intent.getBooleanExtra("FROM_TRASH", false)

        setContent {
            RemindoraTheme {
                val detailViewModel: DetailViewModel = viewModel(
                    factory = DetailViewModel.DetailViewModelFactory(
                        application
                    )
                )

                LaunchedEffect(reminderId) {
                    if (reminderId != -1) {
                        detailViewModel.loadReminder(reminderId)
                    }
                }

                ReminderDetailScreen(
                    reminderId = reminderId,
                    fromTrash = fromTrash,
                    detailViewModel = detailViewModel,
                    onBack = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ReminderDetailScreen(
        reminderId: Int,
        fromTrash: Boolean,
        detailViewModel: DetailViewModel,
        onBack: () -> Unit
    ) {
        val context = LocalContext.current
        val calendar = remember { Calendar.getInstance() }

        val title by detailViewModel.title.observeAsState("")
        val date by detailViewModel.date.observeAsState("")
        val time by detailViewModel.time.observeAsState("")
        val content by detailViewModel.content.observeAsState("")
        val bgColor by detailViewModel.bgColor.observeAsState(Color.White)
        val textColor by detailViewModel.textColor.observeAsState(Color.Black)
        val pushNotification by detailViewModel.pushNotification.observeAsState(false)
        val isLoading by detailViewModel.isLoading.observeAsState(true)
        val isEditMode by detailViewModel.isEditMode.observeAsState(false)
        val showDeleteDialog by detailViewModel.showDeleteDialog.observeAsState(false)
        val isSaveEnabled by detailViewModel.isSaveEnabled.observeAsState(false)

        val showDatePickerEvent by detailViewModel.showDatePickerEvent.observeAsState(false)
        val showTimePickerEvent by detailViewModel.showTimePickerEvent.observeAsState(false)
        val requestExactAlarmPermission by detailViewModel.requestExactAlarmPermission.observeAsState(
            false
        )
        val navigateBack by detailViewModel.navigateBack.observeAsState(false)

        // Date Picker Dialog
        val datePickerDialog = remember {
            DatePickerDialog(
                context,
                R.style.MyCalendarDialogTheme,
                { _, year, month, dayOfMonth ->
                    detailViewModel.setDate(year, month, dayOfMonth)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate =
                    System.currentTimeMillis() - 1000 // Prevent selecting past dates
            }
        }

        // Time Picker Dialog
        val timePickerDialog = remember {
            TimePickerDialog(
                context,
                R.style.MyTimePickerDialogTheme,
                { _, hourOfDay, minute ->
                    detailViewModel.setTime(hourOfDay, minute)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // 24-hour format
            )
        }

        // Trigger date picker display
        LaunchedEffect(showDatePickerEvent) {
            if (showDatePickerEvent) {
                datePickerDialog.show()
            }
        }

        // Trigger time picker display
        LaunchedEffect(showTimePickerEvent) {
            if (showTimePickerEvent) {
                timePickerDialog.show()
            }
        }

        // Trigger exact alarm permission request
        LaunchedEffect(requestExactAlarmPermission) {
            if (requestExactAlarmPermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    context.startActivity(intent)
                }
            }
        }

        // Trigger navigation back
        LaunchedEffect(navigateBack) {
            if (navigateBack) {
                onBack()
            }
        }


        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Scaffold(
                containerColor = colorResource(id = R.color.colorBackground),
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colorResource(id = R.color.colorBackground)
                        ),
                        title = {
                            Text(
                                "Reminder detail",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = Bold),
                                color = colorResource(id = R.color.colorText)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (!fromTrash) {
                                var menuExpanded by remember { mutableStateOf(false) }

                                Box(
                                    modifier = Modifier
                                        .padding(end = 15.dp)
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
                                    modifier = Modifier
                                        .background(colorResource(id = R.color.colorSecondaryButton))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Edit",
                                                color = colorResource(id = R.color.colorBlack)
                                            )
                                        },
                                        onClick = {
                                            detailViewModel.enableEditMode()
                                            menuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Delete",
                                                color = colorResource(id = R.color.colorBlack)
                                            )
                                        },
                                        onClick = {
                                            detailViewModel.showDeleteConfirmation()
                                            menuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { detailViewModel.setTitle(it) },
                        label = { Text("Reminder Title") },
                        enabled = isEditMode,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        textStyle = TextStyle(color = colorResource(id = R.color.colorText)),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = colorResource(id = R.color.colorPrimary),
                            unfocusedBorderColor = colorResource(id = R.color.colorText),
                            cursorColor = colorResource(id = R.color.colorText),
                            focusedLabelColor = colorResource(id = R.color.colorPrimary),
                            unfocusedLabelColor = colorResource(id = R.color.colorText)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = { detailViewModel.setContent(it) },
                        label = { Text("Content") },
                        enabled = isEditMode,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        textStyle = TextStyle(color = colorResource(id = R.color.colorText)),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = colorResource(id = R.color.colorPrimary),
                            unfocusedBorderColor = colorResource(id = R.color.colorText),
                            cursorColor = colorResource(id = R.color.colorText),
                            focusedLabelColor = colorResource(id = R.color.colorPrimary),
                            unfocusedLabelColor = colorResource(id = R.color.colorText)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { detailViewModel.showDatePicker() },
                            enabled = isEditMode,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.colorPrimaryButton),
                                contentColor = colorResource(id = R.color.colorPrimaryButtonText)
                            )
                        ) {
                            Text(text = date.ifEmpty { "Select Date" })
                        }
                        Button(
                            onClick = { detailViewModel.showTimePicker() },
                            enabled = isEditMode,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.colorPrimaryButton),
                                contentColor = colorResource(id = R.color.colorPrimaryButtonText)
                            )
                        ) {
                            Text(text = time.ifEmpty { "Select Time" })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Background Color",
                        fontWeight = Bold,
                        color = colorResource(id = R.color.colorText)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val bgColorInts = listOf(
                        0xFF121212, 0xFF1F1B24, 0xFF263238, 0xFF2E2E2E, 0xFF3B2F2F, 0xFF0D3B66
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        bgColorInts.forEach { colorInt ->
                            val color = Color(colorInt)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (color == bgColor) 3.dp else 1.dp,
                                        color = if (color == bgColor) colorResource(id = R.color.colorPrimary) else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable(enabled = isEditMode) {
                                        detailViewModel.setBgColor(
                                            color
                                        )
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Text Color",
                        fontWeight = Bold,
                        color = colorResource(id = R.color.colorText)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val textColorInts = listOf(
                        0xFFFFFFFF, 0xFFE0C9FF, 0xFFCFD8DC, 0xFFF5EAEA, 0xFFFFD166
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        textColorInts.forEach { colorInt ->
                            val color = Color(colorInt)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (color == textColor) 3.dp else 1.dp,
                                        color = if (color == textColor) colorResource(id = R.color.colorPrimary) else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable(enabled = isEditMode) {
                                        detailViewModel.setTextColor(
                                            color
                                        )
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = pushNotification,
                            onCheckedChange = { detailViewModel.setPushNotification(it) },
                            enabled = isEditMode,
                            colors = CheckboxDefaults.colors(
                                checkedColor = colorResource(id = R.color.colorPrimary),
                            )
                        )
                        Text(
                            text = "Enable Push Notification",
                            color = colorResource(id = R.color.colorText)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isEditMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick = { detailViewModel.cancelEditMode() },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    contentColor = colorResource(id = R.color.colorPrimaryButton),
                                    containerColor = Color.Transparent
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = colorResource(id = R.color.colorPrimary)
                                )
                            ) {
                                Text(
                                    "Cancel",
                                    color = colorResource(id = R.color.colorPrimaryButton)
                                )
                            }
                            Button(
                                onClick = { detailViewModel.updateReminder() },
                                enabled = isSaveEnabled && !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(id = R.color.colorPrimaryButton),
                                    contentColor = colorResource(id = R.color.colorPrimaryButtonText)
                                )
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { detailViewModel.dismissDeleteConfirmation() },
                confirmButton = {
                    TextButton(onClick = {
                        detailViewModel.deleteReminder(reminderId)
                    }) {
                        Text("Delete", color = colorResource(id = R.color.colorPrimary))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { detailViewModel.dismissDeleteConfirmation() }) {
                        Text("Cancel", color = colorResource(id = R.color.colorPrimary))
                    }
                },
                title = { Text("Delete Reminder") },
                text = { Text("This reminder will be deleted from the library. It will be in Trash") }
            )
        }
    }
}