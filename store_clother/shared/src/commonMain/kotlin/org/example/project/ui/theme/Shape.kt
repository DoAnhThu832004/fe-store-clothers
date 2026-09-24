package org.example.project.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape system của Store Clother.
 *
 * Dùng bo góc lớn hơn default Material 3 để tạo cảm giác
 * mềm mại, hiện đại — phù hợp với app thời trang bán lẻ.
 *
 * Scale:
 *  extraSmall  4dp  → Badge, chip nhỏ
 *  small       8dp  → Input field nhỏ, tag
 *  medium      12dp → Card, dialog nội dung
 *  large       16dp → Bottom sheet, card lớn
 *  extraLarge  28dp → Modal bottom sheet, FAB
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
