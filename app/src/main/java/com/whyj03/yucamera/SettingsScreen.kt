package com.whyj03.yucamera

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val config by viewModel.nasConfig.collectAsState()
    val shareListState by viewModel.shareListState.collectAsState()

    var host by remember(config.host) { mutableStateOf(config.host) }
    var port by remember(config.port) { mutableStateOf(config.port.toString()) }
    var username by remember(config.username) { mutableStateOf(config.username) }
    var password by remember(config.password) { mutableStateOf(config.password) }
    var shareName by remember(config.shareName) { mutableStateOf(config.shareName) }
    var remotePath by remember(config.remotePath) { mutableStateOf(config.remotePath) }
    var showPassword by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("NAS 연결 설정", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it; saved = false },
                label = { Text("NAS 주소") },
                placeholder = { Text("192.168.1.100") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                supportingText = { Text("IP 주소 또는 호스트명") }
            )
            OutlinedTextField(
                value = port,
                onValueChange = { v -> if (v.length <= 5 && v.all { it.isDigit() }) { port = v; saved = false } },
                label = { Text("포트") },
                placeholder = { Text("445") },
                modifier = Modifier.width(90.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text("기본 445") }
            )
        }

        OutlinedTextField(
            value = shareName,
            onValueChange = { shareName = it; saved = false },
            label = { Text("공유 폴더명 (Share Name)") },
            placeholder = { Text("photos") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("NAS에서 공유 설정한 폴더명") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (shareListState is ShareListState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            TextButton(
                onClick = { viewModel.loadShareList(host, port.toIntOrNull() ?: 445, username, password) },
                enabled = host.isNotBlank() && shareListState !is ShareListState.Loading
            ) {
                Text("목록 불러오기")
            }
        }

        if (shareListState is ShareListState.Error) {
            Text(
                text = (shareListState as ShareListState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (shareListState is ShareListState.Success) {
            val shares = (shareListState as ShareListState.Success).shares
            var customInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.resetShareListState() },
                title = { Text("공유 폴더 선택") },
                text = {
                    Column {
                        if (shares.isEmpty()) {
                            Text("자동으로 찾은 공유 폴더가 없습니다. 아래에 직접 입력하세요.")
                        } else {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)) {
                                shares.forEach { share ->
                                    TextButton(
                                        onClick = {
                                            shareName = share
                                            saved = false
                                            viewModel.resetShareListState()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(share, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customInput,
                                onValueChange = { customInput = it },
                                label = { Text("직접 입력") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            TextButton(
                                onClick = {
                                    if (customInput.isNotBlank()) {
                                        shareName = customInput.trim()
                                        saved = false
                                        viewModel.resetShareListState()
                                    }
                                },
                                enabled = customInput.isNotBlank()
                            ) {
                                Text("확인")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetShareListState() }) { Text("닫기") }
                }
            )
        }

        OutlinedTextField(
            value = remotePath,
            onValueChange = { remotePath = it; saved = false },
            label = { Text("저장 경로 (선택)") },
            placeholder = { Text("/mobile/uploads") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("공유 폴더 내 하위 경로. 비워두면 루트에 저장") }
        )

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Text("인증 정보", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; saved = false },
            label = { Text("사용자명") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; saved = false },
            label = { Text("비밀번호") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            }
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                viewModel.updateNasConfig(
                    NasConfig(host.trim(), port.toIntOrNull() ?: 445, username, password, shareName.trim(), remotePath.trim())
                )
                saved = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = host.isNotBlank() && shareName.isNotBlank()
        ) {
            Text("설정 저장")
        }

        if (saved) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text("✓ 설정이 저장되었습니다", modifier = Modifier.padding(12.dp))
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Text("연결 안내", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "• 프로토콜: SMB / CIFS (기본 포트 445)\n" +
                    "• NAS와 같은 Wi-Fi 네트워크에 연결되어야 합니다\n" +
                    "• Synology, QNAP, Windows 공유 폴더 지원\n" +
                    "• 익명 접속: 사용자명/비밀번호 비워두기",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
