package com.example.anm2editor.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

data class ProjectPreset(
    val title: String,
    val description: String,
    val type: PresetType
)

enum class PresetType {
    ISAAC_CHARACTER,
    TEAR_PROJECTILE,
    ITEM_PEDESTAL,
    EMPTY
}

@Composable
fun PresetsDialog(
    onSelectPreset: (PresetType) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(
        ProjectPreset(
            title = "Персонаж Айзек (Полный сетап)",
            description = "Готовый актёр Айзека с Головой, Телом, точкой слезы (TearPoint), анимациями ходьбы (WalkDown, WalkSide), стрельбы и урона.",
            type = PresetType.ISAAC_CHARACTER
        ),
        ProjectPreset(
            title = "Снаряд слезы",
            description = "Анимация полёта слезы с плавной интерполяцией масштабирования и триггером всплеска при попадании.",
            type = PresetType.TEAR_PROJECTILE
        ),
        ProjectPreset(
            title = "Пьедестал и предмет",
            description = "Алтарь пьедестала с плавно парящим коллекционным артефактом с интерполяцией кадров.",
            type = PresetType.ITEM_PEDESTAL
        ),
        ProjectPreset(
            title = "Новый чистый проект",
            description = "Пустой холст с одним слоем и 8-кадровой последовательностью для создания анимации с нуля.",
            type = PresetType.EMPTY
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = IsaacPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Шаблоны проектов", color = IsaacBone)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IsaacSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectPreset(preset.type)
                                onDismiss()
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(preset.title, style = MaterialTheme.typography.titleSmall, color = IsaacPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(preset.description, style = MaterialTheme.typography.bodySmall, color = IsaacBone, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = IsaacOnSurfaceMuted)
            }
        },
        containerColor = IsaacSurface
    )
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = IsaacGold)
                Spacer(Modifier.width(8.dp))
                Text("Руководство Isaac ANM2", color = IsaacBone)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Управление и возможности как на ПК:",
                    fontWeight = FontWeight.Bold,
                    color = IsaacPrimary,
                    fontSize = 13.sp
                )
                Text(
                    text = "• Холст: масштабируйте двумя пальцами и двигайте холст. Переключите инструмент справа вверху на 'Перемещение слоя' или 'Pivot', чтобы двигать спрайты пальцем прямо на экране!\n" +
                            "• Нижняя панель: чтобы освободить место на экране, используйте кнопки 'Скрыть' или 'Компактно' в верхней полосе панели инспектора.\n" +
                            "• Выбор с листа: нажмите 'Выбрать с листа' у любого кадра для визуального выбора спрайта 16x16, 32x32 или произвольной области.\n" +
                            "• Интерполяция кадров: включайте интерполяцию для плавных переходов позиции, масштаба, поворота и цвета между кадрами.\n" +
                            "• Калька (Onion Skin): просмотр призраков предыдущего (красный) и следующего (синий) кадров.\n" +
                            "• Импорт / Экспорт: открывайте любые существующие файлы .anm2 из памяти устройства и экспортируйте файлы прямо в папку мода Isaac!",
                    fontSize = 11.sp,
                    color = IsaacBone,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = IsaacPrimary)) {
                Text("Понятно!", color = Color(0xFF002244))
            }
        },
        containerColor = IsaacSurface
    )
}
