package com.kwtsms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageUtilsTest {

    // ──────────────────────────────────────────────
    // Normal text preservation
    // ──────────────────────────────────────────────

    @Test
    fun `cleanMessage - preserves English text`() {
        assertEquals("Hello World", MessageUtils.cleanMessage("Hello World"))
    }

    @Test
    fun `cleanMessage - preserves Arabic text`() {
        val arabic = "\u0645\u0631\u062D\u0628\u0627 \u0628\u0627\u0644\u0639\u0627\u0644\u0645"
        assertEquals(arabic, MessageUtils.cleanMessage(arabic))
    }

    @Test
    fun `cleanMessage - preserves newlines`() {
        assertEquals("line1\nline2", MessageUtils.cleanMessage("line1\nline2"))
    }

    @Test
    fun `cleanMessage - preserves tabs`() {
        assertEquals("col1\tcol2", MessageUtils.cleanMessage("col1\tcol2"))
    }

    @Test
    fun `cleanMessage - preserves spaces`() {
        assertEquals("hello   world", MessageUtils.cleanMessage("hello   world"))
    }

    // ──────────────────────────────────────────────
    // Arabic digit conversion
    // ──────────────────────────────────────────────

    @Test
    fun `cleanMessage - converts Arabic-Indic digits`() {
        // ٠١٢٣٤٥٦٧٨٩
        assertEquals("0123456789", MessageUtils.cleanMessage("\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669"))
    }

    @Test
    fun `cleanMessage - converts Extended Arabic-Indic digits`() {
        // ۰۱۲۳۴۵۶۷۸۹
        assertEquals("0123456789", MessageUtils.cleanMessage("\u06F0\u06F1\u06F2\u06F3\u06F4\u06F5\u06F6\u06F7\u06F8\u06F9"))
    }

    @Test
    fun `cleanMessage - converts Arabic digits in mixed text`() {
        assertEquals("Your OTP is 1234", MessageUtils.cleanMessage("Your OTP is \u0661\u0662\u0663\u0664"))
    }

    // ──────────────────────────────────────────────
    // Emoji stripping
    // ──────────────────────────────────────────────

    @Test
    fun `cleanMessage - strips smiley emoji`() {
        assertEquals("Hello ", MessageUtils.cleanMessage("Hello \uD83D\uDE00"))
    }

    @Test
    fun `cleanMessage - strips heart emoji`() {
        assertEquals("Love ", MessageUtils.cleanMessage("Love \u2764"))
    }

    @Test
    fun `cleanMessage - strips transport emoji`() {
        assertEquals("Car ", MessageUtils.cleanMessage("Car \uD83D\uDE97"))
    }

    @Test
    fun `cleanMessage - strips flag components`() {
        // Regional indicator symbols U+1F1F0 U+1F1FC (Kuwait flag)
        assertEquals("Flag: ", MessageUtils.cleanMessage("Flag: \uD83C\uDDF0\uD83C\uDDFC"))
    }

    @Test
    fun `cleanMessage - strips dingbats`() {
        assertEquals("Check ", MessageUtils.cleanMessage("Check \u2714"))
    }

    @Test
    fun `cleanMessage - strips variation selectors`() {
        // U+2B50 is outside our emoji ranges, but U+FE0F (variation selector) is stripped
        assertEquals("Star \u2B50", MessageUtils.cleanMessage("Star \u2B50\uFE0F"))
    }

    @Test
    fun `cleanMessage - strips misc symbols`() {
        assertEquals("Sun ", MessageUtils.cleanMessage("Sun \u2600"))
    }

    @Test
    fun `cleanMessage - strips multiple emojis`() {
        assertEquals("Hello  World ", MessageUtils.cleanMessage("Hello \uD83D\uDE00 World \uD83C\uDF0D"))
    }

    // ──────────────────────────────────────────────
    // Hidden invisible characters
    // ──────────────────────────────────────────────

    @Test
    fun `cleanMessage - strips BOM`() {
        assertEquals("Hello", MessageUtils.cleanMessage("\uFEFFHello"))
    }

    @Test
    fun `cleanMessage - strips zero-width space`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u200Blo"))
    }

    @Test
    fun `cleanMessage - strips zero-width non-joiner`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u200Clo"))
    }

    @Test
    fun `cleanMessage - strips zero-width joiner`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u200Dlo"))
    }

    @Test
    fun `cleanMessage - strips word joiner`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u2060lo"))
    }

    @Test
    fun `cleanMessage - strips soft hyphen`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u00ADlo"))
    }

    @Test
    fun `cleanMessage - strips object replacement character`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\uFFFClo"))
    }

    // ──────────────────────────────────────────────
    // Directional formatting characters
    // ──────────────────────────────────────────────

    @Test
    fun `cleanMessage - strips LTR mark`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u200Elo"))
    }

    @Test
    fun `cleanMessage - strips RTL mark`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u200Flo"))
    }

    @Test
    fun `cleanMessage - strips LRE and RLE`() {
        assertEquals("Hello", MessageUtils.cleanMessage("\u202AHel\u202Blo"))
    }

    @Test
    fun `cleanMessage - strips directional isolates`() {
        assertEquals("Hello", MessageUtils.cleanMessage("\u2066Hel\u2069lo"))
    }

    // ──────────────────────────────────────────────
    // Control characters
    // ──────────────────────────────────────────────

    @Test
    fun `cleanMessage - strips null character`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u0000lo"))
    }

    @Test
    fun `cleanMessage - strips bell character`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u0007lo"))
    }

    @Test
    fun `cleanMessage - strips DEL character`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u007Flo"))
    }

    @Test
    fun `cleanMessage - strips C1 control characters`() {
        assertEquals("Hello", MessageUtils.cleanMessage("Hel\u0085lo"))
    }

    @Test
    fun `cleanMessage - preserves newline in controls`() {
        assertEquals("Hel\nlo", MessageUtils.cleanMessage("Hel\nlo"))
    }

    @Test
    fun `cleanMessage - preserves tab in controls`() {
        assertEquals("Hel\tlo", MessageUtils.cleanMessage("Hel\tlo"))
    }

    // ──────────────────────────────────────────────
    // HTML tags
    // ──────────────────────────────────────────────

    @Test
    fun `cleanMessage - strips simple HTML tags`() {
        assertEquals("Hello World", MessageUtils.cleanMessage("<b>Hello</b> World"))
    }

    @Test
    fun `cleanMessage - strips HTML with attributes`() {
        assertEquals("Link", MessageUtils.cleanMessage("<a href=\"url\">Link</a>"))
    }

    @Test
    fun `cleanMessage - strips self-closing tags`() {
        assertEquals("HelloWorld", MessageUtils.cleanMessage("Hello<br/>World"))
    }

    @Test
    fun `cleanMessage - strips nested HTML`() {
        assertEquals("Hello", MessageUtils.cleanMessage("<div><p>Hello</p></div>"))
    }

    // ──────────────────────────────────────────────
    // Combined scenarios
    // ──────────────────────────────────────────────

    @Test
    fun `cleanMessage - Arabic text with emoji and digits`() {
        val input = "\u0645\u0631\u062D\u0628\u0627 \uD83D\uDE00 \u0661\u0662\u0663"
        val expected = "\u0645\u0631\u062D\u0628\u0627  123"
        assertEquals(expected, MessageUtils.cleanMessage(input))
    }

    @Test
    fun `cleanMessage - BOM plus HTML plus emoji`() {
        assertEquals("Hello World", MessageUtils.cleanMessage("\uFEFF<b>Hello</b> \uD83D\uDE00World"))
    }

    // ──────────────────────────────────────────────
    // Edge cases
    // ──────────────────────────────────────────────

    @Test
    fun `cleanMessage - empty string`() {
        assertEquals("", MessageUtils.cleanMessage(""))
    }

    @Test
    fun `cleanMessage - emoji-only message returns empty`() {
        assertEquals("", MessageUtils.cleanMessage("\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02").trim())
    }

    @Test
    fun `cleanMessage - mixed whitespace and control chars`() {
        val cleaned = MessageUtils.cleanMessage("  Hello \u0000\u0007 World  ").trim()
        // Control chars are stripped, but spaces around them remain
        assertTrue(cleaned.startsWith("Hello"))
        assertTrue(cleaned.endsWith("World"))
    }
}
