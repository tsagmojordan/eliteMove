package com.llr.rideapp.presentation.auth

import com.llr.rideapp.utils.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.llr.rideapp.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.domain.repository.AuthRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var firstname by mutableStateOf("")
    var lastname by mutableStateOf("")
    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val _registerSuccessEvent = mutableStateOf(false)
    val registerSuccessEvent: State<Boolean> = _registerSuccessEvent

    fun register() {
        log.debug("[RegisterScreen] --register")
        if (firstname.isBlank() || lastname.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            error = "Veuillez remplir tous les champs"
            return
        }

        isLoading = true
        error = null
        viewModelScope.launch {
            val result = authRepository.register(firstname, lastname, username, email, password)
            isLoading = false
            result.fold(
                onSuccess = {
                    _registerSuccessEvent.value = true
                },
                onFailure = {
                    error = it.message ?: "Erreur d'inscription"
                }
            )
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    if (viewModel.registerSuccessEvent.value) {
        LaunchedEffect(Unit) {
            onRegisterSuccess()
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))
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
                text = "Inscription",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Créez votre compte client",
                color = TextSecondary,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            viewModel.error?.let {
                ErrorText(it)
                Spacer(modifier = Modifier.height(16.dp))
            }

            RideAppTextField(
                value = viewModel.firstname,
                onValueChange = { viewModel.firstname = it },
                label = "Prénom",
                leadingIcon = Icons.Filled.Person
            )
            Spacer(modifier = Modifier.height(16.dp))
            RideAppTextField(
                value = viewModel.lastname,
                onValueChange = { viewModel.lastname = it },
                label = "Nom",
                leadingIcon = Icons.Filled.Person
            )
            Spacer(modifier = Modifier.height(16.dp))
            RideAppTextField(
                value = viewModel.username,
                onValueChange = { viewModel.username = it },
                label = "Nom d'utilisateur",
                leadingIcon = Icons.Filled.Person
            )
            Spacer(modifier = Modifier.height(16.dp))
            RideAppTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = "Email",
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
                text = "S'inscrire",
                onClick = { viewModel.register() },
                modifier = Modifier.fillMaxWidth(),
                isLoading = viewModel.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(text = "Déjà un compte ? ", color = TextSecondary)
                Text(
                    text = "Se connecter",
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}
