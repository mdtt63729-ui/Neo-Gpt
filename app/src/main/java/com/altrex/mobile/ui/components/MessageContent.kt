package com.altrex.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altrex.mobile.ui.theme.AltrexColors

/**
 * Renders an assistant message body from a lightweight subset of markdown:
 * fenced code blocks (```lang ... ```), headings (#, ##, ###), unordered lists
 * (-, *), ordered lists (1.), inline `code`, **bold** and *italic* spans.
 *
 * Built on basic Text composables (no third-party markdown library).
 */
@Composable
fun MessageContent(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block -> renderBlock(block) }
    }
}

// ---------- Block model ----------

private sealed class Block {
    data class Code(val language: String, val code: String) : Block()
    data class Heading(val level: Int, val text: String) : Block()
    data class Paragraph(val spans: List<InlineSpan>) : Block()
    data class ListItem(val ordered: Boolean, val index: Int, val spans: List<InlineSpan>) : Block()
    data object SpacerBlock : Block()
}

private sealed class InlineSpan {
    data class Text(val text: String) : InlineSpan()
    data class Bold(val text: String) : InlineSpan()
    data class Italic(val text: String) : InlineSpan()
    data class Code(val text: String) : InlineSpan()
}

// ---------- Parser ----------

private fun parseMarkdown(src: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val lines = src.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        // Fenced code block
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeBuf = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeBuf.append(lines[i]).append('\n')
                i++
            }
            // consume closing fence
            if (i < lines.size) i++
            blocks.add(Block.Code(lang, codeBuf.toString().trimEnd('\n')))
            continue
        }

        // Blank line
        if (line.isBlank()) {
            blocks.add(Block.SpacerBlock)
            i++
            continue
        }

        // Headings
        val headingMatch = Regex("^(#{1,6})\\s+(.*)$").matchEntire(line)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            blocks.add(Block.Heading(level, headingMatch.groupValues[2].trim()))
            i++
            continue
        }

        // Ordered list item
        val orderedMatch = Regex("^(\\s*)(\\d+)\\.\\s+(.*)$").matchEntire(line)
        if (orderedMatch != null) {
            val idx = orderedMatch.groupValues[2].toIntOrNull() ?: 1
            blocks.add(Block.ListItem(true, idx, parseInline(orderedMatch.groupValues[3])))
            i++
            continue
        }

        // Unordered list item
        val unorderedMatch = Regex("^\\s*[-*+]\\s+(.*)$").matchEntire(line)
        if (unorderedMatch != null) {
            blocks.add(Block.ListItem(false, 0, parseInline(unorderedMatch.groupValues[1])))
            i++
            continue
        }

        // Paragraph (consume contiguous non-blank lines)
        val para = StringBuilder(line)
        i++
        while (i < lines.size && lines[i].isNotBlank() &&
            !lines[i].trimStart().startsWith("```") &&
            !lines[i].trimStart().startsWith("#") &&
            !lines[i].trimStart().matches(Regex("\\s*[-*+]\\s+.*")) &&
            !lines[i].trimStart().matches(Regex("\\s*\\d+\\.\\s+.*"))
        ) {
            para.append(' ').append(lines[i].trim())
            i++
        }
        blocks.add(Block.Paragraph(parseInline(para.toString())))
    }
    return blocks
}

private fun parseInline(src: String): List<InlineSpan> {
    val spans = mutableListOf<InlineSpan>()
    val regex = Regex("""(\*\*([^*]+)\*\*|\*([^*]+)\*|`([^`]+)`)""")
    var last = 0
    for (m in regex.findAll(src)) {
        if (m.range.first > last) {
            spans.add(InlineSpan.Text(src.substring(last, m.range.first)))
        }
        when {
            m.groupValues[2].isNotEmpty() -> spans.add(InlineSpan.Bold(m.groupValues[2]))
            m.groupValues[3].isNotEmpty() -> spans.add(InlineSpan.Italic(m.groupValues[3]))
            m.groupValues[4].isNotEmpty() -> spans.add(InlineSpan.Code(m.groupValues[4]))
        }
        last = m.range.last + 1
    }
    if (last < src.length) spans.add(InlineSpan.Text(src.substring(last)))
    return spans
}

// ---------- Rendering ----------

@Composable
private fun renderBlock(block: Block) {
    when (block) {
        is Block.Code -> CodeBlock(
            code = block.code,
            language = block.language,
            modifier = Modifier.fillMaxWidth()
        )
        is Block.Heading -> {
            val (size, weight) = when (block.level) {
                1 -> 18.sp to FontWeight.Bold
                2 -> 16.sp to FontWeight.SemiBold
                3 -> 14.5.sp to FontWeight.SemiBold
                else -> 13.5.sp to FontWeight.Medium
            }
            Text(
                text = block.text,
                color = AltrexColors.textPrimary,
                fontSize = size,
                fontWeight = weight,
                modifier = Modifier.padding(top = 2.dp, bottom = 1.dp)
            )
        }
        is Block.Paragraph -> Text(
            text = buildInlineAnnotated(block.spans),
            color = AltrexColors.textPrimary,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        is Block.ListItem -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (block.ordered) "${block.index}." else "•",
                color = AltrexColors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = buildInlineAnnotated(block.spans),
                color = AltrexColors.textPrimary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.weight(1f)
            )
        }
        Block.SpacerBlock -> Spacer(Modifier.padding(2.dp))
    }
}

private fun buildInlineAnnotated(spans: List<InlineSpan>): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        when (span) {
            is InlineSpan.Text -> append(span.text)
            is InlineSpan.Bold -> withStyle(
                SpanStyle(fontWeight = FontWeight.SemiBold, color = AltrexColors.textPrimary)
            ) { append(span.text) }
            is InlineSpan.Italic -> withStyle(
                SpanStyle(fontStyle = FontStyle.Italic)
            ) { append(span.text) }
            is InlineSpan.Code -> {
                withStyle(
                    SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = AltrexColors.bgSurface,
                        color = AltrexColors.accent
                    )
                ) { append(" ${span.text} ") }
            }
        }
    }
}
