package com.example.syncup

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.example.syncup.ClientState

@Composable
fun accountCreatedScreen(clientState: MutableLiveData<ClientState>)
{
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
                Text(text = "Account created!",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 15.dp),
                    color = Color.White)
            }

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
                    text = "Return to Login", color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 1.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}