package com.example.elite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elite.sourceviewer.AsmFileLine
import com.example.elite.sourceviewer.SourceRepository
import kotlinx.coroutines.launch

@Composable
fun SourceInspectorView(
    repository: SourceRepository,
    modifier: Modifier = Modifier
) {
    var selectedFile by remember { mutableStateOf(repository.availableFiles[0]) }
    var currentStartLine by remember { mutableStateOf(17520) } // Default to TT54
    var lines by remember { mutableStateOf<List<AsmFileLine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun loadLines(file: String, startLine: Int) {
        isLoading = true
        coroutineScope.launch {
            lines = repository.loadFileSlice(file, startLine, 120)
            isLoading = false
        }
    }

    LaunchedEffect(selectedFile, currentStartLine) {
        loadLines(selectedFile, currentStartLine)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030704))
            .padding(6.dp)
            .testTag("source_inspector_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "6502 ASM SOURCE INSPECTOR",
                color = BBC_YELLOW,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "LINES: $currentStartLine - ${currentStartLine + lines.size}",
                color = BBC_CYAN,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Subroutine Quick Jump Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(repository.bookmarks) { bm ->
                Button(
                    onClick = {
                        selectedFile = repository.availableFiles[0] // elite-source.asm
                        currentStartLine = bm.line
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStartLine == bm.line) Color(0xFF005522) else Color(0xFF14241B)
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(
                        text = bm.label,
                        color = if (currentStartLine == bm.line) BBC_YELLOW else BBC_GREEN,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Navigation Line Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B140E))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedFile.substringAfterLast("/"),
                color = BBC_WHITE,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { currentStartLine = (currentStartLine - 80).coerceAtLeast(1) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2E22)),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                ) {
                    Text("-80", fontSize = 8.sp, color = BBC_GREEN)
                }
                Button(
                    onClick = { currentStartLine += 80 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2E22)),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                ) {
                    Text("+80", fontSize = 8.sp, color = BBC_GREEN)
                }
            }
        }

        // Code Viewer Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 2.dp)
                .background(Color(0xFF000000))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = BBC_GREEN,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val horizontalScroll = rememberScrollState()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScroll)
                        .padding(4.dp)
                ) {
                    items(lines) { line ->
                        Row {
                            // Line Number
                            Text(
                                text = "${line.lineNumber}".padStart(5, ' ') + "  ",
                                color = Color(0xFF446644),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            // Line Content
                            val textColor = when {
                                line.isLabel -> BBC_YELLOW
                                line.isComment -> Color(0xFF558855)
                                else -> Color(0xFFC0E0C0)
                            }
                            Text(
                                text = line.text,
                                color = textColor,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (line.isLabel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
