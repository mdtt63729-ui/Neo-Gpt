package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFontFamily = FontFamily(Font(R.font.inter))
val JosefinSansFontFamily = FontFamily(Font(R.font.josefin_sans))

fun getAppTypography(fontName: String): Typography {
    val family = if (fontName == "Josefin Sans") JosefinSansFontFamily else InterFontFamily
    return Typography(
        displayLarge = TextStyle(fontFamily = family),
        displayMedium = TextStyle(fontFamily = family),
        displaySmall = TextStyle(fontFamily = family),
        headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold),
        headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold),
        titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold),
        bodyLarge = TextStyle(fontFamily = family, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = family, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold),
        labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold)
    )
}
