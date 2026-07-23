package com.james.mathwakealarm

import com.google.mlkit.vision.barcode.common.Barcode
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Base64

/**
 * Stores and compares barcode identities without relying on a single raw string.
 *
 * Older builds stored only Barcode.rawValue. Some scanners can return the same
 * physical retail code as UPC-A on one scan and EAN-13 with a leading zero on
 * another, or include invisible whitespace. This helper keeps raw bytes, raw
 * text, display text and format when registering a code, while remaining
 * backwards-compatible with the old plain-string value.
 */
object BarcodeIdentity {
    private const val PREFIX = "TAZB2"
    private const val SEPARATOR = "|"

    private data class Snapshot(
        val format: Int,
        val raw: String,
        val display: String,
        val bytesBase64: String
    )

    fun capture(barcode: Barcode): String = encode(
        Snapshot(
            format = barcode.format,
            raw = barcode.rawValue.orEmpty(),
            display = barcode.displayValue.orEmpty(),
            bytesBase64 = barcode.rawBytes
                ?.takeIf { it.isNotEmpty() }
                ?.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
                .orEmpty()
        )
    )

    fun matches(storedIdentity: String, scannedBarcode: Barcode): Boolean {
        val scanned = Snapshot(
            format = scannedBarcode.format,
            raw = scannedBarcode.rawValue.orEmpty(),
            display = scannedBarcode.displayValue.orEmpty(),
            bytesBase64 = scannedBarcode.rawBytes
                ?.takeIf { it.isNotEmpty() }
                ?.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
                .orEmpty()
        )
        return matchesSnapshot(storedIdentity, scanned)
    }

    internal fun encodeForTest(
        raw: String,
        display: String = raw,
        format: Int = Barcode.FORMAT_UNKNOWN,
        rawBytes: ByteArray? = null
    ): String = encode(
        Snapshot(
            format = format,
            raw = raw,
            display = display,
            bytesBase64 = rawBytes
                ?.takeIf { it.isNotEmpty() }
                ?.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
                .orEmpty()
        )
    )

    internal fun matchesForTest(
        storedIdentity: String,
        scannedRaw: String,
        scannedDisplay: String = scannedRaw,
        scannedFormat: Int = Barcode.FORMAT_UNKNOWN,
        scannedRawBytes: ByteArray? = null
    ): Boolean = matchesSnapshot(
        storedIdentity,
        Snapshot(
            format = scannedFormat,
            raw = scannedRaw,
            display = scannedDisplay,
            bytesBase64 = scannedRawBytes
                ?.takeIf { it.isNotEmpty() }
                ?.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
                .orEmpty()
        )
    )

    private fun matchesSnapshot(storedIdentity: String, scanned: Snapshot): Boolean {
        if (storedIdentity.isBlank()) return false
        val saved = decode(storedIdentity)

        if (
            saved.bytesBase64.isNotBlank() &&
            scanned.bytesBase64.isNotBlank() &&
            saved.bytesBase64 == scanned.bytesBase64
        ) {
            return true
        }

        val savedVariants = textVariants(saved)
        val scannedVariants = textVariants(scanned)
        if (savedVariants.intersect(scannedVariants).isNotEmpty()) return true

        val savedGtins = savedVariants.mapNotNull(::canonicalGtin).toSet()
        val scannedGtins = scannedVariants.mapNotNull(::canonicalGtin).toSet()
        return savedGtins.intersect(scannedGtins).isNotEmpty()
    }

    private fun textVariants(snapshot: Snapshot): Set<String> = buildSet {
        listOf(snapshot.raw, snapshot.display).forEach { value ->
            clean(value).takeIf { it.isNotBlank() }?.let(::add)
        }
    }

    private fun clean(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFKC)
        .replace("\uFEFF", "")
        .replace("\u200B", "")
        .replace("\u200C", "")
        .replace("\u200D", "")
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim()

    /**
     * GTINs are commonly surfaced as UPC-A (12 digits) or EAN-13 with a leading
     * zero. Left-padding supported GTIN lengths to 14 digits treats those two
     * scanner representations as the same physical retail barcode.
     */
    private fun canonicalGtin(value: String): String? {
        val digitsOnly = value.filter(Char::isDigit)
        val containsOnlyGtinCharacters = value.all { it.isDigit() || it.isWhitespace() || it == '-' }
        if (!containsOnlyGtinCharacters) return null
        if (digitsOnly.length !in setOf(8, 12, 13, 14)) return null
        return digitsOnly.padStart(14, '0')
    }

    private fun encode(snapshot: Snapshot): String = listOf(
        PREFIX,
        snapshot.format.toString(),
        encodeText(snapshot.raw),
        encodeText(snapshot.display),
        snapshot.bytesBase64
    ).joinToString(SEPARATOR)

    private fun decode(value: String): Snapshot {
        if (!value.startsWith("$PREFIX$SEPARATOR")) {
            return Snapshot(
                format = Barcode.FORMAT_UNKNOWN,
                raw = value,
                display = value,
                bytesBase64 = ""
            )
        }

        val parts = value.split(SEPARATOR, limit = 5)
        if (parts.size != 5) {
            return Snapshot(Barcode.FORMAT_UNKNOWN, value, value, "")
        }

        return runCatching {
            Snapshot(
                format = parts[1].toIntOrNull() ?: Barcode.FORMAT_UNKNOWN,
                raw = decodeText(parts[2]),
                display = decodeText(parts[3]),
                bytesBase64 = parts[4]
            )
        }.getOrElse {
            Snapshot(Barcode.FORMAT_UNKNOWN, value, value, "")
        }
    }

    private fun encodeText(value: String): String = Base64
        .getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String {
        if (value.isBlank()) return ""
        return String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }
}
