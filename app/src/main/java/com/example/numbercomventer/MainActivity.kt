package com.example.numbercomventer

import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

//0xFF - 100% opacity (alpha) + standard hex code
val appBgColor = Color(0xFF0A0A0A)
val brightGreen = Color(0xFF00FF41)
val mutedGreen = Color(0xFF1A4D1A)
val errorRed = Color(0xFFFF3333)

// Enums defines possible data types, only 5 types possible
enum class NumberType(val displayName: String) {
    BINARY("BINARY"),
    ROMAN("ROMAN"),
    OCTAL("OCTAL"),
    DECIMAL("DECIMAL"),
    HEXADECIMAL("HEXADECIMAL")
}

// sealed enum class defines the possible conversion states, only 3 possible
sealed class ConversionState {
    object Empty : ConversionState() // No input yet
    data class Success(val value: String) : ConversionState() //ConversionState = result
    data class Error(val message: String) : ConversionState() // ConversionState = the error reason
}

// THE MODEL - StateSnapshot
data class ConverterUiState(
    val inputNumber: String = "",
    val fromType: NumberType = NumberType.DECIMAL,
    val toType: NumberType = NumberType.BINARY,
    val conversionResult: ConversionState = ConversionState.Empty,
    val isInputError: Boolean = false // Flag to quickly tell the UI to turn the input box red
)

// THE VIEWMODEL - whole logic, math, updates, ui communication
class NumberConverterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ConverterUiState()) //private and mutable state holder
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow() //public, read only copy of state holder - for ui to read but do not change
    fun onKeyPressed(key: String) {
        val current = _uiState.value
        val maxLength = when (current.fromType) {
            NumberType.BINARY -> 62
            NumberType.OCTAL -> 21
            NumberType.DECIMAL -> 18
            else -> 15
        }
        if (current.inputNumber.length < maxLength) {
            updateInput(current.inputNumber + key)
        }
    }
    fun onBackspace() {
        val current = _uiState.value
        if (current.inputNumber.isNotEmpty()) {
            updateInput(current.inputNumber.dropLast(1))
        }
    }
    // on long backspace click whole input is cleared
    fun onClear() {
        updateInput("")
    }
    // on long click on input window input is pasted from phone's clipboard
    fun onPaste(text: String) {
        updateInput(text)
    }
    fun onFromTypeChanged(type: NumberType) {
        val current = _uiState.value
        if (current.fromType != type) {
            var newToType = current.toType
            // "FROM" type = "TO" type -> swap types
            if (current.toType == type) {
                newToType = current.fromType
            }
            _uiState.update {
                it.copy(fromType = type, toType = newToType, inputNumber = "")
            }
            updateInput("") //start with empty input
        }
    }
    fun onToTypeChanged(type: NumberType) {
        val current = _uiState.value
        if (current.fromType == type) {
            _uiState.update { it.copy(fromType = current.toType, toType = type) }
        } else {
            _uiState.update { it.copy(toType = type) }
        }
        // Recalculate with new output type
        updateInput(_uiState.value.inputNumber)
    }

    // input coordinator - calculator manager: Updates the string, checks for errors, runs the math, and saves the final state
    private fun updateInput(newInput: String) {
        val current = _uiState.value
        val isError = !isValidForType(newInput, current.fromType)
        val result = convertNumber(newInput, current.fromType, current.toType)
        _uiState.update {
            it.copy(
                inputNumber = newInput,
                isInputError = isError,
                conversionResult = result
            )
        }
    }
    private fun convertNumber(input: String, from: NumberType, to: NumberType): ConversionState {
        val cleanInput = input.trim()
        if (cleanInput.isBlank()) return ConversionState.Empty
        if (from == to) return ConversionState.Success(cleanInput) // No math needed if converting Decimal to Decimal!
        return try {
            //input -> standard Long - decimal
            val decimalValue: Long = when (from) {
                NumberType.DECIMAL -> cleanInput.toLong()
                NumberType.BINARY -> cleanInput.toLong(2)
                NumberType.OCTAL -> cleanInput.toLong(8)
                NumberType.HEXADECIMAL -> cleanInput.toLong(16)
                NumberType.ROMAN -> romanToInt(cleanInput.uppercase()).toLong() //custom roman to int function
            }
            // decimal -> output
            when (to) {
                NumberType.DECIMAL -> ConversionState.Success(decimalValue.toString())
                NumberType.BINARY -> ConversionState.Success(decimalValue.toString(2).uppercase())
                NumberType.OCTAL -> ConversionState.Success(decimalValue.toString(8).uppercase())
                NumberType.HEXADECIMAL -> ConversionState.Success(decimalValue.toString(16).uppercase())
                NumberType.ROMAN -> {
                    if (decimalValue in 1..3999) ConversionState.Success(intToRoman(decimalValue.toInt())) //custom int to roman function
                    else ConversionState.Error("ERR: ROMAN_LIMIT (1-3999)")
                }
            }
        } catch (e: Exception) {
            ConversionState.Error("ERR: INVALID_BASE")
        }
    }
    private fun romanToInt(s: String): Int {
        val romanMap = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
        var result = 0
        var prevValue = 0
        for (i in s.indices.reversed()) {
            val currentValue = romanMap[s[i]] ?: throw IllegalArgumentException("Invalid Roman")
            if (currentValue < prevValue) result -= currentValue else result += currentValue //If IV -> it subtracts -else-> it adds.
            prevValue = currentValue
        }
        return result
    }
    // From num subtract the biggest possible num -> add symbol to roman num, until num = 0.
    private fun intToRoman(num: Int): String {
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
    // Validates if input is correct for input type
    private fun isValidForType(input: String, type: NumberType): Boolean {
        if (input.isBlank()) return true
        return try {
            when (type) {
                NumberType.BINARY -> { if (!input.all { it == '0' || it == '1' }) return false; input.toLong(2); true }
                NumberType.OCTAL -> { if (!input.all { it in '0'..'7' }) return false; input.toLong(8); true }
                NumberType.DECIMAL -> { if (!input.all { it.isDigit() }) return false; input.toLong(); true }
                NumberType.HEXADECIMAL -> { if (!input.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) return false; input.toLong(16); true }
                //max MMM, no more than 3000; allows CM, CD, D, DCCC, CCC and similar correct; XC, XL, L, LX, XX, and similar correct; IX, IV, V, VI, and similar correct
                NumberType.ROMAN -> input.uppercase().matches(Regex("^M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$"))
            }
        } catch (_: Exception) { false }
    }
}

//THE VIEW (UI - Jetpack Compose) - only reads the State and draws UI.
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberConverterApp(viewModel: NumberConverterViewModel = viewModel()) {
    //when _uiState changes, the screen will redraw automatically
    val uiState by viewModel.uiState.collectAsState()

    // Tools provided by the Android OS for interacting with the system(keyboard, clipboard, toast msgs)
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Remembers how far the user has scrolled horizontally in the text input
    val scrollState = rememberScrollState()

    // automatic scroling to the typing position
    LaunchedEffect(uiState.inputNumber) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor)
            .systemBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AdBanner()
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(2.dp),
            colors = CardDefaults.cardColors(containerColor = appBgColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //HEADER
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Text(
                        text = "> NUMBER_CONVERTER",
                        fontSize = 20.sp, color = brightGreen,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                //INPUT DISPLAY
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .border(1.dp, if (uiState.isInputError) errorRed else brightGreen, RoundedCornerShape(2.dp))
                        .combinedClickable(
                            onClick = { },
                            onLongClick = {
                                val clipboardText = clipboardManager.getText()?.text
                                if (!clipboardText.isNullOrBlank()) {
                                    viewModel.onPaste(clipboardText)
                                    Toast.makeText(context, "Pasted!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (scrollState.value > 0) Text("< ", color = mutedGreen, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                        else Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = if (uiState.inputNumber.isEmpty()) "_" else uiState.inputNumber,
                            color = if (uiState.isInputError) errorRed else brightGreen,
                            fontFamily = FontFamily.Monospace, fontSize = 16.sp, maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(scrollState)
                        )

                        if (scrollState.value < scrollState.maxValue) Text(" >", color = mutedGreen, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                        else Spacer(modifier = Modifier.width(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                //FROM / TO HEADERS
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Spacer(Modifier.weight(0.05f))
                    Text("FROM", color = brightGreen, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = brightGreen)
                    Spacer(Modifier.weight(1f))
                    Text("TO", color = brightGreen, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Spacer(Modifier.weight(0.15f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                //SELECTION ROWS
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(NumberType.entries) { type ->
                        SelectionRow(
                            type = type,
                            isFrom = (uiState.fromType == type),
                            isTo = (uiState.toType == type),
                            onFromClick = {
                                viewModel.onFromTypeChanged(type)
                                if (type != NumberType.DECIMAL) keyboardController?.hide()
                            },
                            onToClick = { viewModel.onToTypeChanged(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                //RESULT BOX
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .border(1.dp, brightGreen, RoundedCornerShape(2.dp))
                        .clickable {
                            (uiState.conversionResult as? ConversionState.Success)?.let { success ->
                                clipboardManager.setText(AnnotatedString(success.value))
                                Toast.makeText(context, "Data copied.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(16.dp)
                ) {
                    when (val state = uiState.conversionResult) {
                        is ConversionState.Empty -> Text("Awaiting input...", fontSize = 16.sp, color = mutedGreen, fontFamily = FontFamily.Monospace, fontStyle = FontStyle.Italic)
                        is ConversionState.Error -> Text(state.message, fontSize = 16.sp, color = errorRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        is ConversionState.Success -> Text(state.value, fontSize = 16.sp, color = brightGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                //CUSTOM KEYBOARDS
                Box(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    when (uiState.fromType) {
                        NumberType.ROMAN -> RomanKeyboard(onKeyPressed = { viewModel.onKeyPressed(it) }, onBackspace = { viewModel.onBackspace() }, onClear = { viewModel.onClear() })
                        NumberType.HEXADECIMAL -> HexadecimalKeyboard(onKeyPressed = { viewModel.onKeyPressed(it) }, onBackspace = { viewModel.onBackspace() }, onClear = { viewModel.onClear() })
                        NumberType.BINARY -> BinaryKeyboard(onKeyPressed = { viewModel.onKeyPressed(it) }, onBackspace = { viewModel.onBackspace() }, onClear = { viewModel.onClear() })
                        NumberType.OCTAL -> OctalKeyboard(onKeyPressed = { viewModel.onKeyPressed(it) }, onBackspace = { viewModel.onBackspace() }, onClear = { viewModel.onClear() })
                        NumberType.DECIMAL -> DecimalKeyboard(onKeyPressed = { viewModel.onKeyPressed(it) }, onBackspace = { viewModel.onBackspace() }, onClear = { viewModel.onClear() })
                    }
                }
            }
        }
    }
}

// Reusable components
@Composable
fun SelectionRow(type: NumberType, isFrom: Boolean, isTo: Boolean, onFromClick: () -> Unit, onToClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
        if (isFrom) Row(Modifier.fillMaxWidth(0.68f).align(Alignment.CenterStart).fillMaxHeight().background(mutedGreen, RoundedCornerShape(2.dp))) {}
        else if (isTo) Row(Modifier.fillMaxWidth(0.68f).align(Alignment.CenterEnd).fillMaxHeight().background(mutedGreen, RoundedCornerShape(2.dp))) {}

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onFromClick() }.padding(start = 16.dp), contentAlignment = Alignment.CenterStart) {
                CustomRadioButton(selected = isFrom)
            }
            Text(text = type.displayName, color = brightGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onToClick() }.padding(end = 16.dp), contentAlignment = Alignment.CenterEnd) {
                CustomRadioButton(selected = isTo)
            }
        }
    }
}

@Composable
fun CustomRadioButton(selected: Boolean) {
    Box(modifier = Modifier.size(16.dp).background(if (selected) brightGreen else Color.Transparent, RoundedCornerShape(2.dp)).border(1.dp, brightGreen, RoundedCornerShape(2.dp)))
}

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(50.dp).background(appBgColor).border(1.dp, mutedGreen), contentAlignment = Alignment.Center) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(2.dp)).border(1.dp, brightGreen, RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) {
                Text(text = "AD", color = brightGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = "SYS_UPDATE_AVAILABLE", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = brightGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "Download latest utility packages...", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = mutedGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.wrapContentSize().height(30.dp).border(1.dp, brightGreen, RoundedCornerShape(2.dp)).clickable { }.padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                Text(text = "EXECUTE", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = brightGreen)
            }
        }
    }
}

// INDIVIDUAL KEYBOARD LAYOUTS
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyboardButton(text: String, modifier: Modifier, height: Dp = 48.dp, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Box(modifier = modifier.height(height).clip(RoundedCornerShape(2.dp)).background(Color.Transparent).border(1.dp, brightGreen, RoundedCornerShape(2.dp)).combinedClickable(onClick = { onClick() }, onLongClick = { onLongClick() }), contentAlignment = Alignment.Center) {
        Text(text = text, color = brightGreen, fontFamily = FontFamily.Monospace, fontSize = 20.sp)
    }
}

@Composable
fun RomanKeyboard(onKeyPressed: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    val row1 = listOf("I", "V", "X", "L")
    val row2 = listOf("C", "D", "M", "⌫")
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { row1.forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) } }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { key -> if (key == "⌫") KeyboardButton(key, Modifier.weight(1f), onClick = { onBackspace() }, onLongClick = { onClear() }) else KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) }
        }
    }
}

@Composable
fun DecimalKeyboard(onKeyPressed: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("0", "1", "2").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) } }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("3", "4", "5").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) } }
            }
            KeyboardButton(text = "⌫", modifier = Modifier.weight(1f), height = 100.dp, onClick = { onBackspace() }, onLongClick = { onClear() })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("6", "7", "8", "9").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) } }
    }
}

@Composable
fun HexadecimalKeyboard(onKeyPressed: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(modifier = Modifier.weight(5f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("0", "1", "2", "3", "4").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) } }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("5", "6", "7", "8", "9").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) } }
            }
            KeyboardButton(text = "⌫", modifier = Modifier.weight(1f), height = 100.dp, onClick = { onBackspace() }, onLongClick = { onClear() })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("A", "B", "C", "D", "E", "F").forEach { key -> KeyboardButton(key, Modifier.weight(1f), onClick = { onKeyPressed(key) }) } }
    }
}

@Composable
fun BinaryKeyboard(onKeyPressed: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyboardButton("0", Modifier.weight(1f), onClick = { onKeyPressed("0") })
            KeyboardButton("1", Modifier.weight(1f), onClick = { onKeyPressed("1") })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { KeyboardButton("⌫", Modifier.weight(1f), onClick = { onBackspace() }, onLongClick = { onClear() }) }
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
/*
@Preview(showBackground = true, device = "spec:width=320dp,height=480dp")
@Composable
fun SmallPhonePreview() {
    NumberConverterApp()
}
*/
@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp")
@Composable
fun TabletPreview() {
    NumberConverterApp()
}