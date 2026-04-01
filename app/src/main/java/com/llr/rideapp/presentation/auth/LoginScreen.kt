package com.llr.rideapp.presentation.auth

import com.llr.rideapp.utils.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.llr.rideapp.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.domain.model.UserRole
import com.llr.rideapp.domain.repository.AuthRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var usernameOrEmail by mutableStateOf("")
    var password by mutableStateOf("")

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val _loginSuccessEvent = mutableStateOf<String?>(null)
    val loginSuccessEvent: State<String?> = _loginSuccessEvent

    fun login() {
        log.debug("[LoginScreen] --login")
        if (usernameOrEmail.isBlank() || password.isBlank()) {
            error = "Veuillez remplir tous les champs"
            return
        }

        isLoading = true
        error = null
        viewModelScope.launch {
            val result = authRepository.login(usernameOrEmail, password)
            isLoading = false
            result.fold(
                onSuccess = { token ->
                    val role = UserRole.fromRoleNames(token.roles)
                    _loginSuccessEvent.value = role.name
                },
                onFailure = {
                    error = it.message ?: "Erreur de connexion"
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: (String) -> Unit, // role
    onNavigateToRegister: () -> Unit
) {
    val loginSuccess by viewModel.loginSuccessEvent

    LaunchedEffect(loginSuccess) {
        loginSuccess?.let { role ->
            onLoginSuccess(role)
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
             Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bienvenue",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connectez-vous pour commencer",
                color = TextSecondary,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(48.dp))

            viewModel.error?.let {
                ErrorText(it)
                Spacer(modifier = Modifier.height(16.dp))
            }

            RideAppTextField(
                value = viewModel.usernameOrEmail,
                onValueChange = { viewModel.usernameOrEmail = it },
                label = "Username ou Email",
                leadingIcon = Icons.Filled.Email
            )
            Spacer(modifier = Modifier.height(16.dp))
            RideAppTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = "Mot de passe",
                isPassword = true,
                leadingIcon = Icons.Filled.Lock
            )
            Spacer(modifier = Modifier.height(32.dp))

            RideAppButton(
                text = "Se Connecter",
                onClick = { viewModel.login() },
                modifier = Modifier.fillMaxWidth(),
                isLoading = viewModel.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Text(text = "Pas encore de compte ? ", color = TextSecondary)
                Text(
                    text = "S'inscrire",
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}
