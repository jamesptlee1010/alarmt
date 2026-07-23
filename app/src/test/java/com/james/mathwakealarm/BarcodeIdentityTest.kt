package com.james.mathwakealarm

import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeIdentityTest {
    @Test
    fun exactStoredIdentityMatches() {
        val stored = BarcodeIdentity.encodeForTest(
            raw = "9312345678901",
            format = Barcode.FORMAT_EAN_13,
            rawBytes = byteArrayOf(1, 2, 3)
        )

        assertTrue(
            BarcodeIdentity.matchesForTest(
                storedIdentity = stored,
                scannedRaw = "9312345678901",
                scannedFormat = Barcode.FORMAT_EAN_13,
                scannedRawBytes = byteArrayOf(1, 2, 3)
            )
        )
    }

    @Test
    fun oldPlainStringStorageStillMatches() {
        assertTrue(
            BarcodeIdentity.matchesForTest(
                storedIdentity = "ABC-123",
                scannedRaw = "  ABC-123  "
            )
        )
    }

    @Test
    fun upcAndEanLeadingZeroAreTreatedAsSameGtin() {
        val stored = BarcodeIdentity.encodeForTest(
            raw = "012345678905",
            format = Barcode.FORMAT_UPC_A
        )

        assertTrue(
            BarcodeIdentity.matchesForTest(
                storedIdentity = stored,
                scannedRaw = "0012345678905",
                scannedFormat = Barcode.FORMAT_EAN_13
            )
        )
    }

    @Test
    fun invisibleCharactersDoNotCauseFalseMismatch() {
        val stored = BarcodeIdentity.encodeForTest(raw = "CODE123")
        assertTrue(
            BarcodeIdentity.matchesForTest(
                storedIdentity = stored,
                scannedRaw = "\uFEFFCODE123\u200B"
            )
        )
    }

    @Test
    fun genuinelyDifferentBarcodeDoesNotMatch() {
        val stored = BarcodeIdentity.encodeForTest(raw = "9312345678901")
        assertFalse(
            BarcodeIdentity.matchesForTest(
                storedIdentity = stored,
                scannedRaw = "9312345678999"
            )
        )
    }
}
