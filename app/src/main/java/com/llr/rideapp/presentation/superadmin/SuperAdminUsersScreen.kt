package com.llr.rideapp.presentation.superadmin

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.domain.model.Role
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

    // Catalogue des rôles disponibles (pour l'assignation)
    var availableRoles by mutableStateOf<List<Role>>(emptyList())
        private set

    init {
        log.debug("[SuperAdminUsersScreen] --init")
        loadUsers()
        loadRoles()
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

    fun loadRoles() {
        log.debug("[SuperAdminUsersScreen] --loadRoles")
        viewModelScope.launch {
            userRepository.getRoles(null).onSuccess { availableRoles = it }
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

    /** POST /api/v1/users/{id}/roles avec la liste complète des rôleIds cochés. */
    fun assignRoles(userId: String, roleIds: List<String>) {
        log.debug("[SuperAdminUsersScreen] --assignRoles")
        viewModelScope.launch {
            val result = userRepository.assignRoles(userId, roleIds)
            if (result.isSuccess) {
                loadUsers()
            } else {
                error = result.exceptionOrNull()?.message ?: "Erreur assignation des rôles"
            }
        }
    }

    fun deleteUser(userId: String) {
        log.debug("[SuperAdminUsersScreen] --deleteUser")
        viewModelScope.launch {
            val result = userRepository.deleteUser(userId)
            if (result.isSuccess) {
                loadUsers()
            } else {
                error = result.exceptionOrNull()?.message ?: "Erreur suppression de l'utilisateur"
            }
        }
    }
}

@Composable
fun SuperAdminUsersScreen(
    viewModel: SuperAdminUsersViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

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
                                    Column(modifier = Modifier.weight(1f)) {
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RideAppButton(
                                        text = "Rôles",
                                        onClick = { userToEdit = user },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        icon = Icons.Filled.ManageAccounts
                                    )
                                    IconButton(onClick = { userToDelete = user }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Supprimer",
                                            tint = ErrorRed
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

    // ─── Dialog d'assignation des rôles ───────────────────────────────────────
    userToEdit?.let { user ->
        AssignRolesDialog(
            user = user,
            availableRoles = viewModel.availableRoles,
            onDismiss = { userToEdit = null },
            onConfirm = { roleIds ->
                viewModel.assignRoles(user.id, roleIds)
                userToEdit = null
            }
        )
    }

    // ─── Confirmation de suppression ─────────────────────────────────────────
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Supprimer l'utilisateur ?", color = AccentGold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "${user.firstname} ${user.lastname} (${user.email}) sera définitivement supprimé.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(user.id)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Dialogue d'assignation des rôles : rôles actuels pré-cochés (par nom),
 * validation → POST /api/v1/users/{id}/roles.
 */
@Composable
fun AssignRolesDialog(
    user: User,
    availableRoles: List<Role>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    // Ids cochés : rôles existants de l'utilisateur matchés par nom dans le catalogue
    val initialSelection = availableRoles
        .filter { role -> user.roles.any { it.equals(role.name, ignoreCase = true) } }
        .map { it.id }
        .toSet()
    val selectedIds = remember(user.id) { mutableStateOf(initialSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text("Rôles de ${user.firstname}", color = AccentGold, fontWeight = FontWeight.Bold)
                Text(user.email, color = TextSecondary, fontSize = 13.sp)
            }
        },
        text = {
            Column {
                if (availableRoles.isEmpty()) {
                    Text(
                        "Aucun rôle disponible (chargez la liste ou créez des rôles).",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                availableRoles.forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = role.id in selectedIds.value,
                            onCheckedChange = { checked ->
                                selectedIds.value = if (checked) {
                                    selectedIds.value + role.id
                                } else {
                                    selectedIds.value - role.id
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = AccentGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(role.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            role.description?.let {
                                Text(it, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIds.value.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
            ) {
                Text("Enregistrer", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = TextSecondary)
            }
        }
    )
}