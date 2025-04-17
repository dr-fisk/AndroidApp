package com.example.syncup

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject

@Composable
fun confPasswordBox(clientState: MutableLiveData<ClientState>, str : String, confPasswordText: MutableState<String>)
{
    var cursColor = Color.Unspecified

    if (clientState.value!!.isKeyboardOpen())
    {
        cursColor = Color.Gray;
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
            value = confPasswordText.value,
            onValueChange = {
                if (it.length <= MAX_INPUT_SIZE)
                {
                    confPasswordText.value = it
                }
            },
            label = { Text(str) },
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

fun createAccountHandler(clientState: MutableLiveData<ClientState>,
                         username : String,
                         passwordText : String,
                         confPasswordText : String) : Int
{
    if (username.isEmpty())
    {
        return -4
    }
    if ((passwordText != confPasswordText)
        || (passwordText.isEmpty() && confPasswordText.isEmpty()))
    {
        return -3
    }

    sendAccountCreationMsg(clientState, username, passwordText)

    var resp = recv(clientState)
    if (0 == resp.length())
    {
        return -1
    }

    if(ErrorCodes.SUCCESS.code != resp.getInt("Result"))
    {
        return -2
    }

    return 0
}

@Composable
fun createAccScreen(clientState: MutableLiveData<ClientState>)
{
    var passwordErr = ""
    val confirmPasswordString = "Confirm Password:"
    var errorCode = remember { mutableStateOf(0) }
    var confPassLastSize = remember { mutableStateOf(0) }
    var passLastSize = remember{ mutableStateOf(0) }
    var errorMsg = ""
    var emailLastSize = remember{ mutableStateOf(0) }
    var passwordText = remember{ mutableStateOf("")}
    var confPasswordText = remember{ mutableStateOf("")}
    var usernameText = remember{ mutableStateOf("")}

    // create functions for below???

    when(errorCode.value)
    {
        -1 -> errorMsg = "An error occurred communicating with server"
        -2 -> errorMsg = "Account with that username already exists"
        -3 -> {
            if ((confPassLastSize.value != confPasswordText.value.length) ||
                (passLastSize.value != passwordText.value.length))
            {
                passwordErr = ""
                errorCode.value = 0
            }
            else if (passwordText.value.isEmpty() && confPasswordText.value.isEmpty())
            {
                passwordErr = "Please enter a password"
            }
            else
            {
                passwordErr = "Entered passwords do not match"
            }
        }
        -4 -> errorMsg = "Please enter a username"
    }

    passLastSize.value = passwordText.value.length
    confPassLastSize.value = confPasswordText.value.length
    emailLastSize.value = usernameText.value.length

    Surface(modifier = Modifier
        .fillMaxSize()
        .clickable(
            onClick = { clientState.value!!.closeKeyboard() },
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        )
    )
    {
        Column(  modifier = Modifier
            .verticalScroll(
                rememberScrollState()
            )
            .height(IntrinsicSize.Max)
            .width(IntrinsicSize.Max)
            .background(color = Color.Black))
        {
            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.2f))
            {
                Text(text = errorMsg,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 15.dp),
                    color = Color.Red)
            }

            usernameBox(clientState, usernameText);

            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.1f))
            {
                Text(text = passwordErr, color = Color.Red,
                    modifier = Modifier
                        .padding(horizontal = 15.dp)
                        .align(Alignment.BottomStart))
            }

            passwordBox(clientState, "Password:", passwordText)

            Spacer(modifier = Modifier
                .height(15.dp)
                .fillMaxWidth())

            confPasswordBox(clientState, confirmPasswordString, confPasswordText)

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(color = Color.Black)
            )

            Box(modifier = Modifier
                .width(100.dp)
                .height(56.dp)
                .align(Alignment.CenterHorizontally)
                .background(color = Color(50, 32, 122))
                .clickable(
                    onClick = {
                        errorCode.value = createAccountHandler(clientState, usernameText.value,
                            passwordText.value,
                            confPasswordText.value)

                        if(0 == errorCode.value)
                        {
                            clientState.value!!.transitionAppState(ClientAppState.ACC_CREATED)
                        }
                    }
                )
            )
            {
                Text(
                    text = "Create Account", color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 1.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(color = Color.Black)
            )

            Box(modifier = Modifier
                .width(100.dp)
                .height(56.dp)
                .align(Alignment.CenterHorizontally)
                .background(color = Color(50, 32, 122))
                .clickable
                    (
                    onClick = {
                        clientState.value!!.transitionAppState(ClientAppState.LOGGED_OUT) },
                )
            )
            {
                Text(
                    text = "Back", color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 1.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}