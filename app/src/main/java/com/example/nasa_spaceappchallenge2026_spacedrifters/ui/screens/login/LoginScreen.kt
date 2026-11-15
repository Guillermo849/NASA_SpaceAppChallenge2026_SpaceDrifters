package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.ejemplo.CargadorDatosEjemplo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.AvisoPrototipo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/**
 * Pantalla de inicio de sesión. Redirige a la pantalla del astronauta o del admin
 * según el rol almacenado en la base de datos.
 */
@Composable
fun LoginScreen(
    onAstronauta: () -> Unit,
    onAdmin: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Efecto de un solo uso: navegar cuando el login es correcto.
    LaunchedEffect(estado.navegarA) {
        when (estado.navegarA) {
            DestinoLogin.ASTRONAUTA -> { onAstronauta(); viewModel.navegacionCompletada() }
            DestinoLogin.ADMIN      -> { onAdmin();      viewModel.navegacionCompletada() }
            null                    -> Unit
        }
    }

    LoginContent(
        usuario = estado.usuario,
        contrasena = estado.contrasena,
        cargando = estado.cargando,
        error = estado.error,
        onUsuarioCambiado = viewModel::onUsuarioCambiado,
        onContrasenaCambiada = viewModel::onContrasenaCambiada,
        onIniciarSesion = viewModel::iniciarSesion,
    )
}

@Composable
private fun LoginContent(
    usuario: String,
    contrasena: String,
    cargando: Boolean,
    @StringRes error: Int?,
    onUsuarioCambiado: (String) -> Unit,
    onContrasenaCambiada: (String) -> Unit,
    onIniciarSesion: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var mostrarContrasena by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        bottomBar = { AvisoPrototipo() },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                // En tablet, formulario centrado y estrecho en vez de a todo el ancho.
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = stringResource(R.string.cd_logo),
                    modifier = Modifier.size(84.dp),
                )
            }

            Text(
                text = stringResource(R.string.nombre_app),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.login_subtitulo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            // ── Campo usuario ─────────────────────────────────────────────────
            OutlinedTextField(
                value = usuario,
                onValueChange = onUsuarioCambiado,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.login_usuario)) },
                singleLine = true,
                isError = error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )

            // ── Campo contraseña ──────────────────────────────────────────────
            OutlinedTextField(
                value = contrasena,
                onValueChange = onContrasenaCambiada,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.login_contrasena)) },
                singleLine = true,
                isError = error != null,
                visualTransformation = if (mostrarContrasena) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus(); onIniciarSesion() },
                ),
                trailingIcon = {
                    IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                        Icon(
                            painter = painterResource(
                                if (mostrarContrasena) R.drawable.ic_ocultar_contrasena
                                else R.drawable.ic_ver_contrasena,
                            ),
                            contentDescription = stringResource(
                                if (mostrarContrasena) R.string.cd_ocultar_contrasena
                                                 else R.string.cd_mostrar_contrasena,
                            ),
                        )
                    }
                },
            )

            // ── Mensaje de error ──────────────────────────────────────────────
            if (error != null) {
                Text(
                    text = stringResource(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Botón ─────────────────────────────────────────────────────────
            Button(
                onClick = { focusManager.clearFocus(); onIniciarSesion() },
                enabled = !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.login_entrar), style = MaterialTheme.typography.labelLarge)
                }
            }

            // ── Cuentas de demostración (para la presentación) ────────────────
            Spacer(Modifier.height(8.dp))
            CuentasDemo(
                onElegir = { cuenta ->
                    onUsuarioCambiado(cuenta.usuario)
                    onContrasenaCambiada(cuenta.contrasena)
                },
            )
        }
    }
}

/** Una cuenta de los datos de ejemplo. */
private class CuentaDemo(@StringRes val rol: Int, val nombre: String?, val usuario: String, val contrasena: String)

/** Las cuentas que crean los datos de ejemplo (CargadorDatosEjemplo): el médico y un usuario por astronauta. */
private val CUENTAS_DEMO = listOf(
    CuentaDemo(R.string.login_demo_medico, null, CargadorDatosEjemplo.USUARIO_ADMIN, CargadorDatosEjemplo.CONTRASENA_ADMIN),
    CuentaDemo(R.string.login_demo_astronauta, "Maya Okafor", "maya.okafor", "maya.okafor"),
    CuentaDemo(R.string.login_demo_astronauta, "Leo Martín", "leo.martin", "leo.martin"),
    CuentaDemo(R.string.login_demo_astronauta, "Ane Etxeberria", "ane.etxeberria", "ane.etxeberria"),
    CuentaDemo(R.string.login_demo_astronauta, "Ilan Novak", "ilan.novak", "ilan.novak"),
)

/**
 * Recuadro informativo con las credenciales de la demo, para quien vea la app en la presentación.
 * Pulsar una cuenta rellena el formulario.
 */
@Composable
private fun CuentasDemo(onElegir: (CuentaDemo) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.login_demo_titulo), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.login_demo_explicacion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CUENTAS_DEMO.forEach { c ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onElegir(c) }
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    listOfNotNull(stringResource(c.rol), c.nombre).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.login_demo_credenciales, c.usuario, c.contrasena),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    SpaceDriftersTheme {
        LoginContent(
            usuario = "",
            contrasena = "",
            cargando = false,
            error = null,
            onUsuarioCambiado = {},
            onContrasenaCambiada = {},
            onIniciarSesion = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginErrorPreview() {
    SpaceDriftersTheme {
        LoginContent(
            usuario = "maya.okafor",
            contrasena = "wrong",
            cargando = false,
            error = R.string.login_error_credenciales,
            onUsuarioCambiado = {},
            onContrasenaCambiada = {},
            onIniciarSesion = {},
        )
    }
}
