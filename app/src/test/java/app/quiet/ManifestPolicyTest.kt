package app.quiet

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class ManifestPolicyTest {
    @Test
    fun manifestHasNoForbiddenPermissions() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("INTERNET"))
        assertFalse(manifest.contains("SYSTEM_ALERT_WINDOW"))
        assertFalse(manifest.contains("FOREGROUND_SERVICE"))
    }
}
