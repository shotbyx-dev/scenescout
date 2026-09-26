package com.scenescout.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.scenescout.app.BuildConfig
import com.scenescout.app.R
import com.scenescout.app.data.places.ApiKeyStore

/**
 * About / credits screen, plus the one-time Google API key setup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    apiKeyStore: ApiKeyStore,
    hasBuildKey: Boolean,
    onKeyChanged: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.shotbyx_logo),
                contentDescription = "Shotbyx logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            Text(
                "SceneScout",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Find cinematic filming locations anywhere — from golden-hour " +
                    "beaches to abandoned stadiums and roadside relics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            Text("Created by Shotbyx", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            ApiKeySection(apiKeyStore, hasBuildKey, onKeyChanged)
            HorizontalDivider()
            Text(
                "Data & imagery credits",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "Places, photos, and map © Google. Place photos show their " +
                    "author credit. AI analysis never runs on Google imagery — " +
                    "only on Mapillary photos and your own uploads.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One-time Google API key setup. The key needs "Maps SDK for Android" and
 * "Places API (New)" enabled on a billing-enabled Google Cloud project.
 * Stored on-device only; a saved key overrides the build-time key.
 */
@Composable
private fun ApiKeySection(
    apiKeyStore: ApiKeyStore,
    hasBuildKey: Boolean,
    onKeyChanged: () -> Unit,
) {
    var draft by remember { mutableStateOf(apiKeyStore.getKey().orEmpty()) }
    var saved by remember { mutableStateOf(apiKeyStore.hasKey()) }
    var justSaved by remember { mutableStateOf(false) }

    Text("Google API key", style = MaterialTheme.typography.titleSmall)
    Text(
        when {
            saved -> "Key saved on this device — map, live discovery, and " +
                "photos are unlocked."
            hasBuildKey -> "A key is bundled with this build. You can paste " +
                "your own here to override it."
            else -> "Paste your Google API key to unlock the map, live " +
                "discovery, and real photos. Needs Maps SDK for Android + " +
                "Places API (New) enabled with billing on your Google Cloud " +
                "project — Google includes \$200 of free credit every month, " +
                "which covers normal personal use."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it; justSaved = false },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("API key") },
        singleLine = true,
    )
    Spacer(Modifier.height(8.dp))
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                apiKeyStore.setKey(draft)
                saved = true
                justSaved = true
                onKeyChanged()
            },
            enabled = draft.isNotBlank(),
        ) {
            Text(if (justSaved) "Saved ✓" else "Save key")
        }
        if (saved) {
            TextButton(
                onClick = {
                    apiKeyStore.clearKey()
                    draft = ""
                    saved = false
                    justSaved = false
                    onKeyChanged()
                },
            ) {
                Text("Remove")
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}
