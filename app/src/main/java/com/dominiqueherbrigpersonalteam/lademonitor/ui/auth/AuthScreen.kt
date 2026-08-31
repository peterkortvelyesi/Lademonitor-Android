package com.dominiqueherbrigpersonalteam.lademonitor.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dominiqueherbrigpersonalteam.lademonitor.R
import com.dominiqueherbrigpersonalteam.lademonitor.data.remote.ApiException
import com.dominiqueherbrigpersonalteam.lademonitor.data.repo.SyncService
import com.dominiqueherbrigpersonalteam.lademonitor.data.session.SessionManager
import com.dominiqueherbrigpersonalteam.lademonitor.data.remote.ApiClient
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppMode
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppSettings
import kotlinx.coroutines.launch

/** Login/registration incl. server address. Port of the iOS `AuthView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen() {
    val scope = rememberCoroutineScope()
    val serverUrl by AppSettings.serverUrlString.collectAsStateWithLifecycle()

    var isRegistering by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val passwordsMismatchMessage = stringResource(R.string.auth_error_passwords_mismatch)
    val registerLabel = stringResource(R.string.auth_action_register)
    val loginLabel = stringResource(R.string.auth_action_login)

    val trimmedUsername = username.trim()
    val canSubmit = AppSettings.isConfigured &&
        trimmedUsername.length >= 3 &&
        password.length >= 8 &&
        (!isRegistering || password == passwordConfirm)

    fun submit() {
        errorMessage = null
        if (isRegistering && password != passwordConfirm) {
            errorMessage = passwordsMismatchMessage
            return
        }
        scope.launch {
            isSubmitting = true
            try {
                val response = if (isRegistering) ApiClient.register(trimmedUsername, password)
                else ApiClient.login(trimmedUsername, password)
                SessionManager.completeAuthentication(response)
                // Migrating local data (if any) is not special — it's just the first sync pass.
                scope.launch { SyncService.syncNow() }
            } catch (e: ApiException.Server) {
                errorMessage = e.serverMessage
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isSubmitting = false
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(stringResource(R.string.auth_server_address_label), style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { AppSettings.setServerUrlString(it) },
                placeholder = { Text(stringResource(R.string.auth_server_address_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.auth_server_address_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Text(
                if (isRegistering) registerLabel else loginLabel,
                style = MaterialTheme.typography.labelLarge
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.auth_username_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            if (isRegistering) {
                OutlinedTextField(
                    value = passwordConfirm,
                    onValueChange = { passwordConfirm = it },
                    label = { Text(stringResource(R.string.auth_password_confirm_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    stringResource(R.string.auth_validation_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }

            Button(
                onClick = { submit() },
                enabled = !isSubmitting && canSubmit,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isRegistering) registerLabel else loginLabel)
                }
            }

            TextButton(
                onClick = {
                    isRegistering = !isRegistering
                    errorMessage = null
                    passwordConfirm = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isRegistering) stringResource(R.string.auth_toggle_to_login)
                    else stringResource(R.string.auth_toggle_to_register)
                )
            }

            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = { AppSettings.setAppMode(AppMode.LOCAL_ONLY) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.auth_use_local_only)) }
        }
    }
}
