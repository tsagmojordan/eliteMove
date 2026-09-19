package com.llr.rideapp.presentation.superadmin

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.llr.rideapp.domain.repository.UserRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuperAdminRolesViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var roles by mutableStateOf<List<Role>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // Formulaire de création
    var newRoleName by mutableStateOf("")
    var newRoleDescription by mutableStateOf("")

    init {
        log.debug("[SuperAdminRolesScreen] --init")
        loadRoles()
    }

    fun loadRoles() {
        log.debug("[SuperAdminRolesScreen] --loadRoles")
        isLoading = true
        error = null
        viewModelScope.launch {
            val result = userRepository.getRoles(null)
            isLoading = false
            result.fold(
                onSuccess = { roles = it },
                onFailure = { error = it.message ?: "Erreur chargement des rôles" }
            )
        }
    }

    fun createRole() {
        log.debug("[SuperAdminRolesScreen] --createRole")
        if (newRoleName.isBlank()) {
            error = "Le nom du rôle est requis"
            return
        }
        viewModelScope.launch {
            val result = userRepository.createRole(
                name = newRoleName.uppercase().replace(' ', '_'),
                description = newRoleDescription.ifBlank { null } ?: ""
            )
            result.fold(
                onSuccess = {
                    newRoleName = ""
                    newRoleDescription = ""
                    loadRoles()
                },
                onFailure = { error = it.message ?: "Erreur création du rôle" }
            )
        }
    }

    fun deleteRole(roleId: String) {
        log.debug("[SuperAdminRolesScreen] --deleteRole")
        viewModelScope.launch {
            val result = userRepository.deleteRole(roleId)
            result.fold(
                onSuccess = { loadRoles() },
                onFailure = { error = it.message ?: "Erreur suppression du rôle" }
            )
        }
    }
}

@Composable
fun SuperAdminRolesScreen(
    viewModel: SuperAdminRolesViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var roleToDelete by remember { mutableStateOf<Role?>(null) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Gestion des Rôles",
                onBack = onBack
            )

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                // Formulaire de création (POST /api/v1/roles)
                Text("Créer un rôle", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                RideAppTextField(
                    value = viewModel.newRoleName,
                    onValueChange = { viewModel.newRoleName = it },
                    label = "Nom (ex: CHAUFFEUR)",
                    leadingIcon = Icons.Filled.Add
                )
                Spacer(modifier = Modifier.height(8.dp))
                RideAppTextField(
                    value = viewModel.newRoleDescription,
                    onValueChange = { viewModel.newRoleDescription = it },
                    label = "Description (optionnel)"
                )
                Spacer(modifier = Modifier.height(8.dp))
                RideAppButton(
                    text = "Créer",
                    onClick = { viewModel.createRole() },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                viewModel.error?.let {
                    ErrorText(it)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (viewModel.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGold)
                    }
                } else {
                    Text(
                        "Rôles existants (${viewModel.roles.size})",
                        color = AccentGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(viewModel.roles) { role ->
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(role.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        role.description?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, color = TextSecondary, fontSize = 13.sp)
                                        }
                                    }
                                    IconButton(onClick = { roleToDelete = role }) {
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

    // Confirmation de suppression
    roleToDelete?.let { role ->
        AlertDialog(
            onDismissRequest = { roleToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Supprimer le rôle ?", color = AccentGold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Le rôle ${role.name} sera supprimé. Les utilisateurs qui le possèdent le perdront.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRole(role.id)
                        roleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { roleToDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }
}