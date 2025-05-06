package com.example.calculatingpaper.view.components
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.calculatingpaper.R
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
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressKey by remember { mutableStateOf("") }
//    LaunchedEffect(isLongPressing) {
//        var delayTime = 500L
//        val minDelayTime = 50L
//        val acceleration = 0.9f
//        while (isLongPressing) {
//            when (longPressKey) {
//                //"⌫" -> onSpecialKeyPress("⌫")
//                "←" -> onSpecialKeyPress("←")
//                "→" -> onSpecialKeyPress("→")
//            }
//            delay(delayTime)
//            delayTime = (delayTime * acceleration).toLong().coerceAtLeast(minDelayTime)
//        }
//    }
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
                                        val released = tryAwaitRelease()
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
                            modifier = Modifier.size(50.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (index < 3) {
                        Divider(
                            modifier = Modifier
                                .width(1.dp)
                                .height(50.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrigButton(
                    imageRes = R.drawable.sin_arcsin,
                    primaryLabel = "sin(",
                    secondaryLabel = "arcsin(",
                    onPrimaryClick = { onKeyPress("sin(") },
                    onSecondaryClick = { onKeyPress("arcsin(") }
                )
                TrigButton(
                    imageRes = R.drawable.cos_arccos,
                    primaryLabel = "cos(",
                    secondaryLabel = "arccos(",
                    onPrimaryClick = { onKeyPress("cos(") },
                    onSecondaryClick = { onKeyPress("arccos(") }
                )
                TrigButton(
                    imageRes = R.drawable.tan_arctan,
                    primaryLabel = "tan(",
                    secondaryLabel = "arctan(",
                    onPrimaryClick = { onKeyPress("tan(") },
                    onSecondaryClick = { onKeyPress("arctan(") }
                )
                KeyboardButton(imageRes = R.drawable.cot, onClick = { onKeyPress("cot(") })
                KeyboardButton(imageRes = R.drawable.sec, onClick = { onKeyPress("sec(") })
                KeyboardButton(imageRes = R.drawable.csc, onClick = { onKeyPress("csc(") })
                KeyboardButton(imageRes = R.drawable.pi, onClick = { onKeyPress("π") })
            }
            Spacer(modifier = Modifier.height(8.dp))
            MathKeyRow(
                listOf(
                    R.drawable.exp to "exp(", R.drawable.power to "^", R.drawable.absolute to "abs(",
                    R.drawable.seven to "7", R.drawable.eight to "8", R.drawable.nine to "9", R.drawable.divide to "/"
                ), onKeyPress
            )
            MathKeyRow(
                listOf(
                    R.drawable.e to "e^", R.drawable.root to "√(", R.drawable.left_parentheses to "(",
                    R.drawable.four to "4", R.drawable.five to "5", R.drawable.six to "6", R.drawable.multiplication to "*"
                ), onKeyPress
            )
            MathKeyRow(
                listOf(
                    R.drawable.function to "f(x)", R.drawable.log_basetwo to "log₂(", R.drawable.right_parentheses to ")",
                    R.drawable.one to "1", R.drawable.two to "2", R.drawable.three to "3", R.drawable.subtraction to "-"
                ), onKeyPress
            )
            MathKeyRow(
                listOf(
                    R.drawable.ln to "ln(", R.drawable.log_base10 to "log₁₀(", R.drawable.percent to "%",
                    R.drawable.dot to ".", R.drawable.zero to "0", R.drawable.equal to "=", R.drawable.plus to "+"
                ), onKeyPress
            )
            Spacer(modifier = Modifier.height(8.dp))
            BottomKeyRow(
                isDegreesMode = isDegreesMode,
                keyboardController = keyboardController,
                onCloseKeyboard = onCloseKeyboard,
                onKeyPress = onKeyPress,
                onCalculate = onCalculate,
                onToggleToSystemKeyboard = onToggleToSystemKeyboard,
                onToggleMode = onToggleMode
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
    onSecondaryClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .size(48.dp)
            .combinedClickable(
                onClick = onPrimaryClick,
                onDoubleClick = onSecondaryClick
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
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
fun KeyboardButton(
    imageRes: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .size(48.dp)
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
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
fun MathKeyRow(keys: List<Pair<Int, String>>, onKeyPress: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        keys.forEach { (imageRes, key) ->
            val action = if (key == "=") {
                { onKeyPress(key) }
            } else {
                { onKeyPress(key) }
            }
            KeyboardButton(
                imageRes = imageRes,
                onClick = action
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
    onToggleMode: (Boolean) -> Unit
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
            contentDescription = "Toggle Degree/Radian Mode"
        )
        KeyboardIconButton(
            weight = 20f,
            onClick = onToggleToSystemKeyboard,
            iconRes = R.drawable.abc,
            contentDescription = "Switch to System Keyboard"
        )
        KeyboardIconButton(
            weight = 40f,
            onClick = { onKeyPress(" ") },
            iconRes = R.drawable.space,
            contentDescription = "Space"
        )
        KeyboardIconButton(
            weight = 20f,
            onClick = onCloseKeyboard,
            iconRes = R.drawable.arrow_down,
            contentDescription = "Close Math Keyboard"
        )
        KeyboardIconButton(
            weight = 20f,
            onClick = onCalculate,
            iconRes = R.drawable.calculate,
            contentDescription = "Calculate"
        )
    }
}