package com.example.syncup

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData

val MAX_INPUT_SIZE = 32

@Composable
fun passwordBox(clientState: MutableLiveData<ClientState>, passwordStr : String, password : MutableState<String>)
{
    var cursColor = Color.Unspecified

    if (clientState.value!!.isKeyboardOpen())
    {
        cursColor = Color.Gray
    }

    Box(modifier = Modifier
        .height(56.dp)
        .fillMaxWidth()
        .clickable(
            onClick = { clientState.value!!.closeKeyboard() },
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ))
    {
        TextField(
            value = password.value,
            onValueChange = {
                if (it.length <= MAX_INPUT_SIZE)
                {
                    password.value = it
                }
            },
            label = { Text(passwordStr) },
            placeholder = { Text("") }, shape = RectangleShape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(10, 10, 10),
                textColor = Color.White,
                cursorColor = cursColor,
                placeholderColor = Color.Gray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .padding(horizontal = 15.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            interactionSource = remember { MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect {
                            if (it is PressInteraction.Release) {
                                clientState.value!!.setKeyboardOpen()
                            }
                        }
                    }
                },
            singleLine = true
        )
    }
}

@Composable
fun usernameBox(clientState: MutableLiveData<ClientState>, username : MutableState<String>)
{
    var cursColor = Color.Unspecified

    if (clientState.value!!.isKeyboardOpen())
    {
        cursColor = Color.Gray
    }
    TextField(
        value = username.value,
        onValueChange = {
            if (it.length <= MAX_INPUT_SIZE)
            {
                username.value = it
            }
        },
        label = { Text("Username:") },
        placeholder = { Text("") }, shape = RectangleShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color(10, 10, 10),
            textColor = Color.White,
            cursorColor = cursColor,
            placeholderColor = Color.Gray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .padding(horizontal = 15.dp)
            .fillMaxWidth()
            .height(56.dp),
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            clientState.value!!.setKeyboardOpen()
                        }
                    }
                }
            },
        singleLine = true
    )
}