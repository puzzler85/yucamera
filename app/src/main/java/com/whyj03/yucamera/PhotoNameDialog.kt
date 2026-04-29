package com.whyj03.yucamera

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoNameDialog(
    prefix: String,
    prefixCounter: Int,
    onPrefixChange: (String) -> Unit,
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultName = remember {
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    var prefixField by remember(prefix) { mutableStateOf(prefix) }

    val prefixActive = prefixField.trim().isNotBlank()
    val previewName = if (prefixActive) {
        "${prefixField.trim()}_${prefixCounter.toString().padStart(4, '0')}"
    } else null

    var nameField by remember {
        mutableStateOf(TextFieldValue(defaultName, selection = TextRange(0, defaultName.length)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("사진 저장") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Prefix section
                Text("Prefix 설정", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = prefixField,
                    onValueChange = { prefixField = it },
                    label = { Text("Prefix (선택)") },
                    placeholder = { Text("예: beach, trip") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (prefixField.isNotEmpty()) {
                            IconButton(onClick = { prefixField = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "초기화", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    supportingText = {
                        if (prefixActive) Text("prefix 변경 시 번호가 1부터 다시 시작됩니다")
                    }
                )

                HorizontalDivider()

                // Name section
                if (prefixActive) {
                    // Prefix mode: show auto-generated name preview
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("저장될 파일명", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$previewName.jpg",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Manual mode
                    OutlinedTextField(
                        value = nameField,
                        onValueChange = { nameField = it },
                        label = { Text("파일명 (.jpg 자동 추가)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Save prefix first (fires counter reset if prefix changed)
                    onPrefixChange(prefixField)
                    // Pass empty name to trigger prefix-based naming in ViewModel,
                    // or pass manual name if no prefix
                    val nameToSave = if (prefixActive) "" else nameField.text.trim().ifBlank { defaultName }
                    onConfirm(nameToSave)
                }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
