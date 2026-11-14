package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AppUserEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** Entra en la app como médico (admin). */
fun AndroidComposeTestRule<*, *>.entrarComoMedico() = entrar("admin", Role.ADMIN)

/** Entra en la app como astronauta (por defecto Maya Okafor; "ane.etxeberria", "leo.martin"...). */
fun AndroidComposeTestRule<*, *>.entrarComoAstronauta(usuario: String = "maya.okafor") = entrar(usuario, Role.ASTRONAUT)

/**
 * Rellena el login y pulsa "Entrar". Si la base de datos está vacía (BaseDeDatosLimpia) crea antes
 * el usuario, con su nombre como contraseña; si ya existe, usa la contraseña guardada.
 */
private fun AndroidComposeTestRule<*, *>.entrar(usuario: String, rol: Role) {
    val container = ApplicationProvider.getApplicationContext<SpaceDriftersApp>().container
    // Cada test empieza con el astronauta por defecto (Maya); el login lo cambia si encuentra el suyo.
    container.astronautaActualId = 1L
    val dao = container.database.appUserDao()
    val contrasena = runBlocking {
        withContext(Dispatchers.IO) {
            dao.findByUsername(usuario)?.passwordHash ?: usuario.also {
                dao.insert(
                    AppUserEntity().apply {
                        username = usuario
                        passwordHash = usuario
                        role = rol.name
                        createdAt = System.currentTimeMillis()
                    },
                )
            }
        }
    }
    val entrar = activity.getString(R.string.login_entrar)
    onNodeWithText(activity.getString(R.string.login_usuario)).performTextInput(usuario)
    onNodeWithText(activity.getString(R.string.login_contrasena)).performTextInput(contrasena)
    onNodeWithText(entrar).performClick()
    waitUntil(5_000) { onAllNodes(hasText(entrar)).fetchSemanticsNodes().isEmpty() }
}
