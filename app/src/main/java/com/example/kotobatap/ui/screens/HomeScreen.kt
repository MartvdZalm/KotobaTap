package com.example.kotobatap.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.example.kotobatap.ui.components.AppHeader
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavHostController,
    onNavigateToReader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("Dictionary") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("dictionary")
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("settings")
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppHeader(
                    title = "KotobaTap",
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                AboutSection()
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Article URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val formattedUrl = if (!url.startsWith("http")) {
                            "https://$url"
                        } else {
                            url
                        }
                        onNavigateToReader(Uri.encode(formattedUrl))
                    },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Read Article")
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()

                QuickAccessPanel(onNavigate = onNavigateToReader)
            }
        }
    }
}

@Composable
fun QuickAccessPanel(onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val quickLinks = listOf(
            "NHK Easy News" to "https://www3.nhk.or.jp/news/easy/",
            "NHK News" to "https://www3.nhk.or.jp/news/"
        )

        quickLinks.forEach { (name, url) ->
            Button(
                onClick = { onNavigate(url) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(name)
            }
        }
    }
}

@Composable
fun AboutSection() {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("This app is 100% free and open-source", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val repoUrl = "https://github.com/MartvdZalm/KotobaTap"
                val intent = Intent(Intent.ACTION_VIEW, repoUrl.toUri())
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text("View on GitHub")
        }
    }
}