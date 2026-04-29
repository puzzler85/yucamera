package com.whyj03.yucamera

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoNameDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val defaultName = remember {
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    var nameField by remember {
        mutableStateOf(TextFieldValue(defaultName, selection = TextRange(0, defaultName.length)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("사진 이름 입력") },
        text = {
            OutlinedTextField(
                value = nameField,
                onValueChange = { nameField = it },
                label = { Text("파일명 (.jpg 자동 추가)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nameField.text.trim().ifBlank { defaultName }) }) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
