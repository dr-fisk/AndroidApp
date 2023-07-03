package com.example.helloworld

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UiState : ViewModel()
{
    var messageText by mutableStateOf("")
    var messageScrollState by mutableStateOf(0)
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
                .padding(bottom = 10.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
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

    @Composable
    fun TextBox(context : Context)
    {
        var width = 0.85f
        var height = 50.dp

        if (mUiState.value!!.messageText.isNotEmpty())
        {
            width = 0.85f
            height = 100.dp
        }

        TextField(value = mUiState.value!!.messageText,
                  onValueChange = { mUiState.value!!.messageText = it },
                  placeholder = { Text("Text Message")}, shape = CircleShape,
                  colors = TextFieldDefaults.textFieldColors(
                      containerColor = Color(10,10,10),
                      textColor = Color.White,
                      cursorColor = Color.Gray,
                      placeholderColor = Color.Gray,
                      focusedIndicatorColor = Color.Transparent,
                      unfocusedIndicatorColor = Color.Transparent,
                      disabledIndicatorColor = Color.Transparent),
                  modifier = Modifier
                      .fillMaxWidth(width)
                      .height(height),
                  singleLine = false,
                  maxLines = 3)
    }

    private fun temp()
    {
    }
    @Composable
    private fun SendButton()
    {
        if (mUiState.value!!.messageText.isNotEmpty())
        {
            Box(
                Modifier
                    .padding(vertical = 15.dp, horizontal = 5.dp)
                    .width(50.dp)
                    .height(50.dp)
                    .background(Color(10, 10, 10), CircleShape)
                    .clickable(
                        onClick = { temp() },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ),
                contentAlignment = Alignment.Center
            )
            {
                Text(text = "Send", color = Color.White, textAlign = TextAlign.Center,
                     modifier = Modifier.padding(horizontal = 1.dp)
                     .wrapContentHeight())
            }
        }
    }

    @Composable
    private fun StandardUi(context: Context)
    {
        if (mUiState.value!!.messageText.isEmpty())
        {
            Box(
                Modifier
                    .offset(x = -10.dp)
                    .width(40.dp)
                    .height(50.dp)
                    .background(Color(10, 10, 10), CircleShape),
                contentAlignment = Alignment.Center
            )
            {
                Text(text = "Box", color = Color.White, textAlign = TextAlign.Center,
                     modifier = Modifier.padding(horizontal = 1.dp)
                     .wrapContentHeight())
            }
        }
        TextBox(context)
        if (mUiState.value!!.messageText.isEmpty())
        {
            Box(
                Modifier
                    .offset(x = 10.dp)
                    .width(40.dp)
                    .height(50.dp)
                    .background(Color(10, 10, 10), CircleShape),
                    contentAlignment = Alignment.Center
            )
            {
                Text(text = "Misc", color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 1.dp)
                    .wrapContentHeight())
            }
        }
    }

    @Composable
    fun MainChat(context: Context) {
        Surface(modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)
            .padding(10.dp))
        {
            Column(
                modifier = Modifier
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .height(IntrinsicSize.Max)
                    .width(IntrinsicSize.Max)
                    .background(color = Color.Black)
                    .padding(16.dp)
            )
            {
                ChatScreen(context)
                Row(
                    Modifier
                        .height(IntrinsicSize.Max)
                        .width(IntrinsicSize.Max)
                )
                {
                    StandardUi(context)
                    SendButton()
                }
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
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        val context: Context = this
        setContent {
            MainChat(context)
        }
        ScrollView(this)
    }
}