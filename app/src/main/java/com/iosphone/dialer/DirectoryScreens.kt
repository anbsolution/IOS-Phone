package com.iosphone.dialer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.DateFormat
import java.util.Date

private val DirectoryBlue = Color(0xFF007AFF)

private data class PhoneContact(val name: String, val number: String)
private data class PhoneRecent(val name: String, val number: String, val type: Int, val date: Long)

private object PhoneDirectory {
    fun contacts(context: Context): List<PhoneContact> {
        val result = linkedMapOf<String, PhoneContact>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            val ni = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val pi = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(ni).orEmpty()
                val number = cursor.getString(pi).orEmpty()
                if (number.isNotBlank()) result["$name|$number"] = PhoneContact(name, number)
            }
        }
        return result.values.toList()
    }

    fun recents(context: Context): List<PhoneRecent> {
        val result = mutableListOf<PhoneRecent>()
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null, null, CallLog.Calls.DATE + " DESC"
        )?.use { cursor ->
            val ni = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val pi = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val ti = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val di = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            while (cursor.moveToNext() && result.size < 100) {
                result += PhoneRecent(cursor.getString(ni).orEmpty(),
                    cursor.getString(pi).orEmpty(), cursor.getInt(ti), cursor.getLong(di))
            }
        }
        return result
    }

    fun favorites(context: Context): Set<String> =
        context.getSharedPreferences("phone_directory", Context.MODE_PRIVATE)
            .getStringSet("favorites", emptySet())?.toSet() ?: emptySet()

    fun toggleFavorite(context: Context, number: String): Set<String> {
        val values = favorites(context).toMutableSet()
        if (!values.add(number)) values.remove(number)
        context.getSharedPreferences("phone_directory", Context.MODE_PRIVATE)
            .edit().putStringSet("favorites", values).apply()
        return values
    }
}

@Composable
fun ContactsScreen(onCall: (String) -> Unit) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(emptyList<PhoneContact>()) }
    var favorites by remember { mutableStateOf(PhoneDirectory.favorites(context)) }
    var query by remember { mutableStateOf("") }
    var denied by remember { mutableStateOf(false) }
    fun load() {
        contacts = PhoneDirectory.contacts(context)
        favorites = PhoneDirectory.favorites(context)
        denied = false
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) load() else denied = true
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED) load()
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(),
            singleLine = true, label = { Text("Search name or number") })
        if (contacts.isEmpty()) {
            PermissionPrompt("Allow contacts access to view and search saved contacts.", denied) {
                permission.launch(Manifest.permission.READ_CONTACTS)
            }
        } else {
            LazyColumn {
                items(contacts.filter {
                    it.name.contains(query, true) || it.number.contains(query, true)
                }, key = { it.name + it.number }) { contact ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { onCall(contact.number) }) {
                            Text(contact.name.ifBlank { "Unknown contact" }, fontWeight = FontWeight.SemiBold)
                            Text(contact.number, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            favorites = PhoneDirectory.toggleFavorite(context, contact.number)
                        }) {
                            Icon(if (contact.number in favorites) Icons.Default.Star else Icons.Default.StarBorder,
                                "Toggle favorite", tint = DirectoryBlue)
                        }
                        IconButton(onClick = { onCall(contact.number) }) {
                            Icon(Icons.Default.Call, "Call", tint = DirectoryBlue)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(onCall: (String) -> Unit) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(emptyList<PhoneContact>()) }
    var favorites by remember { mutableStateOf(PhoneDirectory.favorites(context)) }
    var denied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) contacts = PhoneDirectory.contacts(context) else denied = true
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED) contacts = PhoneDirectory.contacts(context)
    }
    val selected = contacts.filter { it.number in favorites }
    if (selected.isEmpty()) {
        PermissionPrompt(
            if (favorites.isEmpty()) "Star a contact to add it to Favorites."
            else "Allow contacts access to display your saved favorites.",
            denied
        ) { permission.launch(Manifest.permission.READ_CONTACTS) }
    } else {
        LazyColumn {
            items(selected, key = { it.name + it.number }) { contact ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onCall(contact.number) }) {
                        Text(contact.name.ifBlank { contact.number }, fontWeight = FontWeight.SemiBold)
                        Text(contact.number, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        favorites = PhoneDirectory.toggleFavorite(context, contact.number)
                    }) { Icon(Icons.Default.Star, "Remove favorite", tint = DirectoryBlue) }
                    IconButton(onClick = { onCall(contact.number) }) {
                        Icon(Icons.Default.Call, "Call", tint = DirectoryBlue)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun RecentsScreen(onCall: (String) -> Unit) {
    val context = LocalContext.current
    var recents by remember { mutableStateOf(emptyList<PhoneRecent>()) }
    var denied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) recents = PhoneDirectory.recents(context) else denied = true
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED) recents = PhoneDirectory.recents(context)
    }
    if (recents.isEmpty()) {
        PermissionPrompt("Allow call-history access to display recent calls.", denied) {
            permission.launch(Manifest.permission.READ_CALL_LOG)
        }
    } else {
        LazyColumn {
            items(recents) { item ->
                val kind = when (item.type) {
                    CallLog.Calls.INCOMING_TYPE -> "Incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    CallLog.Calls.REJECTED_TYPE -> "Rejected"
                    else -> "Call"
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name.ifBlank { item.number.ifBlank { "Unknown" } },
                            fontWeight = FontWeight.SemiBold)
                        Text("$kind • " + DateFormat.getDateTimeInstance(DateFormat.SHORT,
                            DateFormat.SHORT).format(Date(item.date)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onCall(item.number) }) {
                        Icon(Icons.Default.Call, "Call back", tint = DirectoryBlue)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PermissionPrompt(message: String, denied: Boolean, onGrant: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (denied) "Permission not granted" else message,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (denied) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onGrant) { Text("Allow access") }
    }
}
