package com.example.numbercomventer

import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.os.Bundle
import androidx.compose.ui.unit.Dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState

val appBgColor = Color(0xFF0A0A0A)
val brightGreen = Color(0xFF00FF41)
val mutedGreen = Color(0xFF1A4D1A)
val errorRed = Color(0xFFFF3333)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = appBgColor
            ) {
                NumberConverterApp()
            }
        }
    }
}

enum class NumberType(val displayName: String) {
    BINARY("BINARY"),
    ROMAN("ROMAN"),
    OCTAL("OCTAL"),
    DECIMAL("DECIMAL"),
    HEXADECIMAL("HEXADECIMAL")
}
sealed class ConversionState {
    object Empty : ConversionState()
    data class Success(val value: String) : ConversionState()
    data class Error(val message: String) : ConversionState()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberConverterApp() {
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var inputNumber by remember { mutableStateOf("") }
    var fromType by remember { mutableStateOf(NumberType.DECIMAL) }
    var toType by remember { mutableStateOf(NumberType.BINARY) }
    var conversionResult by remember { mutableStateOf<ConversionState>(ConversionState.Empty) }

    val scrollState = rememberScrollState()

    LaunchedEffect(inputNumber) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Real-time conversion
    LaunchedEffect(inputNumber, fromType, toType) {
        if (inputNumber.isNotEmpty()) {
            conversionResult = convertNumber(inputNumber, fromType, toType)
        } else {
            conversionResult = ConversionState.Empty
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor)
            .systemBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP AD BANNER
        AdBanner()

        Spacer(modifier = Modifier.height(16.dp))

        // MAIN CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(2.dp),
            colors = CardDefaults.cardColors(containerColor = appBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "> NUMBER_CONVERTER",
                        fontSize = 20.sp,
                        color = brightGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                val isInputError = !isValidForType(inputNumber, fromType)

                // BULLETPROOF INPUT DISPLAY
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Transparent)
                        .border(1.dp, if (isInputError) errorRed else brightGreen, RoundedCornerShape(2.dp))
                        .combinedClickable(
                            onClick = { /* Do nothing on short tap */ },
                            onLongClick = {
                                val clipboardText = clipboardManager.getText()?.text
                                if (!clipboardText.isNullOrBlank()) {
                                    inputNumber = clipboardText
                                    android.widget.Toast.makeText(context, "Pasted!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        .padding(horizontal = 8.dp), // Reduced slightly to make room for arrows
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT SCROLL INDICATOR
                        if (scrollState.value > 0) {
                            Text("< ", color = mutedGreen, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                        } else {
                            Spacer(modifier = Modifier.width(16.dp)) // Keeps text from jumping around
                        }

                        // ACTUAL TEXT
                        Text(
                            text = if (inputNumber.isEmpty()) "_" else inputNumber,
                            color = if (isInputError) errorRed else brightGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f) // Takes up remaining space between arrows
                                .horizontalScroll(scrollState)
                        )

                        // RIGHT SCROLL INDICATOR
                        if (scrollState.value < scrollState.maxValue) {
                            Text(" >", color = mutedGreen, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                        } else {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FROM / TO HEADERS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.weight(0.05f))
                    Text("FROM", color = brightGreen, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = brightGreen)
                    Spacer(Modifier.weight(1f))
                    Text("TO", color = brightGreen, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Spacer(Modifier.weight(0.15f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // SELECTION ROWS
                NumberType.entries.forEach { type ->
                    SelectionRow(
                        type = type,
                        isFrom = (fromType == type),
                        isTo = (toType == type),
                        onFromClick = {
                            if (fromType != type) {
                                if (toType == type) toType = fromType
                                fromType = type
                                inputNumber = ""
                            }
                            if (type != NumberType.DECIMAL) keyboardController?.hide()
                        },
                        onToClick = {
                            if (fromType == type) fromType = toType
                            toType = type
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RESULT BOX (Terminal Style)
                // RESULT BOX (Terminal Style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Transparent)
                        .border(1.dp, brightGreen, RoundedCornerShape(2.dp))
                        .clickable {
                            // Only copy if it's a success!
                            if (conversionResult is ConversionState.Success) {
                                val textToCopy = (conversionResult as ConversionState.Success).value
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "Data copied.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(16.dp)
                ) {
                    Column {
                        when (val state = conversionResult) {
                            is ConversionState.Empty -> {
                                Text(
                                    text = "Awaiting input...",
                                    fontSize = 16.sp,
                                    color = mutedGreen, // Make empty state look dim
                                    fontFamily = FontFamily.Monospace,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                            is ConversionState.Error -> {
                                Text(
                                    text = state.message,
                                    fontSize = 16.sp,
                                    color = errorRed, // Make errors flash red!
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            is ConversionState.Success -> {
                                Text(
                                    text = state.value,
                                    fontSize = 16.sp,
                                    color = brightGreen, // Valid math is terminal green
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- CUSTOM KEYBOARDS ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    when (fromType) {
                        NumberType.ROMAN -> RomanKeyboard(
                            onKeyPressed = { if (inputNumber.length < 15) inputNumber += it },
                            onBackspace = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                            onClear = { inputNumber = "" } // <-- Added Clear logic!
                        )
                        NumberType.HEXADECIMAL -> HexadecimalKeyboard(
                            onKeyPressed = { if (inputNumber.length < 15) inputNumber += it },
                            onBackspace = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                            onClear = { inputNumber = "" }
                        )
                        NumberType.BINARY -> BinaryKeyboard(
                            onKeyPressed = { if (inputNumber.length < 62) inputNumber += it },
                            onBackspace = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                            onClear = { inputNumber = "" }
                        )
                        NumberType.OCTAL -> OctalKeyboard(
                            onKeyPressed = { if (inputNumber.length < 21) inputNumber += it },
                            onBackspace = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                            onClear = { inputNumber = "" }
                        )
                        NumberType.DECIMAL -> DecimalKeyboard(
                            onKeyPressed = { if (inputNumber.length < 18) inputNumber += it },
                            onBackspace = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                            onClear = { inputNumber = "" }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionRow(
    type: NumberType,
    isFrom: Boolean,
    isTo: Boolean,
    onFromClick: () -> Unit,
    onToClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Highlight Pills - Changed to sharp rectangles for terminal look
        if (isFrom) {
            Row(
                Modifier
                    .fillMaxWidth(0.68f)
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .background(mutedGreen, RoundedCornerShape(2.dp))
            ) {}
        } else if (isTo) {
            Row(
                Modifier
                    .fillMaxWidth(0.68f)
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .background(mutedGreen, RoundedCornerShape(2.dp))
            ) {}
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onFromClick() }
                    .padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                CustomRadioButton(selected = isFrom)
            }

            Text(
                text = type.displayName,
                color = brightGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onToClick() }
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                CustomRadioButton(selected = isTo)
            }
        }
    }
}

@Composable
fun CustomRadioButton(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(if (selected) brightGreen else Color.Transparent, RoundedCornerShape(2.dp))
            .border(1.dp, brightGreen, RoundedCornerShape(2.dp))
    )
}

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(appBgColor)
            .border(1.dp, mutedGreen),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .border(1.dp, brightGreen, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "AD", color = brightGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SYS_UPDATE_AVAILABLE",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = brightGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Download latest utility packages...",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = mutedGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .height(30.dp)
                    .border(1.dp, brightGreen, RoundedCornerShape(2.dp))
                    .clickable { /* Action */ }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EXECUTE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = brightGreen
                )
            }
        }
    }
}

// --- CONVERSION LOGIC ---
fun convertNumber(input: String, from: NumberType, to: NumberType): ConversionState {
    val cleanInput = input.trim()
    if (cleanInput.isBlank()) return ConversionState.Empty
    if (from == to) return ConversionState.Success(cleanInput)

    return try {
        val decimalValue: Long = when (from) {
            NumberType.DECIMAL -> cleanInput.toLong()
            NumberType.BINARY -> cleanInput.toLong(2)
            NumberType.OCTAL -> cleanInput.toLong(8)
            NumberType.HEXADECIMAL -> cleanInput.toLong(16)
            NumberType.ROMAN -> romanToInt(cleanInput.uppercase()).toLong()
        }

        when (to) {
            NumberType.DECIMAL -> ConversionState.Success(decimalValue.toString())
            NumberType.BINARY -> ConversionState.Success(decimalValue.toString(2).uppercase())
            NumberType.OCTAL -> ConversionState.Success(decimalValue.toString(8).uppercase())
            NumberType.HEXADECIMAL -> ConversionState.Success(decimalValue.toString(16).uppercase())
            NumberType.ROMAN -> {
                if (decimalValue in 1..3999) ConversionState.Success(intToRoman(decimalValue.toInt()))
                else ConversionState.Error("ERR: ROMAN_LIMIT (1-3999)")
            }
        }
    } catch (e: Exception) {
        ConversionState.Error("ERR: INVALID_BASE")
    }
}

fun romanToInt(s: String): Int {
    val romanMap = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
    var result = 0
    var prevValue = 0

    for (i in s.indices.reversed()) {
        val currentValue = romanMap[s[i]] ?: throw IllegalArgumentException("Invalid Roman")
        if (currentValue < prevValue) result -= currentValue else result += currentValue
        prevValue = currentValue
    }
    return result
}

fun intToRoman(num: Int): String {
    val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
    var currentNum = num
    val sb = StringBuilder()

    for (i in values.indices) {
        while (currentNum >= values[i]) {
            currentNum -= values[i]
            sb.append(symbols[i])
        }
    }
    return sb.toString()
}

// --- KEYBOARDS ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyboardButton(
    text: String,
    modifier: Modifier,
    height: Dp = 48.dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {} // Added default empty action so normal keys don't break
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.Transparent)
            .border(1.dp, brightGreen, RoundedCornerShape(2.dp))
            .combinedClickable( // Swapped clickable for combinedClickable
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = brightGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp
        )
    }
}

@Composable
fun RomanKeyboard(onKeyPressed: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    val row1 = listOf("I", "V", "X", "L")
    val row2 = listOf("C", "D", "M", "⌫")
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { key ->
                if (key == "⌫") KeyboardButton(key, Modifier.weight(1f), onClick = { onBackspace() }, onLongClick = { onClear() })
                else KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) })
            }
        }
    }
}

@Composable
fun DecimalKeyboard(onKeyPressed: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("0", "1", "2").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("3", "4", "5").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) }
                }
            }
            KeyboardButton(text = "⌫", modifier = Modifier.weight(1f), height = 100.dp, onClick = { onBackspace() }, onLongClick = { onClear() })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("6", "7", "8", "9").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) }
        }
    }
}

@Composable
fun HexadecimalKeyboard(onKeyPressed: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(modifier = Modifier.weight(5f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("0", "1", "2", "3", "4").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("5", "6", "7", "8", "9").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) }
                }
            }
            KeyboardButton(text = "⌫", modifier = Modifier.weight(1f), height = 100.dp, onClick = { onBackspace() }, onLongClick = { onClear() })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("A", "B", "C", "D", "E", "F").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) }
        }
    }
}

@Composable
fun BinaryKeyboard(onKeyPressed: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyboardButton("0", Modifier.weight(1f), onClick = { onKeyPressed("0") })
            KeyboardButton("1", Modifier.weight(1f), onClick = { onKeyPressed("1") })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyboardButton("⌫", Modifier.weight(1f), onClick = { onBackspace() }, onLongClick = { onClear() })
        }
    }
}

@Composable
fun OctalKeyboard(onKeyPressed: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    val rows = listOf(listOf("0", "1", "2"), listOf("3", "4", "5"), listOf("6", "7", "⌫"))
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    if (key == "⌫") KeyboardButton(key, Modifier.weight(1f), onClick = { onBackspace() }, onLongClick = { onClear() })
                    else KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) })
                }
            }
        }
    }
}

fun isValidForType(input: String, type: NumberType): Boolean {
    if (input.isBlank()) return true
    return try {
        when (type) {
            NumberType.BINARY -> { if (!input.all { it == '0' || it == '1' }) return false; input.toLong(2); true }
            NumberType.OCTAL -> { if (!input.all { it in '0'..'7' }) return false; input.toLong(8); true }
            NumberType.DECIMAL -> { if (!input.all { it.isDigit() }) return false; input.toLong(); true }
            NumberType.HEXADECIMAL -> { if (!input.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) return false; input.toLong(16); true }
            NumberType.ROMAN -> input.uppercase().matches(Regex("^M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$"))
        }
    } catch (_: Exception) { false }
}