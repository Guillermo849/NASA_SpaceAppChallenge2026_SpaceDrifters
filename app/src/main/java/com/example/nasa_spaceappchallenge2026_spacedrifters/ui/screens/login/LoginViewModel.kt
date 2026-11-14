package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.AppUserDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.dao.AstronautDao
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estado de la pantalla de inicio de sesión. */
data class LoginUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    /** Mensaje de error a mostrar bajo el formulario; null si no hay error. */
    @StringRes val error: Int? = null,
    /** Destino al que navegar cuando el login es correcto. */
    val navegarA: DestinoLogin? = null,
)

enum class DestinoLogin { ASTRONAUTA, ADMIN }

class LoginViewModel(
    private val userDao: AppUserDao,
    private val astronautDao: AstronautDao? = null,
    /** Se llama con el astronaut.id de quien entra como astronauta (para que vea sus datos). */
    private val alEntrarAstronauta: (Long) -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsuarioCambiado(value: String) {
        _uiState.update { it.copy(usuario = value, error = null) }
    }

    fun onContrasenaCambiada(value: String) {
        _uiState.update { it.copy(contrasena = value, error = null) }
    }

    fun iniciarSesion() {
        val usuario = _uiState.value.usuario.trim()
        val contrasena = _uiState.value.contrasena

        if (usuario.isBlank() || contrasena.isBlank()) {
            _uiState.update { it.copy(error = R.string.login_error_vacio) }
            return
        }

        _uiState.update { it.copy(cargando = true, error = null) }

        viewModelScope.launch {
            val destino = withContext(Dispatchers.IO) {
                val entidad = userDao.findByUsername(usuario) ?: return@withContext null
                if (!entidad.active.asBoolean()) return@withContext null
                // Prototipo: comparación directa (en producción usar hash seguro).
                // passwordHash vacío = cuenta sin contraseña configurada → siempre rechazar.
                if (entidad.passwordHash.isNullOrBlank()) return@withContext null
                if (entidad.passwordHash != contrasena) return@withContext null
                when (entidad.role) {
                    Role.ASTRONAUT.name -> {
                        // Su astronaut (si existe): el panel, las pruebas y la alarma son suyos.
                        astronautDao?.findByUserId(entidad.id)?.let { alEntrarAstronauta(it.id) }
                        DestinoLogin.ASTRONAUTA
                    }
                    Role.ADMIN.name     -> DestinoLogin.ADMIN
                    else                -> null
                }
            }

            if (destino == null) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        error = R.string.login_error_credenciales,
                        contrasena = "",
                    )
                }
            } else {
                _uiState.update { it.copy(cargando = false, navegarA = destino) }
            }
        }
    }

    /** Se llama desde la pantalla cuando ya se navegó, para limpiar el evento de un solo uso. */
    fun navegacionCompletada() {
        _uiState.update { it.copy(navegarA = null) }
    }

    private fun Int.asBoolean() = this != 0
}
