package com.example.calculatingpaper.view.components
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.calculatingpaper.R
import com.example.calculatingpaper.data.AppPreferences
import kotlinx.coroutines.delay
@Composable
fun MathKeyboard(
    onKeyPress: (String) -> Unit,
    onSpecialKeyPress: (String) -> Unit,
    onCalculate: () -> Unit,
    onCloseKeyboard: () -> Unit,
    onToggleMode: (Boolean) -> Unit,
    isDegreesMode: MutableState<Boolean>,
    keyboardController: SoftwareKeyboardController?,
    onToggleToSystemKeyboard: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val buttonSize = (screenWidth / 8).coerceIn(40.dp, 60.dp)
    val iconSize = (buttonSize.value * 0.8f).dp
    val rowSpacing = (buttonSize.value * 0.2f).dp
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressKey by remember { mutableStateOf("") }
    var showInfinitePrecisionDialog by remember { mutableStateOf(false) }
    var decimalPrecisionText by remember { mutableStateOf(AppPreferences.decimalPrecision.toString()) }
    var showInvalidInputError by remember { mutableStateOf(false) }
    var showHighPrecisionWarning by remember { mutableStateOf(false) }
    fun handleSavePrecision() {
        val precision = decimalPrecisionText.toIntOrNull()
        when {
            precision == null || precision < 0 -> {
                showInvalidInputError = true
            }
            precision > 50 -> {
                showHighPrecisionWarning = true
            }
            else -> {
                appPreferences.saveDecimalPrecision(precision)
                Toast.makeText(context, "Precision updated", Toast.LENGTH_SHORT).show()
                showInfinitePrecisionDialog = false
            }
        }
    }
    if (showInfinitePrecisionDialog) {
        AlertDialog(
            onDismissRequest = { showInfinitePrecisionDialog = false },
            title = { Text("Calculation Precision") },
            text = {
                Column {
                    Text("Number of decimal places in calculations")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = decimalPrecisionText,
                        onValueChange = { newValue ->
                            decimalPrecisionText = newValue.filter { it.isDigit() }
                            showInvalidInputError = false
                        },
                        label = { Text("Decimal places") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showInvalidInputError,
                        supportingText = { if (showInvalidInputError) Text("Please enter a valid positive number") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { handleSavePrecision() }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfinitePrecisionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showHighPrecisionWarning) {
        AlertDialog(
            onDismissRequest = { showHighPrecisionWarning = false },
            title = { Text("High Precision Warning") },
            text = { Text("High precision above 50 may cause slow calculations or crashes. You can lower it later if needed.") },
            confirmButton = {
                Button({
                    showHighPrecisionWarning = false
                    val precision = decimalPrecisionText.toIntOrNull()
                    if (precision != null) {
                        appPreferences.saveDecimalPrecision(precision)
                        Toast.makeText(context, "Precision updated", Toast.LENGTH_SHORT).show()
                        showInfinitePrecisionDialog = false
                    } else {
                        showInvalidInputError = true
                    }
                }) { Text("OK") }
            },
            dismissButton = { Button(onClick = { showHighPrecisionWarning = false }) { Text("Cancel") } }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(top = 8.dp, bottom = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            decimalPrecisionText = AppPreferences.decimalPrecision.toString()
                            showInvalidInputError = false
                            showInfinitePrecisionDialog = true
                        }
                        .padding(horizontal = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.infinitep),
                        contentDescription = "Infinite Precision",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(buttonSize)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
                listOf(
                    R.drawable.arrow_left to "←",
                    R.drawable.arrow_right to "→",
                    R.drawable.next_line to "↵",
                    R.drawable.deletee to "⌫"
                ).forEachIndexed { index, (imageRes, key) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(key) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        isLongPressing = true
                                        longPressKey = key
                                        tryAwaitRelease()
                                        isLongPressing = false
                                        longPressKey = ""
                                    },
                                    onTap = { offset -> onSpecialKeyPress(key) }
                                )
                            }
                            .padding(horizontal = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = imageRes),
                            contentDescription = key,
                            modifier = Modifier.size(iconSize),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (index < 3) {
                        Divider(
                            modifier = Modifier
                                .width(1.dp)
                                .height(buttonSize)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(rowSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrigButton(
                    imageRes = R.drawable.sin_arcsin,
                    primaryLabel = "sin(",
                    secondaryLabel = "arcsin(",
                    onPrimaryClick = { onKeyPress("sin(") },
                    onSecondaryClick = { onKeyPress("arcsin(") },
                    buttonSize = buttonSize,
                    iconSize = iconSize
                )
                TrigButton(
                    imageRes = R.drawable.cos_arccos,
                    primaryLabel = "cos(",
                    secondaryLabel = "arccos(",
                    onPrimaryClick = { onKeyPress("cos(") },
                    onSecondaryClick = { onKeyPress("arccos(") },
                    buttonSize = buttonSize,
                    iconSize = iconSize
                )
                TrigButton(
                    imageRes = R.drawable.tan_arctan,
                    primaryLabel = "tan(",
                    secondaryLabel = "arctan(",
                    onPrimaryClick = { onKeyPress("tan(") },
                    onSecondaryClick = { onKeyPress("arctan(") },
                    buttonSize = buttonSize,
                    iconSize = iconSize
                )
                KeyboardButton(
                    imageRes = R.drawable.cot,
                    onClick = { onKeyPress("cot(") },
                    buttonSize = buttonSize,
                    iconSize = iconSize
                )
                KeyboardButton(
                    imageRes = R.drawable.sec,
                    onClick = { onKeyPress("sec(") },
                    buttonSize = buttonSize,
                    iconSize = iconSize
                )
                KeyboardButton(
                    imageRes = R.drawable.csc,
                    onClick = { onKeyPress("csc(") },
                    buttonSize = buttonSize,
                    iconSize = iconSize
                )
                KeyboardButton(
                    imageRes = R.drawable.pi,
                    onClick = { onKeyPress("π") },
                    buttonSize = buttonSize,
                    iconSize = iconSize
                )
            }
            Spacer(modifier = Modifier.height(rowSpacing))
            val row1Keys = listOf(
                R.drawable.exp to "exp(", R.drawable.power to "^", R.drawable.absolute to "abs(",
                R.drawable.seven to "7", R.drawable.eight to "8", R.drawable.nine to "9", R.drawable.divide to "/"
            )
            val row2Keys = listOf(
                R.drawable.e to "e^", R.drawable.root to "√(", R.drawable.left_parentheses to "(",
                R.drawable.four to "4", R.drawable.five to "5", R.drawable.six to "6", R.drawable.multiplication to "*"
            )
            val row3KeyData = listOf(
                R.drawable.log_basetwo to "log₂(",
                R.drawable.right_parentheses to ")",
                R.drawable.one to "1",
                R.drawable.two to "2",
                R.drawable.three to "3",
                R.drawable.subtraction to "-"
            )
            val row4Keys = listOf(
                R.drawable.ln to "ln(", R.drawable.log_base10 to "log₁₀(", R.drawable.percent to "%",
                R.drawable.dot to ".", R.drawable.zero to "0", R.drawable.equal to "=", R.drawable.plus to "+"
            )
            MathKeyRow(keys = row1Keys, onKeyPress = onKeyPress, buttonSize = buttonSize, iconSize = iconSize)
            Spacer(modifier = Modifier.height(rowSpacing))
            MathKeyRow(keys = row2Keys, onKeyPress = onKeyPress, buttonSize = buttonSize, iconSize = iconSize)
            Spacer(modifier = Modifier.height(rowSpacing))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrigButton(
                    imageRes = R.drawable.x_func,
                    primaryLabel = "x",
                    secondaryLabel = "f(x)",
                    onPrimaryClick = { onKeyPress("x") },
                    onSecondaryClick = { onKeyPress("f(x)") },
                    buttonSize = buttonSize,
                    iconSize = iconSize
                )
                row3KeyData.forEach { (imageRes, key) ->
                    KeyboardButton(
                        imageRes = imageRes,
                        onClick = { onKeyPress(key) },
                        buttonSize = buttonSize,
                        iconSize = iconSize
                    )
                }
            }
            Spacer(modifier = Modifier.height(rowSpacing))
            MathKeyRow(keys = row4Keys, onKeyPress = onKeyPress, buttonSize = buttonSize, iconSize = iconSize)
            Spacer(modifier = Modifier.height(rowSpacing))
            Spacer(modifier = Modifier.height(8.dp))
            BottomKeyRow(
                isDegreesMode = isDegreesMode,
                keyboardController = keyboardController,
                onCloseKeyboard = onCloseKeyboard,
                onKeyPress = onKeyPress,
                onCalculate = onCalculate,
                onToggleToSystemKeyboard = onToggleToSystemKeyboard,
                onToggleMode = onToggleMode,
                buttonSize = buttonSize,
                iconSize = iconSize
            )
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrigButton(
    imageRes: Int,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    buttonSize: Dp,
    iconSize: Dp
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .size(buttonSize)
            .combinedClickable(
                onClick = onPrimaryClick,
                onDoubleClick = onSecondaryClick,
                onLongClick = onSecondaryClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
fun KeyboardButton(
    imageRes: Int,
    onClick: () -> Unit,
    buttonSize: Dp,
    iconSize: Dp
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .size(buttonSize)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
fun MathKeyRow(
    keys: List<Pair<Int, String>>,
    onKeyPress: (String) -> Unit,
    buttonSize: Dp,
    iconSize: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        keys.forEach { (imageRes, key) ->
            KeyboardButton(
                imageRes = imageRes,
                onClick = { onKeyPress(key) },
                buttonSize = buttonSize,
                iconSize = iconSize
            )
        }
    }
}
@Composable
fun RowScope.KeyboardIconButton(
    weight: Float,
    onClick: () -> Unit,
    iconRes: Int,
    contentDescription: String,
    iconSize: Dp = 45.dp
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun BottomKeyRow(
    isDegreesMode: MutableState<Boolean>,
    keyboardController: SoftwareKeyboardController?,
    onCloseKeyboard: () -> Unit,
    onKeyPress: (String) -> Unit,
    onCalculate: () -> Unit,
    onToggleToSystemKeyboard: () -> Unit,
    onToggleMode: (Boolean) -> Unit,
    buttonSize: Dp,
    iconSize: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        KeyboardIconButton(
            weight = 20f,
            onClick = {
                val newMode = !isDegreesMode.value
                isDegreesMode.value = newMode
                onToggleMode(newMode)
            },
            iconRes = if (isDegreesMode.value) R.drawable.deg else R.drawable.rad,
            contentDescription = "Toggle Degree/Radian Mode",
            iconSize = iconSize
        )
        KeyboardIconButton(
            weight = 20f,
            onClick = onToggleToSystemKeyboard,
            iconRes = R.drawable.abc,
            contentDescription = "Switch to System Keyboard",
            iconSize = iconSize
        )
        KeyboardIconButton(
            weight = 40f,
            onClick = { onKeyPress(" ") },
            iconRes = R.drawable.space,
            contentDescription = "Space",
            iconSize = iconSize
        )
        KeyboardIconButton(
            weight = 20f,
            onClick = onCloseKeyboard,
            iconRes = R.drawable.arrow_down,
            contentDescription = "Close Math Keyboard",
            iconSize = iconSize
        )
        KeyboardIconButton(
            weight = 20f,
            onClick = onCalculate,
            iconRes = R.drawable.calculate,
            contentDescription = "Calculate",
            iconSize = iconSize
        )
    }
}