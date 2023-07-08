package com.example.helloworld

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

class ClientInfo
{
    var clientFd by mutableStateOf(-1)
    var clientLoggedIn by mutableStateOf(false)
}

class UiState : ViewModel()
{
    var messageText by mutableStateOf("")
    var passwordText by mutableStateOf("")
    var kbdOpen by mutableStateOf(false)
}

class MainActivity : ComponentActivity()
{
    companion object
    {
      init {
         System.loadLibrary("client")
      }
    }

    fun getClientFd() : Int
    {
        return mClientInfo.value!!.clientFd
    }

    fun setClientFd(fd : Int)
    {
        mClientInfo.value!!.clientFd = fd;
    }

    private val mUiState : MutableLiveData<UiState> = MutableLiveData(UiState())
    private val mClientInfo : MutableLiveData<ClientInfo> = MutableLiveData(ClientInfo())

    external fun connectToServer() : Int
    external fun close(fd : Int)
    external fun sendText(fd : Int, msg : String) : Int
    @Composable
    fun ChatScreen(context : Context)
    {
        var textStr = "Chat Screen"

        when(mClientInfo.value!!.clientFd)
        {
            -1 -> textStr = "Failed to connect to server"
            -2 -> textStr = "Lost connection to server"
        }

        Box(
            Modifier
                .padding(bottom = 10.dp, top = 10.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .background(Color.Black)
                .clickable(
                    onClick = { closeKeyboard(context) },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )

        ) {
            Text(text = textStr, color = Color.White)
        }
    }

    fun keyboardOpened()
    {
        mUiState.value!!.kbdOpen = true
    }
    @Composable
    fun TextBox()
    {
        var cursColor = Color.Unspecified

        if (mUiState.value!!.kbdOpen)
        {
            cursColor = Color.Gray;
        }

        var width = 0.80f
        var height = 56.dp

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
                      cursorColor = cursColor,
                      placeholderColor = Color.Gray,
                      focusedIndicatorColor = Color.Transparent,
                      unfocusedIndicatorColor = Color.Transparent,
                      disabledIndicatorColor = Color.Transparent),
                  modifier = Modifier
                      .fillMaxWidth(width)
                      .height(height),
                  interactionSource = remember { MutableInteractionSource() }
                      .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is PressInteraction.Release) {
                                    keyboardOpened()
                                }
                            }
                        }
                  },
                  singleLine = false,
                  maxLines = 3)
    }

    private fun sendMsg()
    {
        if (mUiState.value!!.messageText.isNotEmpty()) {
            if(0 > sendText(mClientInfo.value!!.clientFd, mUiState.value!!.messageText))
            {
               Log.e("sendMsg", "Msg failed to send")
            }
            mUiState.value!!.messageText = ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        close(mClientInfo.value!!.clientFd)
    }

    @Composable
    private fun StandardUi()
    {
        if (mUiState.value!!.messageText.isEmpty())
        {
            Box(
                Modifier
                    .offset(x = (-10).dp)
                    .width(56.dp)
                    .height(56.dp)
                    .background(Color(10, 10, 10), CircleShape),
                contentAlignment = Alignment.Center
            )
            {
                Text(text = "Box", color = Color.White, textAlign = TextAlign.Center,
                     modifier = Modifier
                         .padding(horizontal = 1.dp)
                         .wrapContentHeight())
            }
        }

        TextBox()

        var textStr = "Misc"

        if (mUiState.value!!.messageText.isNotEmpty())
        {
            textStr = "Send"
        }

        Box(
            Modifier
                .offset(x = 10.dp)
                .width(56.dp)
                .height(56.dp)
                .background(Color(10, 10, 10), CircleShape)
                .clickable(
                    onClick = { sendMsg() },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ),
                contentAlignment = Alignment.Center
        )
        {
            Text(text = textStr, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .wrapContentHeight())
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
                    StandardUi()
                }
            }
        }
    }

    @Composable
    private fun loginScreen(context : Context)
    {
        var cursColor = Color.Unspecified

        if (mUiState.value!!.kbdOpen)
        {
            cursColor = Color.Gray;
        }

        Surface(modifier = Modifier
            .fillMaxSize()
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
                Box( modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.2f)
                    .clickable(
                        onClick = { closeKeyboard(context) },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ))

                TextField(
                    value = mUiState.value!!.messageText,
                    onValueChange = { mUiState.value!!.messageText = it },
                    label = { Text("Email:") },
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
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    interactionSource = remember { MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect {
                                    if (it is PressInteraction.Release) {
                                        keyboardOpened()
                                    }
                                }
                            }
                        },
                    singleLine = true
                )
                Box( modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.1f)
                    .clickable(
                        onClick = { closeKeyboard(context) },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ))

                    TextField(
                        value = mUiState.value!!.passwordText,
                        onValueChange = { mUiState.value!!.passwordText = it },
                        label = { Text("Password:") },
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
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(56.dp),
                        interactionSource = remember { MutableInteractionSource() }
                            .also { interactionSource ->
                                LaunchedEffect(interactionSource) {
                                    interactionSource.interactions.collect {
                                        if (it is PressInteraction.Release) {
                                            keyboardOpened()
                                        }
                                    }
                                }
                            },
                        singleLine = true
                    )

                Box(modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth()
                    .clickable(
                        onClick = { closeKeyboard(context) },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ))

                Column()
                {
                    Row(modifier = Modifier
                        .clickable(
                            onClick = { closeKeyboard(context) },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        )
                        .height(56.dp)
                        .fillMaxWidth()
                    )
                    {
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.37f)
                            .clickable(
                                onClick = { closeKeyboard(context) },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ))
                        Box(modifier = Modifier
                            .width(100.dp)
                            .fillMaxHeight()
                            .background(color = Color(50, 32, 122))
                            .clickable(
                                onClick = { closeKeyboard(context) },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
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
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(color = Color.Black)
                            .clickable(
                                onClick = { closeKeyboard(context) },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            )
                        )
                    } // End of Login Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(color = Color.Black)
                            .clickable(
                                onClick = { closeKeyboard(context) },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            )
                    )
                    Row(modifier = Modifier
                        .clickable(
                            onClick = { closeKeyboard(context) },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        )
                        .height(56.dp)
                        .fillMaxWidth()
                    )
                    {
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.37f)
                            .clickable(
                                onClick = { closeKeyboard(context) },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ))
                        Box(modifier = Modifier
                            .width(100.dp)
                            .fillMaxHeight()
                            .background(color = Color(50, 32, 122))
                            .clickable(
                                onClick = { closeKeyboard(context) },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
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
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(color = Color.Black)
                            .clickable(
                                onClick = { closeKeyboard(context) },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            )
                        )
                    } // End of Create Account Row
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .background(color = Color.Black)
                        .clickable(
                            onClick = { closeKeyboard(context) },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        )
                )
            }
        }
    }

    private fun closeKeyboard(context: Context)
    {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
        mUiState.value!!.kbdOpen = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        val context: Context = this
        window.setDecorFitsSystemWindows(false)
        window.statusBarColor = resources.getColor(R.color.black, theme)
        window.navigationBarColor = resources.getColor(R.color.black, theme)
        mClientInfo.value!!.clientFd = connectToServer()

        if (mClientInfo.value!!.clientFd < 0)
        {
            Log.e("onCreate", "Failed to connect")
        }
        else
        {
            Log.e("onCreate", "Connection success " + mClientInfo.value!!.clientFd.toString())
        }

        //Create sockets
        setContent {
            if (mClientInfo.value!!.clientLoggedIn)
            {
                MainChat(context)
            }
            else
            {
                loginScreen(context)
            }
        }
    }
}