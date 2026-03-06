package com.kwtsms

/**
 * Message cleaning utilities for kwtSMS.
 */
object MessageUtils {

    /**
     * Clean a message for safe sending via kwtSMS.
     *
     * Processing order:
     * 1. Convert Arabic-Indic and Extended Arabic-Indic digits to Latin
     * 2. Remove emojis
     * 3. Remove hidden invisible characters (zero-width space, BOM, soft hyphen, etc.)
     * 4. Remove directional formatting characters
     * 5. Remove C0 and C1 control characters (preserve \n and \t)
     * 6. Strip HTML tags
     */
    @JvmStatic
    fun cleanMessage(text: String): String {
        val sb = StringBuilder(text.length)

        // Steps 1-5: character-level processing using code points
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val charCount = Character.charCount(cp)

            when {
                // Step 1a: Arabic-Indic digits -> Latin
                cp in 0x0660..0x0669 -> sb.append(('0'.code + (cp - 0x0660)).toChar())
                // Step 1b: Extended Arabic-Indic / Persian digits -> Latin
                cp in 0x06F0..0x06F9 -> sb.append(('0'.code + (cp - 0x06F0)).toChar())

                // Step 2: Remove emojis
                isEmoji(cp) -> { /* skip */ }

                // Step 3: Remove hidden invisible characters
                isHiddenInvisible(cp) -> { /* skip */ }

                // Step 4: Remove directional formatting characters
                isDirectionalFormatting(cp) -> { /* skip */ }

                // Step 5: Remove C0 and C1 control characters (preserve \n and \t)
                isControlChar(cp) -> { /* skip */ }

                // Keep everything else (including Arabic text)
                else -> sb.appendCodePoint(cp)
            }

            i += charCount
        }

        // Step 6: Strip HTML tags
        return stripHtmlTags(sb.toString())
    }

    private fun isEmoji(cp: Int): Boolean {
        return cp in 0x1F000..0x1F02F ||   // Mahjong, domino tiles
            cp in 0x1F0A0..0x1F0FF ||      // Playing cards
            cp in 0x1F1E0..0x1F1FF ||      // Regional indicator symbols / flags
            cp in 0x1F300..0x1F5FF ||      // Misc symbols and pictographs
            cp in 0x1F600..0x1F64F ||      // Emoticons
            cp in 0x1F680..0x1F6FF ||      // Transport and map
            cp in 0x1F700..0x1F77F ||      // Alchemical symbols
            cp in 0x1F780..0x1F7FF ||      // Geometric shapes extended
            cp in 0x1F800..0x1F8FF ||      // Supplemental arrows
            cp in 0x1F900..0x1F9FF ||      // Supplemental symbols and pictographs
            cp in 0x1FA00..0x1FA6F ||      // Chess symbols
            cp in 0x1FA70..0x1FAFF ||      // Symbols and pictographs extended
            cp in 0x2600..0x26FF ||        // Misc symbols
            cp in 0x2700..0x27BF ||        // Dingbats
            cp in 0xFE00..0xFE0F ||        // Variation selectors
            cp == 0x20E3 ||                // Combining enclosing keycap
            cp in 0xE0000..0xE007F         // Tags block
    }

    private fun isHiddenInvisible(cp: Int): Boolean {
        return cp == 0x200B || // Zero-width space
            cp == 0x200C ||    // Zero-width non-joiner
            cp == 0x200D ||    // Zero-width joiner
            cp == 0x2060 ||    // Word joiner
            cp == 0x00AD ||    // Soft hyphen
            cp == 0xFEFF ||    // BOM
            cp == 0xFFFC       // Object replacement character
    }

    private fun isDirectionalFormatting(cp: Int): Boolean {
        return cp == 0x200E ||          // Left-to-right mark
            cp == 0x200F ||             // Right-to-left mark
            cp in 0x202A..0x202E ||     // LRE, RLE, PDF, LRO, RLO
            cp in 0x2066..0x2069        // LRI, RLI, FSI, PDI
    }

    private fun isControlChar(cp: Int): Boolean {
        // C0 controls (0x0000-0x001F) except TAB (0x0009) and LF (0x000A)
        if (cp in 0x0000..0x001F && cp != 0x0009 && cp != 0x000A) return true
        // DEL
        if (cp == 0x007F) return true
        // C1 controls
        if (cp in 0x0080..0x009F) return true
        return false
    }

    private fun stripHtmlTags(text: String): String {
        return text.replace(Regex("<[^>]*>"), "")
    }
}
