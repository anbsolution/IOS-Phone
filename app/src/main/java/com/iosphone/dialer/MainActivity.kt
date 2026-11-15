package com.iosphone.dialer

import android.os.Bundle
import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.TelecomManager
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext

private val Blue = Color(0xFF087DFA)
private val Ink = Color(0xFF17181C)
private val Grey = Color(0xFF858A93)
private val Pale = Color(0xFFF1F2F6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        setContent {
            MaterialTheme {
                PhoneApp()
            }
        }
    }
}

@Composable
fun PhoneApp() {
    var tab by remember { mutableStateOf("Keypad") }
    var number by remember { mutableStateOf("") }

    val context = LocalContext.current
    var pendingCallNumber by remember { mutableStateOf("") }

    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted && pendingCallNumber.isNotBlank()) {
                val telecom = context.getSystemService(
                    Context.TELECOM_SERVICE
                ) as TelecomManager

                try {
                    telecom.placeCall(
                        Uri.fromParts("tel", pendingCallNumber, null),
                        Bundle()
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Unable to place call: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else if (!granted) {
                Toast.makeText(
                    context,
                    "Call permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
            pendingCallNumber = ""
        }

    val defaultRoleLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            val manager = context.getSystemService(
                RoleManager::class.java
            )
            Toast.makeText(
                context,
                if (manager.isRoleHeld(RoleManager.ROLE_DIALER))
                    "Default Phone enabled"
                else "Default Phone not selected",
                Toast.LENGTH_SHORT
            ).show()
        }

    fun requestDefaultPhone() {
        val manager = context.getSystemService(
            RoleManager::class.java
        )
        if (manager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
            !manager.isRoleHeld(RoleManager.ROLE_DIALER)
        ) {
            defaultRoleLauncher.launch(
                manager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            )
        } else {
            Toast.makeText(
                context,
                "Already set as Default Phone",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun requestCall(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "Enter a phone number", Toast.LENGTH_SHORT).show()
            return
        }

        pendingCallNumber = phoneNumber

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val telecom = context.getSystemService(
                Context.TELECOM_SERVICE
            ) as TelecomManager

            try {
                telecom.placeCall(
                    Uri.fromParts("tel", phoneNumber, null),
                    Bundle()
                )
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Unable to place call: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            pendingCallNumber = ""
        } else {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    val tabs = listOf(
        "Favorites", "Recents", "Contacts", "Keypad", "Voicemail"
    )

    val icons = listOf(
        Icons.Default.StarBorder,
        Icons.Default.History,
        Icons.Default.Person,
        Icons.Default.Dialpad,
        Icons.Default.Voicemail
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Blue, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Call,
                        null,
                        tint = Color.White
                    )
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    "iOS Phone",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(Modifier.weight(1f))

                TextButton(onClick = { requestDefaultPhone() }) {
                    Text("Default", color = Blue)
                }
            }

            Text(
                tab,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier.padding(
                    top = 16.dp,
                    bottom = 18.dp
                )
            )

            when (tab) {
                "Keypad" -> {
                    KeypadScreen(
                        number = number,
                        onNumber = { number += it },
                        onDelete = {
                            if (number.isNotEmpty()) {
                                number = number.dropLast(1)
                            }
                        },
                        onCall = { requestCall(number) }
                    )
                }

                "Contacts" -> EmptyScreen(
                    Icons.Default.Person,
                    "Contacts",
                    "Your contacts will appear here"
                )

                "Recents" -> EmptyScreen(
                    Icons.Default.History,
                    "No Recent Calls",
                    "Your call history will appear here"
                )

                "Favorites" -> EmptyScreen(
                    Icons.Default.StarBorder,
                    "Favorites",
                    "Your favorite contacts will appear here"
                )

                else -> EmptyScreen(
                    Icons.Default.Voicemail,
                    "Voicemail",
                    "Operator voicemail support required"
                )
            }

            Spacer(Modifier.weight(1f))

            HorizontalDivider(color = Color(0xFFE7E8EC))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { tab = item },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            icons[index],
                            contentDescription = item,
                            tint = if (tab == item) Blue else Grey
                        )

                        Text(
                            item,
                            fontSize = 9.sp,
                            color = if (tab == item) Blue else Grey
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadScreen(
    number: String,
    onNumber: (String) -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(Pale, CircleShape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                number.ifEmpty { "Enter Number" },
                fontSize = 23.sp,
                color = if (number.isEmpty()) Grey else Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            if (number.isNotEmpty()) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Backspace, "Delete")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        val keys = listOf(
            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("*" to "", "0" to "+", "#" to "")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(Pale, CircleShape)
                                .clickable { onNumber(digit) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    digit,
                                    fontSize = 29.sp,
                                    color = Ink
                                )
                                Text(
                                    letters,
                                    fontSize = 9.sp,
                                    color = Ink
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Blue, CircleShape)
                    .clickable { onCall() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Call,
                    "Call",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyScreen(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            null,
            tint = Grey,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )

        Text(
            subtitle,
            fontSize = 13.sp,
            color = Grey
        )
    }
}
