package com.janithjayashan.todolistapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.janithjayashan.todolistapp.auth.AuthViewModel
import com.janithjayashan.todolistapp.auth.AuthenticationState
import com.janithjayashan.todolistapp.utils.FirebaseBackupManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthenticationState.Authenticated) {
            // Restore user data before navigating
            scope.launch {
                val backupManager = FirebaseBackupManager(viewModel.getContext())
                backupManager.restoreUserData()
                onAuthSuccess()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text("Email") },
            singleLine = true,
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        if (isSignUp) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordError = null
                },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = passwordError != null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (authState is AuthenticationState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    // Reset errors
                    emailError = null
                    passwordError = null

                    // Validate email
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailError = "Please enter a valid email address"
                        return@Button
                    }

                    if (isSignUp) {
                        // Additional validations only for signup
                        if (password.length < 6) {
                            passwordError = "Password must be at least 6 characters"
                            return@Button
                        }

                        // Check if passwords match
                        if (password != confirmPassword) {
                            passwordError = "Passwords do not match"
                            return@Button
                        }
                        viewModel.signUp(email, password)
                    } else {
                        // For sign in, just attempt to sign in
                        viewModel.signIn(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSignUp) "Sign Up" else "Sign In")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                isSignUp = !isSignUp
                emailError = null
                passwordError = null
                email = ""
                password = ""
                confirmPassword = ""
            }
        ) {
            Text(
                if (isSignUp) "Already have an account? Sign In"
                else "Don't have an account? Sign Up"
            )
        }

        when (val state = authState) {
            is AuthenticationState.Error -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!isSignUp) {
                        "Email or password is incorrect"
                    } else {
                        when {
                            state.message.contains("email") -> "Email already in use"
                            state.message.contains("password") -> "Password must be at least 6 characters"
                            else -> state.message
                        }
                    },
                    color = MaterialTheme.colorScheme.error
                )
            }
            is AuthenticationState.Authenticated -> {
                if (isSignUp) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Account created successfully!",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            else -> { /* do nothing */ }
        }
    }
}
