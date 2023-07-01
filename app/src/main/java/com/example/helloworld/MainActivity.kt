package com.example.helloworld

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UiState : ViewModel()
{
    var messageText by mutableStateOf("")
}

class MainActivity : ComponentActivity()
{
    companion object {
      init {
         System.loadLibrary("helloworld")
      }
    }

    private val mUiState : MutableLiveData<UiState> = MutableLiveData(UiState())

    external fun getString(str: String): String

    @Composable
    fun ChatScreen(context : Context)
    {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .background(Color.Black)
                .clickable(
                    onClick = { closeKeyboard(context) },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
        ) {
            Text(text = "Chat Screen", color = Color.White)
        }
    }

    private fun isChar(keycode : Int) : Boolean
    {
        val minKeyCode = 32
        val maxKeyCode = 126
        val newLine = 10
        return (((minKeyCode <= keycode) && (maxKeyCode >= keycode)) || (newLine == keycode))
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null)
        {
            return false
        }

        if (event.action == KeyEvent.ACTION_UP)
        {

            if (isChar(event.getUnicodeChar(event.metaState)))
            {
                mUiState.value!!.messageText += event.getUnicodeChar(event.metaState).toChar()
            }
            else
            {
                // Delete key is pressed, remove last character if string is not empty
                if (event.keyCode == KeyEvent.KEYCODE_DEL &&
                    mUiState.value!!.messageText.isNotEmpty() )
                {
                    mUiState.value!!.messageText = mUiState.value!!.messageText.dropLast(1)
                }
            }
        }

        return super.onKeyUp(keyCode, event)
    }

    @Composable
    fun TextBox(context : Context)
    {
        var textStr = "Text Message"

        // Message has been typed so show it on the UI
        if (mUiState.value!!.messageText.isNotEmpty())
        {
            textStr = mUiState.value!!.messageText
        }

        Box(
            Modifier
                .fillMaxWidth(0.80f)
                .fillMaxHeight()
                .background(Color(10, 10, 10))
                .clickable(onClick = { openKeyboard(context) })
        ) {
            Text(text = textStr, color = Color.White)
        }
    }

    @Composable
    fun MainChat(context: Context) {
        Row ( horizontalArrangement = Arrangement.SpaceBetween ){
//        Column {
//            Box(
//                Modifier
//                    .fillMaxWidth(0.15f)
//                    .fillMaxHeight()
//                    .background(Color.DarkGray)
//            ){
//                Text(text = "List of chats")
//            }
//        }
            Column(verticalArrangement = Arrangement.Top) {
                ChatScreen(context)
                Row(Modifier .fillMaxHeight()) {
                    Box(
                        Modifier
                            .width(30.dp)
                            .fillMaxHeight()
                            .background(Color(10, 10, 10))
                    ) {
                        Text(text = "Box", color = Color.White)
                    }
                    Box(
                        Modifier
                            .width(10.dp)
                            .fillMaxHeight()
                            .background(Color.Black)
                    )
                    TextBox(context)
                    Box(
                        Modifier
                            .width(10.dp)
                            .fillMaxHeight()
                            .background(Color.Black)
                    )
                    Box(
                        Modifier
                            .width(30.dp)
                            .fillMaxHeight()
                            .background(Color(10, 10, 10))
                    ) {
                        Text(text = "Misc", color = Color.White)
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(Color.Black)
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(Color.Black)
                )
            }
        }
    }

    private fun openKeyboard(context: Context)
    {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(window.decorView, 0)
    }

    private fun closeKeyboard(context: Context)
    {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context: Context = this
        setContent {
            MainChat(context)
        }
    }
}