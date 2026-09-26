package com.example.anm2editor.ui.xmleditor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun Anm2XmlViewerModal(
    xmlContent: String,
    onApplyXml: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var editableXml by remember { mutableStateOf(xmlContent) }
    var isEditMode by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, IsaacPrimary, RoundedCornerShape(12.dp)),
            color = IsaacSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditMode) "Редактирование ANM2 XML" else "Исходный XML-код ANM2",
                        style = MaterialTheme.typography.titleMedium,
                        color = IsaacBone
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(editableXml))
                                Toast.makeText(context, "XML скопирован в буфер обмена!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Копировать XML", tint = IsaacPrimary)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = IsaacBone)
                        }
                    }
                }

                // Mode switch
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isEditMode,
                        onClick = { isEditMode = false },
                        label = { Text("Просмотр кода", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = isEditMode,
                        onClick = { isEditMode = true },
                        label = { Text("Редактировать / Вставить XML", fontSize = 11.sp) }
                    )
                }

                // XML Display or Text Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(IsaacBackground)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    if (isEditMode) {
                        OutlinedTextField(
                            value = editableXml,
                            onValueChange = { editableXml = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = IsaacBone
                            )
                        )
                    } else {
                        val verticalScroll = rememberScrollState()
                        val horizontalScroll = rememberScrollState()
                        Text(
                            text = editableXml,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(verticalScroll)
                                .horizontalScroll(horizontalScroll),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFC5E1A5)
                        )
                    }
                }

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Закрыть")
                    }
                    if (isEditMode) {
                        Button(
                            onClick = {
                                try {
                                    onApplyXml(editableXml)
                                    Toast.makeText(context, "XML успешно применён!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Ошибка парсинга XML: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IsaacPrimary)
                        ) {
                            Text("Применить XML", color = Color(0xFF002244), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
