package com.freeapp.founderrooms.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freeapp.founderrooms.core.error.ErrorState
import com.freeapp.founderrooms.core.ui.toDisplayMessage
import com.freeapp.founderrooms.designsystem.FounderColors
import com.freeapp.founderrooms.designsystem.FounderFontFamily
import org.koin.compose.viewmodel.koinViewModel

private val ErrorRed = Color(0xFFC62828)

/**
 * `design/mockups/05-auth.html`, ported 1:1: brand block, the two fields, the
 * dark "Continue" pill, the mode switch line, the footer tagline. Note in
 * `design/APPROVED.md`: only 00-04 are sha256-locked there, 05 is not, but it
 * is still treated as the visual contract per the task brief.
 *
 * Navigation is not owned here: [onAuthenticated] fires once, after a
 * successful sign-up or log-in (see [AuthEffect.Authenticated]); the caller
 * decides where that goes.
 */
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AuthEffect.Authenticated -> currentOnAuthenticated()
            }
        }
    }

    ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = FounderFontFamily)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(FounderColors.background)
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "TrustMRR",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    color = FounderColors.ink,
                )
                Text(
                    text = "A public chat and private groups for founders with verified revenue.",
                    fontSize = 12.sp,
                    color = FounderColors.muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .padding(horizontal = 12.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AuthField(
                    label = "Email",
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    placeholder = "you@startup.com",
                    keyboardType = KeyboardType.Email,
                )
                AuthField(
                    label = "Password",
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    placeholder = "••••••••",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                )
            }

            authMessage(state)?.let { message ->
                Text(
                    text = message,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FounderColors.ink)
                    .clickable(enabled = !state.isLoading, onClick = viewModel::onContinue)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(
                        text = "Continue",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(top = 18.dp)
                    .clickable(onClick = viewModel::onToggleMode),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Only the LogIn-mode copy is drawn in the mockup; the SignUp
                // complement below is this file's own, symmetric wording.
                val (prompt, action) = when (state.mode) {
                    AuthMode.LogIn -> "New here? " to "Create an account"
                    AuthMode.SignUp -> "Already have an account? " to "Log in"
                }
                Text(text = prompt, fontSize = 12.sp, color = FounderColors.muted)
                Text(text = action, fontSize = 12.sp, color = FounderColors.blue, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "You start in 🌱 Public. Verify your revenue any time to unlock your tier's room.",
                fontSize = 10.5.sp,
                color = FounderColors.muted,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            )
        }
    }
}

/**
 * [ErrorState.Unauthorized] and a sign-up [ErrorState.Validation] (409, email
 * taken) get friendly, mode-aware copy here so the field-agnostic F1 rule
 * (design doc section 8: "wrong password, no hint which field") still holds.
 * Everything else falls back to [toDisplayMessage]; note that function's
 * `Validation`/`Unknown` branches render the underlying message verbatim,
 * which can be a raw Ktor exception (request line, URL, body) rather than
 * user-facing copy -- flagged in this build's delivery report, not silently
 * patched here since that shared function is not owned by this screen.
 * [AuthUiState.fieldHint] is checked first since it is a local nudge, not a
 * failure, and must never be replaced by a stale server error.
 */
private fun authMessage(state: AuthUiState): String? {
    state.fieldHint?.let { return it }
    val error = state.error ?: return null
    return when {
        error is ErrorState.Unauthorized -> "Incorrect email or password."
        error is ErrorState.Validation && state.mode == AuthMode.SignUp -> "That email may already be in use."
        else -> error.toDisplayMessage()
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FounderColors.card)
            .border(1.dp, FounderColors.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            color = FounderColors.muted,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = FounderColors.ink, fontFamily = FounderFontFamily),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(FounderColors.ink),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(text = placeholder, fontSize = 14.sp, color = FounderColors.muted)
                    }
                    innerTextField()
                }
            },
        )
    }
}
