/*package com.example.program

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.program.ui.theme.ProgramTheme
import com.example.program.screens.SplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProgramTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProgramTheme {
        Greeting("Android")
    }
}*/

package com.example.stopchase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stopchase.screens.FaceCaptureScreen
import com.example.stopchase.screens.SplashScreen
import com.example.stopchase.screens.SetupScreen

// Test gitHub

class MainActivity : ComponentActivity() {
    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen(
                onFaceLogin = {
                    // Тут запускаєш FaceCaptureScreen або логіку перевірки
                    navController.navigate("face_capture")
                }
            )
        }
    }*/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") {
                    SplashScreen(
                        onFaceLogin = {
                            navController.navigate("face_capture")
                        }
                    )
                }

                composable("face_capture") {
                    FaceCaptureScreen(
                        onFaceCaptured = {
                            navController.navigate("setup") // ⬅️ зміна з "home" на "setup"
                        }
                    )
                }

                composable("setup") {
                    SetupScreen() // ⬅️ новий екран замість HomeScreen
                }
            }
        }
    }
}



