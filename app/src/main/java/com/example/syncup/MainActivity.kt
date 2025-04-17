package com.example.syncup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.syncup.R
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.MessageDigest

class UiState : ViewModel()
{
    var messageText by mutableStateOf("")
    var passwordText by mutableStateOf("")
    var confPasswordText by mutableStateOf("")
    var kbdOpen by mutableStateOf(false)
}

// Separate some of these functions to  Client info
class MainActivity : ComponentActivity()
{
    private val maxEmailSize by mutableStateOf(320)
    private val maxPasswordSize by mutableStateOf(32)
    companion object
    {
      init {
         System.loadLibrary("client")
      }
    }

    fun getClientFd() : Int
    {
        return mClientState.value!!.getClientSocket()
    }

    fun setClientFd(fd : Int)
    {
        mClientState.value!!.setClientFd(fd)
    }

    private val mUiState : MutableLiveData<UiState> = MutableLiveData(UiState())
    private val mClientState : MutableLiveData<ClientState> = MutableLiveData(ClientState())

    external fun connectToServer() : Int
    external fun close(fd : Int)

    external fun recv(fd : Int, buffer : ByteArray, bytesToRecv : Int, timeoutTime : Int) : Int

    external fun sendMsg(fd : Int, msg : ByteArray, msgSize : Int) : Int

    override fun onDestroy() {
        super.onDestroy()
        close(mClientState.value!!.getClientSocket())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        window.statusBarColor = resources.getColor(R.color.black, theme)
        window.navigationBarColor = resources.getColor(R.color.black, theme)
        mClientState.value!!.setClientFd(connectToServer()) //TODO: Uncomment out and remove bottom line
        mClientState.value!!.setSendFunc(::sendMsg)
        mClientState.value!!.setRecvFunc(::recv)

        if (mClientState.value!!.getClientSocket() < 0)
        {
            Log.e("onCreate", "Failed to connect")
        }
        else
        {
            Log.e("onCreate", "Connection success " + mClientState.value!!.getClientSocket().toString())
        }

        //Create sockets
        setContent {
            mClientState.value!!.setBinder(window.decorView.windowToken)
            mClientState.value!!.setInputManager(this.getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager)
            when(mClientState.value!!.getClientAppState())
            {
                ClientAppState.LOGGED_OUT -> loginScreen(mClientState)
                ClientAppState.CREATE_ACC -> createAccScreen(mClientState)
                ClientAppState.LOGGED_IN -> { MainChat(mClientState) }
                ClientAppState.DIR_NAV -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    {
                        val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                        val intent =
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                        if (!Environment.isExternalStorageManager()) {
                            startActivity(intent)
                        }
                        else if(Environment.isExternalStorageManager())
                        {
                            DirectoryUI(mClientState)
                        }
                    }
                }
                ClientAppState.ACC_CREATED-> accountCreatedScreen(mClientState)
            }
        }
    }
}