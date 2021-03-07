/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.androiddevchallenge.ui.theme.JetCounterTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CountdownInfo(val currentSec: Int, val totalSec: Int) {
    companion object {
        val default = CountdownInfo(currentSec = 0, totalSec = 0)
    }

    val progress: Float
        get() = currentSec * 1f / totalSec

    val displayText: String
        get() {
            var remaining = currentSec
            val hour = currentSec / 60 / 60
            remaining -= hour * 60 * 60

            val min = remaining / 60
            remaining -= min * 60

            val sec = remaining
            return "${hour.format()}:${min.format()}:${sec.format()}"
        }

    private fun Int.format(): String {
        return String.format("%02d", this)
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _countdownInfo = MutableLiveData<CountdownInfo>()
    val countdownInfo: LiveData<CountdownInfo>
        get() = _countdownInfo

    private var job: Job? = null

    val started: Boolean
        get() = job?.isActive ?: false

    var totalSec: Int = 0

    fun setTotalSec(sec: Int = 0, min: Int = 0, hour: Int = 0) {
        totalSec = hour * 60 * 60 + min * 60 + sec
        resetValue()
    }

    fun start() {
        job = viewModelScope.launch {
            var remainSec = totalSec

            while (remainSec >= 0) {
                _countdownInfo.value = CountdownInfo(currentSec = remainSec, totalSec = totalSec)
                Log.i(TAG, "countdown remain sec is $remainSec")

                delay(1_000)
                remainSec--
            }
        }
    }

    fun cancel() {
        job?.cancel()
        resetValue()
    }

    private fun resetValue() {
        _countdownInfo.value = CountdownInfo(currentSec = totalSec, totalSec = totalSec)
    }
}

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JetCounterTheme {
                MyApp(viewModel)
            }
        }
    }
}

// Start building your app here!
@ExperimentalAnimationApi
@Composable
fun MyApp(viewModel: MainViewModel) {
    val countdownInfo = viewModel.countdownInfo.observeAsState().value ?: CountdownInfo.default

    Surface(
        color = MaterialTheme.colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        ConstraintLayout {
            val (title, content) = createRefs()

            Text(
                text = "JET COUNTER",
                color = MaterialTheme.colors.primaryVariant,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(parent.top, margin = 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.constrainAs(content) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f)
                ) {
                    CountdownCircle(countdownInfo)
                    CountDownText(countdownInfo.displayText)
                }

                ContentVerticalSpacer()

                CountdownChoices(viewModel)

                repeat(3) {
                    ContentVerticalSpacer()
                }

                Crossfade(targetState = viewModel.started) {
                    if (it) {
                        OutlinedButton(
                            onClick = { viewModel.cancel() },
                            modifier = Modifier.width(150.dp)
                        ) {
                            Text("CANCEL", modifier = Modifier.padding(8.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.start()
                            },
                            modifier = Modifier.width(150.dp)
                        ) {
                            Text("START", modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownChoices(viewModel: MainViewModel) {
    Row {
        val selectedMin = remember { mutableStateOf(viewModel.totalSec) }

        CountdownChoiceButton(
            selectedMin = selectedMin.value,
            min = 60,
            onClick = {
                viewModel.setTotalSec(min = it)
                selectedMin.value = it
            }
        )

        ContentHorizontalSpacer()

        CountdownChoiceButton(
            selectedMin = selectedMin.value,
            min = 30,
            onClick = {
                viewModel.setTotalSec(min = it)
                selectedMin.value = it
            }
        )

        ContentHorizontalSpacer()

        CountdownChoiceButton(
            selectedMin = selectedMin.value,
            min = 15,
            onClick = {
                viewModel.setTotalSec(min = it)
                selectedMin.value = it
            }
        )

        ContentHorizontalSpacer()

        CountdownChoiceButton(
            selectedMin = selectedMin.value,
            min = 5,
            onClick = {
                viewModel.setTotalSec(min = it)
                selectedMin.value = it
            }
        )
    }
}

@Composable
fun CountdownChoiceButton(selectedMin: Int, min: Int, onClick: (Int) -> Unit) {
    if (selectedMin == min) {
        Button(
            onClick = {
                onClick(min)
            }
        ) {
            Text("$min MIN")
        }
    } else {
        OutlinedButton(
            onClick = {
                onClick(min)
            }
        ) {
            Text("$min MIN")
        }
    }
}

@Composable
fun ContentVerticalSpacer() {
    Spacer(Modifier.height(20.dp))
}

@Composable
fun ContentHorizontalSpacer() {
    Spacer(Modifier.width(8.dp))
}

@Composable
fun CountdownCircle(info: CountdownInfo) {
    val ringColor = MaterialTheme.colors.primary
    val progressColor = MaterialTheme.colors.primaryVariant
    val strokeWidth = 30.dp.value
    val sweepAngle = 360 * info.progress

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .padding(20.dp)
    ) {
        drawCircle(color = ringColor.copy(alpha = 0.5f), style = Stroke(width = strokeWidth))
        if (info.progress > 0) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun CountDownText(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = MaterialTheme.colors.primaryVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 50.sp
        )
    )
}

@ExperimentalAnimationApi
@Preview(
    "Light Theme",
    widthDp = 360,
    heightDp = 640,
)
@Composable
fun LightPreview() {
    JetCounterTheme {
        MyApp(MainViewModel(Application()))
    }
}
