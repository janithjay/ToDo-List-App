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

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthenticationState.Authenticated) {
            onAuthSuccess()
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
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (authState is AuthenticationState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (isSignUp) {
                        viewModel.signUp(email, password)
                    } else {
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
            onClick = { isSignUp = !isSignUp }
        ) {
            Text(
                if (isSignUp) "Already have an account? Sign In"
                else "Don't have an account? Sign Up"
            )
        }

        if (authState is AuthenticationState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (authState as AuthenticationState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
