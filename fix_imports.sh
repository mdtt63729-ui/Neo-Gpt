#!/bin/bash
sed -i 's/import androidx.compose.runtime.\*/import androidx.compose.runtime.\*\nimport androidx.compose.animation.core.*\nimport androidx.compose.ui.draw.scale\nimport androidx.compose.material.icons.filled.ArrowDropDown/g' app/src/main/java/com/example/ui/chat/ChatScreen.kt
