package com.hillbeater.remindora.uiScreens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hillbeater.remindora.R
import com.hillbeater.remindora.database.Reminder
import com.hillbeater.remindora.database.RemindoraDatabase
import com.hillbeater.remindora.database.UserInformation
import com.hillbeater.remindora.ui.theme.RemindoraTheme
import com.hillbeater.remindora.viewModels.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs: SharedPreferences = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        val database = RemindoraDatabase.getDatabase(this)
        homeViewModel = ViewModelProvider(this, HomeViewModel.ViewModelFactory(database, this, sharedPrefs))[HomeViewModel::class.java]

        val fromPush = intent.getBooleanExtra("FROM_PUSH", false)
        val reminderId = intent.getIntExtra("REMINDER_ID", -1)

        askNotificationPermission()

        setContent {
            RemindoraTheme {
                Surface(color = colorResource(id = R.color.colorBackground)) {
                    val userInfo by homeViewModel.userInfo.collectAsState()
                    val reminders by homeViewModel.reminders.collectAsState()
                    val showFab by homeViewModel.showFab.collectAsState()
                    val searchText by homeViewModel.searchText.collectAsState()
                    val selectedFilter by homeViewModel.selectedFilter.collectAsState()
                    val filteredReminders by homeViewModel.filteredReminders.collectAsState()


                    if (userInfo != null) {
                        HomeScreen(
                            userInfo = userInfo!!,
                            reminders = reminders,
                            showFab = showFab,
                            searchText = searchText,
                            selectedFilter = selectedFilter,
                            filteredReminders = filteredReminders,
                            onSearchTextChanged = homeViewModel::onSearchTextChanged,
                            onFilterSelected = homeViewModel::onFilterSelected,
                            onAddReminder = homeViewModel::onAddReminder,
                            isReminderExpired = homeViewModel::isReminderExpired,
                            requestExactAlarmPermission = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                startActivity(intent)
                            }
                        )
                    }

                    if (fromPush && reminderId > 0) {
                        val detailIntent = Intent(this, DetailActivity::class.java).apply {
                            putExtra("REMINDER_ID", reminderId)
                        }
                        startActivity(detailIntent)
                    }
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    println("Notification permission already granted.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        else{
            homeViewModel.toggleNotifications(true)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeScreen(
        userInfo: UserInformation,
        reminders: List<Reminder>,
        showFab: Boolean,
        searchText: String,
        selectedFilter: HomeViewModel.FilterType,
        filteredReminders: List<Reminder>,
        onSearchTextChanged: (String) -> Unit,
        onFilterSelected: (HomeViewModel.FilterType) -> Unit,
        onAddReminder: (String, String, String, String, Int, Int, Boolean, () -> Unit, () -> Unit) -> Unit,
        isReminderExpired: (String, String) -> Boolean,
        requestExactAlarmPermission: () -> Unit
    ) {
        val scaffoldState = rememberDrawerState(DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()
        var selectedMenuItem by remember { mutableStateOf("Home") }
        val openBottomSheet = remember { mutableStateOf(false) }

        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    homeViewModel.fetchReminders()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        AppDrawer(
            scaffoldState = scaffoldState,
            userInfo = userInfo,
            selectedMenuItem = selectedMenuItem,
            onMenuItemSelected = { selectedMenuItem = it },
            coroutineScope = coroutineScope,
            openActivity = { screen ->
                when (screen) {
                    "Trash" -> {
                        val intent = Intent(this@HomeActivity, TrashActivity::class.java)
                        startActivity(intent)
                    }
                    "Settings" -> {
                        val intent = Intent(this@HomeActivity, SettingActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        ) {
            Scaffold(
                containerColor = colorResource(id = R.color.colorBackground),
                floatingActionButton = {
                    if (showFab) {
                        FloatingActionButton(
                            onClick = { openBottomSheet.value = true },
                            containerColor = colorResource(id = R.color.colorPrimaryButton)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = colorResource(id = R.color.colorPrimaryButtonText)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(CircleShape)
                            .background(colorResource(id = R.color.colorText).copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = colorResource(id = R.color.colorPrimary),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        coroutineScope.launch { scaffoldState.open() }
                                    }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchText,
                                onValueChange = onSearchTextChanged,
                                placeholder = { Text("Search...") },
                                singleLine = true,
                                colors = TextFieldDefaults.textFieldColors(
                                    containerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = colorResource(id = R.color.colorText)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    RemindersHeader(
                        showFab = showFab,
                        reminders = reminders,
                        selectedFilter = selectedFilter,
                        onFilterSelected = onFilterSelected
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredReminders.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No Reminder", color = colorResource(id = R.color.colorText))
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredReminders.size) { index ->
                                val reminder = filteredReminders[index]
                                Box(
                                    modifier = Modifier.clickable {
                                        val intent = Intent(this@HomeActivity, DetailActivity::class.java).apply {
                                            putExtra("REMINDER_ID", reminder.id)
                                        }
                                        startActivity(intent)
                                    }
                                ) {
                                    ReminderCard(reminder, isReminderExpired)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (openBottomSheet.value) {
            Dialog(onDismissRequest = { openBottomSheet.value = false }) {
                AddNewReminderDialog(
                    onDismiss = { openBottomSheet.value = false },
                    onAdd = { title, date, time, content, bgColor, textColor, notify, onComplete ->
                        onAddReminder(title, date, time, content, bgColor, textColor, notify, {
                            onComplete()
                            openBottomSheet.value = false
                        }, requestExactAlarmPermission)
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddNewReminderDialog(
        onDismiss: () -> Unit,
        onAdd: (
            title: String,
            date: String,
            time: String,
            content: String,
            bgColor: Int,
            textColor: Int,
            pushNotification: Boolean,
            onComplete: () -> Unit
        ) -> Unit
    ) {
        var title by remember { mutableStateOf("") }
        var date by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }

        var bgColor by remember { mutableIntStateOf(Color(0xFF121212).toArgb()) }
        var textColor by remember { mutableIntStateOf(Color(0xFFFFFFFF).toArgb()) }

        var pushNotification by remember { mutableStateOf(true) }

        val context = LocalContext.current

        val calendar = remember { Calendar.getInstance() }
        var isLoading by remember { mutableStateOf(false) }

        val datePickerDialog = remember {
            android.app.DatePickerDialog(
                context,
                R.style.MyTimePickerDialogTheme,
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    date = formatter.format(selectedDate.time)
                },
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.YEAR)
            ).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
            }
        }

        val timePickerDialog = remember {
            TimePickerDialog(
                context,
                R.style.MyTimePickerDialogTheme,
                { _, hourOfDay, minute ->
                    val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                    time = formattedTime
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add New Reminder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.colorText)
                    )
                }
            }
            ,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Reminder Title") },
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
                        onValueChange = { content = it },
                        label = { Text("Content") },
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
                        Button(onClick = { datePickerDialog.show() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.colorPrimaryButton),
                                contentColor = colorResource(id = R.color.colorPrimaryButtonText)
                            )
                        ) {
                            Text(text = date.ifEmpty { "Select Date" })
                        }
                        Button(onClick = { timePickerDialog.show() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.colorPrimaryButton),
                                contentColor = colorResource(id = R.color.colorPrimaryButtonText)
                            )) {
                            Text(text = time.ifEmpty { "Select Time" })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Background Color",
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.colorText)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val bgColorInts = listOf(
                        Color(0xFF121212).toArgb(),
                        Color(0xFF1F1B24).toArgb(),
                        Color(0xFF263238).toArgb(),
                        Color(0xFF2E2E2E).toArgb(),
                        Color(0xFF3B2F2F).toArgb(),
                        Color(0xFF0D3B66).toArgb()
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
                                        width = if (colorInt == bgColor) 3.dp else 1.dp,
                                        color = if (colorInt == bgColor) colorResource(id = R.color.colorPrimary) else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { bgColor = colorInt }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Text Color",
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.colorText)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val textColorInts = listOf(
                        Color(0xFFFFFFFF).toArgb(),
                        Color(0xFFE0C9FF).toArgb(),
                        Color(0xFFCFD8DC).toArgb(),
                        Color(0xFFF5EAEA).toArgb(),
                        Color(0xFFFFD166).toArgb()
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
                                        width = if (colorInt == textColor) 3.dp else 1.dp,
                                        color = if (colorInt == textColor) colorResource(id = R.color.colorPrimary) else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { textColor = colorInt }
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = pushNotification,
                            onCheckedChange = { pushNotification = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colorResource(id = R.color.colorPrimary),
                            )
                        )
                        Text(
                            text = "Enable Push Notification",
                            color = colorResource(id = R.color.colorText)
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
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
                        Text("Cancel", color = colorResource(id = R.color.colorPrimaryButton))
                    }
                    Button(
                        onClick = {
                            isLoading = true
                            onAdd(
                                title, date, time, content,
                                bgColor, textColor, pushNotification
                            ) {
                                isLoading = false
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.colorPrimaryButton),
                            contentColor = colorResource(id = R.color.colorPrimaryButtonText)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = colorResource(id = R.color.colorPrimaryButtonText),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text("Add")
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun RemindersHeader(
        showFab: Boolean,
        reminders: List<Reminder>,
        selectedFilter: HomeViewModel.FilterType,
        onFilterSelected: (HomeViewModel.FilterType) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when (selectedFilter) {
                    HomeViewModel.FilterType.UpcomingLatest -> "Upcoming reminders"
                    HomeViewModel.FilterType.Expired -> "Expired reminders"
                    HomeViewModel.FilterType.ByAddedDateTime -> "Your reminders"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            if (!showFab || reminders.size > 1) { // Only show filter if there are reminders or if FAB is hidden
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = colorResource(id = R.color.colorPrimary)
                            )
                            if (selectedFilter != HomeViewModel.FilterType.ByAddedDateTime) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, shape = CircleShape)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .background(colorResource(id = R.color.colorSecondaryButton))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Filter by:",
                                    color = colorResource(id = R.color.colorPrimary),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = Bold)
                                )
                            },
                            onClick = {},
                            enabled = false,
                        )
                        DropdownMenuItem(
                            text = { Text("By Added Date/Time", color = colorResource(id = R.color.colorBlack)) },
                            onClick = {
                                expanded = false
                                onFilterSelected(HomeViewModel.FilterType.ByAddedDateTime)
                            },
                            trailingIcon = if (selectedFilter == HomeViewModel.FilterType.ByAddedDateTime) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected", tint = colorResource(id = R.color.colorPrimary)) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("By Upcoming Latest", color = colorResource(id = R.color.colorBlack)) },
                            onClick = {
                                expanded = false
                                onFilterSelected(HomeViewModel.FilterType.UpcomingLatest)
                            },
                            trailingIcon = if (selectedFilter == HomeViewModel.FilterType.UpcomingLatest) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected", tint = colorResource(id = R.color.colorPrimary)) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("By Expired", color = colorResource(id = R.color.colorBlack)) },
                            onClick = {
                                expanded = false
                                onFilterSelected(HomeViewModel.FilterType.Expired)
                            },
                            trailingIcon = if (selectedFilter == HomeViewModel.FilterType.Expired) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected", tint = colorResource(id = R.color.colorPrimary)) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ReminderCard(reminder: Reminder, isReminderExpired: (String, String) -> Boolean) {
        val backgroundColor = Color(reminder.bgColor)
        val textColor = Color(reminder.textColor)

        Card(
            modifier = Modifier
                .fillMaxSize()
                .requiredHeight(110.dp),
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Top
                ) {
                    if (isReminderExpired(reminder.date, reminder.time)) {
                        Text(
                            text = "Expired",
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

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

    override fun onResume() {
        super.onResume()
        homeViewModel.refreshUserInfo()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            println("Notification permission granted!")
            homeViewModel.toggleNotifications(true)
        } else {
            println("Notification permission denied.")
            homeViewModel.toggleNotifications(false)
        }
    }
}
