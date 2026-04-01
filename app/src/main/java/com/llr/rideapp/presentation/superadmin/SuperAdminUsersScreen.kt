package com.llr.rideapp.presentation.superadmin

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.domain.model.User
import com.llr.rideapp.domain.repository.UserRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuperAdminUsersViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var users by mutableStateOf<List<User>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var searchQuery by mutableStateOf("")

    init {
        log.debug("[SuperAdminUsersScreen] --init")
        loadUsers()
    }

    fun loadUsers() {
        log.debug("[SuperAdminUsersScreen] --loadUsers")
        isLoading = true
        error = null
        viewModelScope.launch {
            val result = userRepository.getAllUsers(searchQuery.ifBlank { null })
            isLoading = false
            result.fold(
                onSuccess = { users = it },
                onFailure = { error = it.message ?: "Erreur chargement des utilisateurs" }
            )
        }
    }

    fun toggleUserStatus(userId: String, currentStatus: Boolean) {
        log.debug("[SuperAdminUsersScreen] --toggleUserStatus")
        viewModelScope.launch {
            val result = userRepository.updateUserStatus(userId, !currentStatus)
            if (result.isSuccess) {
                loadUsers()
            } else {
                error = result.exceptionOrNull()?.message ?: "Erreur changement statut"
            }
        }
    }
}

@Composable
fun SuperAdminUsersScreen(
    viewModel: SuperAdminUsersViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Gestion Utilisateurs",
                onBack = onBack
            )

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Search bar
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RideAppTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.searchQuery = it },
                        label = "Rechercher...",
                        leadingIcon = Icons.Filled.Search,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RideAppButton(
                        text = "OK",
                        onClick = { viewModel.loadUsers() },
                        modifier = Modifier.width(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (viewModel.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGold)
                    }
                } else if (viewModel.error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorText(message = viewModel.error!!)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(viewModel.users) { user ->
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${user.firstname} ${user.lastname}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = TextPrimary
                                        )
                                        Text(text = user.email, color = TextSecondary)
                                        Text(text = "Rôles: ${user.roles.joinToString()}", color = AccentGold, fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = user.enabled,
                                        onCheckedChange = { viewModel.toggleUserStatus(user.id, user.enabled) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PrimaryBlack,
                                            checkedTrackColor = AccentGold,
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = PrimaryDark
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
