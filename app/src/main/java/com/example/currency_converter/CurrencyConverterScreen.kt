package com.example.currency_converter

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor
import androidx.core.graphics.toColorInt

/**
 * CurrencyConverterScreen is the main composable that builds the currency conversion UI.
 * Handles currency selection, input, conversion, and rendering a historical graph.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CurrencyConverterScreen(viewModel: CurrencyViewModel)
{
    val context = LocalContext.current
    val font = FontFamily.Default
    val haptics = LocalHapticFeedback.current

    // Collect state from ViewModel
    val fromCurrency by viewModel.fromCurrency.collectAsState()
    val toCurrency by viewModel.toCurrency.collectAsState()
    val input by viewModel.inputAmount.collectAsState()
    val result by viewModel.conversionResult.collectAsState()
    val currencies by viewModel.availableCurrencies.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val isLoading by viewModel.isLoading.collectAsState()
    val historicalRates by viewModel.historicalRates.collectAsState()

    // Result animation: fade + scale
    val resultAlpha = remember { Animatable(0f) }
    val resultScale = remember { Animatable(0.95f) }

    // Animate result change when new conversion is calculated
    LaunchedEffect(result)
    {
        if (result.isNotEmpty())
        {
            resultAlpha.snapTo(0f)
            resultScale.snapTo(0.95f)
            resultAlpha.animateTo(1f, animationSpec = tween(300))
            resultScale.animateTo(1f, animationSpec = tween(300))
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Reload rates if either currency changes
    LaunchedEffect(fromCurrency, toCurrency)
    {
        viewModel.loadLatestRates()
        viewModel.loadHistoricalRates(daysBack = 30)
    }

    // Root container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF181818))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    )
    {
        // Stack UI vertically
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .widthIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            // Currency selector - FROM
            CurrencyDropdown(
                selected = fromCurrency,
                options = currencies,
                onSelected = viewModel::setFromCurrency,
                font = font
            )

            // Input field for amount
            OutlinedTextField(
                value = input,
                onValueChange = viewModel::setInputAmount,
                placeholder =
                {
                    Text(
                        "Enter amount",
                        color = Color.Gray,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone =
                {
                    keyboard?.hide()
                    viewModel.convert()
                }),
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White,
                    fontFamily = font,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color(0xFF8C9EFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFF2C2C2C), shape = RoundedCornerShape(12.dp))
            )

            // Currency selector - TO
            CurrencyDropdown(
                selected = toCurrency,
                options = currencies,
                onSelected = viewModel::setToCurrency,
                font = font
            )

            // Animated result text
            Text(
                text = if (result.isEmpty()) "Result will appear here" else
                    "$input ${fromCurrency.uppercase()} = $result ${toCurrency.uppercase()}",
                color = Color(0xFF40C4FF),
                fontSize = 26.sp,
                fontFamily = font,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().graphicsLayer
                {
                    alpha = resultAlpha.value
                    scaleX = resultScale.value
                    scaleY = resultScale.value
                }
            )

            // Row for Copy / Convert buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            )
            {
                // Copy result to clipboard
                AnimatedActionButton(label = "Copy", modifier = Modifier.weight(1f))
                {
                    if (result.isNotEmpty())
                    {
                        clipboardManager.setText(AnnotatedString(result))
                        Toast.makeText(context, "Copied: $result", Toast.LENGTH_SHORT).show()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    else
                    {
                        Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
                    }
                }

                // Trigger currency conversion
                AnimatedActionButton(label = "Convert", modifier = Modifier.weight(1f))
                {
                    viewModel.convert()
                }
            }

            // Show progress bar while loading
            if (isLoading)
            {
                CircularProgressIndicator(
                    color = Color(0xFF8C9EFF),
                    strokeWidth = 4.dp,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(48.dp)
                )
            }
            else
            {
                // Show graph if historical data is available
                AnimatedVisibility(
                    visible = historicalRates != null,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut()
                )
                {
                    HistoricalRateGraph(historicalRates!!)
                }
            }
        }
    }
}

/**
 * CurrencyDropdown displays a tappable currency selector styled like a button.
 * When clicked, it opens a searchable dialog showing all available currency codes.
 *
 * @param selected The currently selected currency code.
 * @param options All available currency codes to choose from.
 * @param onSelected Callback to update selection.
 * @param font The font to use for display text.
 */
@Composable
fun CurrencyDropdown(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    font: FontFamily
)
{
    val haptics = LocalHapticFeedback.current
    var showDialog by remember { mutableStateOf(false) }

    // Main dropdown surface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF8C9EFF), shape = RoundedCornerShape(12.dp))
            .clickable
            {
                showDialog = true
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }.padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    )
    {
        // Show current selection in uppercase
        Text(
            text = selected.uppercase(),
            color = Color.White,
            fontFamily = font,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Show dialog with search + options
    if (showDialog)
    {
        Dialog(onDismissRequest = { showDialog = false })
        {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2C2C2C),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            {
                var searchQuery by remember { mutableStateOf("") }

                // Filter list as user types
                val filteredOptions = options.filter { it.contains(searchQuery, ignoreCase = true) }

                Column(modifier = Modifier.padding(16.dp))
                {
                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search currency", color = Color.Gray) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White,
                            fontFamily = font,
                            fontSize = 18.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8C9EFF),
                            unfocusedBorderColor = Color.DarkGray,
                            cursorColor = Color(0xFF8C9EFF)
                        ),
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2C))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Currency list (filtered)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    )
                    {
                        // Each item selectable
                        items(filteredOptions)
                        {
                                currency ->
                            Text(
                                text = currency.uppercase(),
                                color = Color.White,
                                fontFamily = font,
                                fontSize = 18.sp,
                                modifier = Modifier.fillMaxWidth().clickable
                                {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelected(currency)
                                    showDialog = false
                                }.padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                        }

                        // No results fallback
                        if (filteredOptions.isEmpty())
                        {
                            item()
                            {
                                Text(
                                    text = "No results",
                                    color = Color.Gray,
                                    fontFamily = font,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * HistoricalRateGraph displays a line chart of historical exchange rate data.
 * Uses MPAndroidChart via AndroidView to integrate a native chart inside Compose.
 *
 * @param rates A list of (date label, value) pairs to be plotted on the chart.
 */
@Composable
fun HistoricalRateGraph(rates: List<Pair<String, Double>>)
{
    // Fallback if no data
    if (rates.isEmpty())
    {
        Text(
            text = "No historical data available",
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    var visible by remember { mutableStateOf(false) }

    // Animate graph fade-in
    val alphaAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "graphAlpha"
    )

    // Trigger animation on load
    LaunchedEffect(Unit)
    {
        visible = true
    }

    // Render the MPAndroidChart LineChart inside Compose
    AndroidView(
        factory =
        {
            context -> LineChart(context).apply()
            {
                // Basic chart configuration
                description.isEnabled = false
                setTouchEnabled(false)
                isDragEnabled = false
                setScaleEnabled(false)
                setPinchZoom(false)

                // X axis styling
                xAxis.apply()
                {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = AndroidColor.WHITE
                    setDrawGridLines(false)
                    labelCount = 5
                }

                // Y axis styling (left only)
                axisLeft.apply()
                {
                    textColor = AndroidColor.WHITE
                    setDrawGridLines(true)
                }

                // Disable right axis entirely
                axisRight.isEnabled = false

                // Set chart background
                setBackgroundColor("#2C2C2C".toColorInt())
                setDrawGridBackground(false)

                // Animate chart entrance
                animateX(1000)

                // Hide legend for cleaner appearance
                legend.isEnabled = false
            }
        },
        update =
        {
            chart ->
            // Convert rates into chart entries
            val entries = rates.mapIndexed()
            {
                    index, rate -> Entry(index.toFloat(), rate.second.toFloat())
            }

            // Style the dataset line and fill
            val dataSet = LineDataSet(entries, "").apply()
            {
                color = "#8C9EFF".toColorInt()
                valueTextColor = AndroidColor.WHITE
                setDrawValues(false)
                lineWidth = 2f
                circleRadius = 4f
                circleColors = listOf("#40C4FF".toColorInt())
                setDrawCircleHole(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = "#404C9EFF".toColorInt()
                setDrawHighlightIndicators(false)
                isHighlightEnabled = false
            }

            // Set and redraw data
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .graphicsLayer { alpha = alphaAnim }
            .padding(vertical = 16.dp)
            .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
            .padding(8.dp)
    )
}

/**
 * AnimatedActionButton is a reusable button with a glowing, animated RGB border.
 * Includes scaling on press, cooldown to prevent spam, and haptic feedback.
 *
 * @param label Text to display on the button.
 * @param modifier Modifier for layout/styling.
 * @param onClick Lambda to invoke when pressed.
 */
@Composable
fun AnimatedActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
)
{
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isCooldown by remember { mutableStateOf(false) }

    // Infinite looping animation to simulate glowing gradient
    val infiniteTransition = rememberInfiniteTransition(label = "button-border")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Base colors for animated RGB border
    val baseColors = listOf(
        Color(0xFFBB86FC), Color(0xFF8C9EFF), Color(0xFF64B5F6),
        Color(0xFF40C4FF), Color(0xFF80D8FF), Color(0xFFBB86FC)
    )

    // Generate interpolated gradient stops for smooth animation
    val expandedStops: List<Pair<Float, Color>> = buildList()
    {
        val steps = 60
        for (i in 0 until steps)
        {
            val t = i / steps.toFloat()
            val index = (t * (baseColors.size - 1)).toInt()
            val blend = t * (baseColors.size - 1) % 1f
            val color = lerp(
                baseColors[index],
                baseColors.getOrElse(index + 1) { baseColors.last() },
                blend
            )
            add((t + progress) % 1f to color)
        }
    }.sortedBy { it.first }

    var pressed by remember { mutableStateOf(false) }

    // Scale button on press for tactile feel
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "scale"
    )

    // Button container with animated border
    Box(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer
            {
                scaleX = scale
                scaleY = scale
            }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    )
    {
        // Draw animated border with Canvas
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp))
        {
            val stroke = 2.dp.toPx()
            val radius = 16.dp.toPx()

            drawRoundRect(
                brush = Brush.sweepGradient(*expandedStops.toTypedArray()),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(radius),
                style = Stroke(width = stroke)
            )
        }

        // Core button logic
        Button(
            onClick =
                {
                    if (!isCooldown)
                    {
                        isCooldown = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()

                        // Reset cooldown after brief delay
                        coroutineScope.launch()
                        {
                            delay(200)
                            isCooldown = false
                        }
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize().padding(4.dp).pointerInput(Unit)
            {
                detectTapGestures(
                    onPress =
                    {
                        pressed = true
                        try
                        {
                            tryAwaitRelease()
                        }
                        finally
                        {
                            pressed = false
                        }
                    }
                )
            }
        )
        {
            // Text label centered in button
            Text(
                text = label,
                fontSize = 20.sp,
                color = Color.White,
                fontFamily = FontFamily.Default
            )
        }
    }
}
