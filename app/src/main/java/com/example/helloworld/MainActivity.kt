package com.example.helloworld

import android.app.PendingIntent.getActivity
import android.content.Context
import android.os.Bundle
import android.view.View
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.helloworld.ui.theme.HelloWorldTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class UiState : ViewModel()
{
    var openTextBox by mutableStateOf(false)
}

class MainActivity : ComponentActivity()
{
    companion object {
      init {
         System.loadLibrary("helloworld")
      }
    }

    private val _UiState : MutableLiveData<UiState> = MutableLiveData(UiState())

    external fun getString(str: String): String

    @Composable
    fun ChatScreen()
    {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .background(Color.Black)
                .clickable(
                    onClick = { _UiState.value!!.openTextBox = false },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
        ) {
            Text(text = "Chat Screen", color = Color.White)
        }
    }

    @Composable
    fun TextBox()
    {
        var textStr = "Text Box"

        if(_UiState.value!!.openTextBox)
        {
            textStr= "Text Box Opened"
        }

        Box(
            Modifier
                .fillMaxWidth(0.80f)
                .fillMaxHeight()
                .background(Color(10, 10, 10))
                .clickable(onClick = { _UiState.value!!.openTextBox = true })
        ) {
            Text(text = textStr, color = Color.White)
        }
    }

    @Composable
    fun MainChat(context: Context, view : View) {
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
                ChatScreen()
                Row {
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
                    TextBox()
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
                keyboard(context, window.decorView)
            }
        }
    }

    fun keyboard(context: Context, view : View)
    {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if(_UiState.value!!.openTextBox)
        {
            imm.showSoftInput(view, 0);
        }
        else
        {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context: Context = this
        setContent {
            MainChat(context, window.decorView)
        }
    }
}