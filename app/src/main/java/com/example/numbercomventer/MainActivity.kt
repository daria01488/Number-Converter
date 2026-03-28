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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val blackText = Color(0xFF000000)
val appBgColor = Color(0xFF0E1B1E)
val cardBgColor = Color(0xFF1E343C)
val brightGreen = Color(0xFF2EF600)
val lightGreen = Color(0xFF9AFF80)
val paleGreen = Color(0xFFD6FDCA)
val whiteText = Color(0xFFFDFDFD)

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberConverterApp() {
    val keyboardController = LocalSoftwareKeyboardController.current

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var inputNumber by remember { mutableStateOf("") }
    var fromType by remember { mutableStateOf(NumberType.DECIMAL) }
    var toType by remember { mutableStateOf(NumberType.BINARY) }
    var resultText by remember { mutableStateOf("The result will appear here") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor)
            .padding(top = 40.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP AD BANNER
        AdBanner()

        Spacer(modifier = Modifier.height(24.dp))

        // MAIN CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Number Converter",
                    fontSize = 22.sp,
                    color = whiteText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Determine if the current text breaks the rules
                val isInputError = !isValidForType(inputNumber, fromType)

                // BULLETPROOF INPUT DISPLAY with Long-Press Paste
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp)) // Ripple stays in bounds
                        .background(whiteText)
                        .combinedClickable(
                            onClick = { /* Do nothing on short tap */ },
                            onLongClick = {
                                val clipboardText = clipboardManager.getText()?.text
                                if (!clipboardText.isNullOrBlank()) {
                                    inputNumber = clipboardText
                                    resultText = "Enter a number to convert" // Reset result on paste
                                    android.widget.Toast.makeText(context, "Pasted!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputNumber.isEmpty()) {
                        Text("Enter number...", color = Color.Gray, fontSize = 16.sp)
                    } else {
                        Text(
                            text = inputNumber,
                            color = if (isInputError) Color.Red else blackText,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // FROM / TO HEADERS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.weight(0.05f))
                    Text("FROM", color = whiteText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = whiteText)
                    Spacer(Modifier.weight(1f))
                    Text("TO", color = whiteText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(0.15f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SELECTION ROWS
                NumberType.entries.forEach { type ->
                    SelectionRow(
                        type = type,
                        isFrom = (fromType == type),
                        isTo = (toType == type),
                        onFromClick = {
                            // Only clear things if they are actually picking a *different* type
                            if (fromType != type) {

                                // Swap logic if they pick the one currently in "TO"
                                if (toType == type) toType = fromType

                                fromType = type
                                inputNumber = "" // <--- CLEARS THE INPUT FIELD
                                resultText = "Enter a number to convert" // <--- RESETS THE RESULT BOX
                            }

                            // Hide system keyboard if switching to ANY custom keyboard
                            if (type != NumberType.DECIMAL) {
                                keyboardController?.hide()
                            }
                        },
                        onToClick = {
                            // If they click the one that is currently "FROM", swap them
                            if (fromType == type) {
                                fromType = toType
                            }
                            toType = type
                            resultText = "Enter a number to convert"
                        },
                        highlightColor = paleGreen,
                        lightGreen = lightGreen,
                        cardBgColor = cardBgColor,
                        whiteText = whiteText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // RESULT BOX
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)) // Clips the ripple effect to the rounded corners
                        .background(lightGreen)
                        .clickable {
                            // Only copy if it's an actual result, not the placeholder text or an error
                            if (!resultText.contains("Enter") && !resultText.contains("Invalid")) {
                                clipboardManager.setText(AnnotatedString(resultText))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(16.dp)
                ) {
                    Column {
                        Text("RESULT", fontSize = 12.sp, color = cardBgColor, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = resultText,
                            fontSize = 16.sp,
                            color = cardBgColor,
                            fontStyle = if (resultText.contains("Enter")) FontStyle.Italic else FontStyle.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CONVERT BUTTON
                Button(
                    onClick = {
                        keyboardController?.hide()
                        resultText = convertNumber(inputNumber, fromType, toType)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brightGreen)
                ) {
                    Text("Convert", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = blackText)
                }
                // --- CUSTOM KEYBOARDS ---
                val keyboardBg = Color(0xFF0E1B1E) // Palette app background color

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp), // Locks the area to the height of the 4-row Decimal keyboard
                    contentAlignment = Alignment.BottomCenter // Anchors shorter keyboards to the bottom
                ) {
                    when (fromType) {
                        NumberType.ROMAN -> {
                            RomanKeyboard(
                                onKeyPressed = { inputNumber += it },
                                onBackspace = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                                cardBgColor = keyboardBg,
                                lightGreen = lightGreen
                            )
                        }
                        NumberType.HEXADECIMAL -> {
                            HexadecimalKeyboard(
                                onKeyPressed = { inputNumber += it },
                                onBackspace = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                                cardBgColor = keyboardBg,
                                lightGreen = lightGreen
                            )
                        }
                        NumberType.BINARY -> {
                            BinaryKeyboard(
                                onKeyPressed = { inputNumber += it },
                                onBackspace = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                                cardBgColor = keyboardBg,
                                lightGreen = lightGreen
                            )
                        }
                        NumberType.OCTAL -> {
                            OctalKeyboard(
                                onKeyPressed = { inputNumber += it },
                                onBackspace = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                                cardBgColor = keyboardBg,
                                lightGreen = lightGreen
                            )
                        }
                        NumberType.DECIMAL -> {
                            DecimalKeyboard(
                                onKeyPressed = { inputNumber += it },
                                onBackspace = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                                cardBgColor = keyboardBg,
                                lightGreen = lightGreen
                            )
                        }
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
    onToClick: () -> Unit,
    highlightColor: Color,
    lightGreen: Color,
    cardBgColor: Color,
    whiteText: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Highlight Pills (Drawn behind the content)
        if (isFrom) {
            Row(
                Modifier
                    .fillMaxWidth(0.68f)
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .background(whiteText, CircleShape)
            ) {}
        } else if (isTo) {
            Row(
                Modifier
                    .fillMaxWidth(0.68f)
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .background(lightGreen, CircleShape)
            ) {}
        }

        // Foreground Content
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Radio Button Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onFromClick() }
                    .padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                CustomRadioButton(selected = isFrom, radioColor = if (isFrom) cardBgColor else highlightColor)
            }

            // Center Text
            Text(
                text = type.displayName,
                color = if (isFrom || isTo) cardBgColor else Color(0xFFFDFDFD),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Right Radio Button Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onToClick() }
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                CustomRadioButton(selected = isTo, radioColor = if (isTo) cardBgColor else highlightColor)
            }
        }
    }
}

@Composable
fun CustomRadioButton(selected: Boolean, radioColor: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(radioColor, CircleShape)
            .border(2.dp, radioColor, CircleShape)
    )
}
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val mainPurple = brightGreen
    val inputBg = paleGreen
    val labelGrey = cardBgColor
    val white = whiteText
    val lightGreyBorder = paleGreen

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(lightGreyBorder),
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(labelGrey),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "AD", color = white, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(12.dp))

            //AD TEXT SECTION
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Unlock Historical Date Math!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Find exact BC/AD dates with 100% precision. Get the best Date Calculator on Android!",
                    fontSize = 10.sp,
                    color = labelGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = { /* Simulation: No Action */ },
                colors = ButtonDefaults.buttonColors(containerColor = mainPurple),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .wrapContentSize()
                    .height(30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "FREE INSTALL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelGrey
                )
            }
        }
    }
}

// --- CONVERSION LOGIC ---

fun convertNumber(input: String, from: NumberType, to: NumberType): String {
    val cleanInput = input.trim()
    if (cleanInput.isBlank()) return "Please enter a valid number"
    if (from == to) return cleanInput

    return try {
        val decimalValue: Long = when (from) {
            NumberType.DECIMAL -> cleanInput.toLong()
            NumberType.BINARY -> cleanInput.toLong(2)
            NumberType.OCTAL -> cleanInput.toLong(8)
            NumberType.HEXADECIMAL -> cleanInput.toLong(16)
            NumberType.ROMAN -> romanToInt(cleanInput.uppercase()).toLong()
        }

        when (to) {
            NumberType.DECIMAL -> decimalValue.toString()
            NumberType.BINARY -> decimalValue.toString(2).uppercase()
            NumberType.OCTAL -> decimalValue.toString(8).uppercase()
            NumberType.HEXADECIMAL -> decimalValue.toString(16).uppercase()
            NumberType.ROMAN -> {
                if (decimalValue in 1..3999) {
                    intToRoman(decimalValue.toInt())
                } else {
                    "Roman numerals must be between 1 and 3999"
                }
            }
        }
    } catch (e: Exception) {
        "Invalid input for selected base"
    }
}

fun romanToInt(s: String): Int {
    val romanMap = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
    var result = 0
    var prevValue = 0

    for (i in s.indices.reversed()) {
        val currentValue = romanMap[s[i]] ?: throw IllegalArgumentException("Invalid Roman character")
        if (currentValue < prevValue) {
            result -= currentValue
        } else {
            result += currentValue
        }
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
@Composable
fun RomanKeyboard(
    onKeyPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    cardBgColor: Color,
    lightGreen: Color
) {
    val row1 = listOf("I", "V", "X", "L")
    val row2 = listOf("C", "D", "M", "⌫")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // First Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row1.forEach { key ->
                KeyboardButton(key, Modifier.weight(1f), cardBgColor, lightGreen) { onKeyPressed(key) }
            }
        }
        // Second Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row2.forEach { key ->
                if (key == "⌫") {
                    KeyboardButton(key, Modifier.weight(1f), cardBgColor, lightGreen) { onBackspace() }
                } else {
                    KeyboardButton(key, Modifier.weight(1f), cardBgColor, lightGreen) { onKeyPressed(key) }
                }
            }
        }
    }
}
@Composable
fun KeyboardButton(
    text: String,
    modifier: Modifier,
    bgColor: Color,
    textColor: Color,
    height: Dp = 48.dp, // Default height, easily overridden for tall buttons
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable
fun DecimalKeyboard(
    onKeyPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    cardBgColor: Color,
    lightGreen: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // TOP SECTION: Numbers & Tall Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Left Side: 2 Rows of Numbers (Weight 3 to match bottom row grid)
            Column(
                modifier = Modifier.weight(3f), // <--- CHANGED FROM 5f TO 3f
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("0", "1", "2").forEach { key ->
                        KeyboardButton(
                            key,
                            Modifier.weight(1f),
                            cardBgColor,
                            lightGreen
                        ) { onKeyPressed(key) }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("3", "4", "5").forEach { key ->
                        KeyboardButton(
                            key,
                            Modifier.weight(1f),
                            cardBgColor,
                            lightGreen
                        ) { onKeyPressed(key) }
                    }
                }
            }

            // Right Side: Tall Backspace (Weight 1)
            KeyboardButton(
                text = "⌫",
                modifier = Modifier.weight(1f),
                bgColor = cardBgColor,
                textColor = lightGreen,
                height = 100.dp // 48dp (row 1) + 48dp (row 2) + 4dp (spacing)
            ) { onBackspace() }
        }

        // BOTTOM SECTION: 4 equal buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("6", "7", "8", "9" ).forEach { key ->
                KeyboardButton(key, Modifier.weight(1f), cardBgColor, lightGreen) { onKeyPressed(key) }
            }
        }
    }
}
@Composable
fun HexadecimalKeyboard(
    onKeyPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    cardBgColor: Color,
    lightGreen: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // TOP SECTION: Numbers & Tall Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Left Side: 2 Rows of Numbers (Weight 5 out of 6)
            Column(
                modifier = Modifier.weight(5f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("0", "1", "2", "3", "4").forEach { key ->
                        KeyboardButton(key, Modifier.weight(1f), cardBgColor, lightGreen) { onKeyPressed(key) }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("5", "6", "7", "8", "9").forEach { key ->
                        KeyboardButton(key, Modifier.weight(1f), cardBgColor, lightGreen) { onKeyPressed(key) }
                    }
                }
            }

            // Right Side: Tall Backspace (Weight 1 out of 6)
            KeyboardButton(
                text = "⌫",
                modifier = Modifier.weight(1f),
                bgColor = cardBgColor,
                textColor = lightGreen,
                height = 100.dp // 48dp (row 1) + 48dp (row 2) + 8dp (spacing)
            ) { onBackspace() }
        }

        // BOTTOM SECTION: Letters A-F
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("A", "B", "C", "D", "E", "F").forEach { key ->
                KeyboardButton(key, Modifier.weight(1f), cardBgColor, lightGreen) { onKeyPressed(key) }
            }
        }
    }
}

@Composable
fun BinaryKeyboard(
    onKeyPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    cardBgColor: Color,
    lightGreen: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top Row: 0 and 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyboardButton("0", Modifier.weight(1f), cardBgColor, lightGreen) { onKeyPressed("0") }
            KeyboardButton("1", Modifier.weight(1f), cardBgColor, lightGreen) { onKeyPressed("1") }
        }
        // Bottom Row: Full-width Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyboardButton("⌫", Modifier.weight(1f), cardBgColor, lightGreen) { onBackspace() }
        }
    }
}

@Composable
fun OctalKeyboard(
    onKeyPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    cardBgColor: Color,
    lightGreen: Color
) {
    val rows = listOf(
        listOf("0", "1", "2"),
        listOf("3", "4", "5"),
        listOf("6", "7", "⌫")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    if (key == "⌫") {
                        KeyboardButton(key, Modifier.weight(1f), cardBgColor, lightGreen) { onBackspace() }
                    } else {
                        KeyboardButton(key, Modifier.weight(1f), cardBgColor, lightGreen) { onKeyPressed(key) }
                    }
                }
            }
        }
    }
}
fun isValidForType(input: String, type: NumberType): Boolean {
    if (input.isBlank()) return true // Empty input isn't an error

    return try {
        when (type) {
            NumberType.BINARY -> {
                if (!input.all { it == '0' || it == '1' }) return false
                input.toLong(2) // Checks if it's too big
                true
            }
            NumberType.OCTAL -> {
                if (!input.all { it in '0'..'7' }) return false
                input.toLong(8) // Checks if it's too big
                true
            }
            NumberType.DECIMAL -> {
                if (!input.all { it.isDigit() }) return false
                input.toLong() // Checks if it's too big
                true
            }
            NumberType.HEXADECIMAL -> {
                if (!input.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) return false
                input.toLong(16) // Checks if it's too big
                true
            }
            NumberType.ROMAN -> {
                // Strict Roman numeral check (also inherently limits it to 3999)
                val romanRegex = Regex("^M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$")
                input.uppercase().matches(romanRegex)
            }
        }
    } catch (_: Exception) {
        // If toLong() fails because the number is too massive, we catch it here!
        false
    }
}