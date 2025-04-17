package com.example.syncup

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64

fun loginServerRequest(clientState: MutableLiveData<ClientState>, username : String, password : String) : Boolean
{
    var msg = JSONObject()
    var msgData = JSONObject()
    val md = MessageDigest.getInstance("SHA-256")

    msg.put("Event", EventType.LOGIN.ordinal)
    msgData.put("Username", username)
    val encryptedPass = md.digest(password.toByteArray(Charsets.UTF_8))
    msgData.put("Password", Base64.getEncoder().encodeToString(encryptedPass))
    msg.put("Data", msgData)

    val buffer = msg.toString().toByteArray(Charsets.UTF_8)
    return 0 < clientState.value!!.sendMsg(buffer, buffer.size)
}

fun loginHandler(clientState : MutableLiveData<ClientState>, username: String, password : String) : Int
{
    if (username.isEmpty() || password.isEmpty()) {
        return -2;
    }

    if (!loginServerRequest(clientState, username, password))
    {
        return -1
    }

    val resp = recv(clientState)

    if(0 == resp.length())
    {
        return -1
    }

    if(ErrorCodes.SUCCESS.code != resp.getInt("Result"))
    {
        return -3
    }

    clientState.value!!.transitionAppState(ClientAppState.LOGGED_IN)
    return 0
}


@Composable
fun loginScreen(clientState : MutableLiveData<ClientState>)
{
    val passwordString = "Password:"
    var interactionSource = remember { MutableInteractionSource() }
    var usernameText = remember{ mutableStateOf("") }
    var passwordText = remember{ mutableStateOf("") }

    Surface(modifier = Modifier
        .fillMaxSize()
        .clickable(
            onClick = { clientState.value!!.closeKeyboard() },
            interactionSource = interactionSource,
            indication = null
        ))
    {
        Column(  modifier = Modifier
            .verticalScroll(
                rememberScrollState()
            )
            .height(IntrinsicSize.Max)
            .width(IntrinsicSize.Max)
            .background(color = Color.Black))
        {
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.2f))

            usernameBox(clientState, usernameText);

            Spacer(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.1f)
            )

            passwordBox(clientState, passwordString, passwordText)

            Spacer(modifier = Modifier
                .height(10.dp)
                .fillMaxWidth())

            Box(modifier = Modifier
                .width(100.dp)
                .height(56.dp)
                .align(Alignment.CenterHorizontally)
                .background(color = Color(50, 32, 122))
                .clickable(
                    onClick = { loginHandler(clientState, usernameText.value, passwordText.value) }
                )
            )
            {
                Text(
                    text = "Login", color = Color.White, textAlign = TextAlign.Center,
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
                    onClick = { clientState.value!!.transitionAppState(ClientAppState.CREATE_ACC) },
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = Color.White)
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
        }
    }
}