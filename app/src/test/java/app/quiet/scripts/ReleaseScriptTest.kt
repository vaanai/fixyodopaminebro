package app.quiet.scripts

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseScriptTest {
    @Test
    fun releaseScriptRefusesWithoutEnv() {
        val root = File(System.getProperty("user.dir")).let {
            if (it.name == "app") it.parentFile!! else it
        }
        val script = File(root, "scripts/build-release.sh")
        assertTrue(script.exists())
        val proc = ProcessBuilder("bash", script.absolutePath)
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        assertTrue(out.contains("QUIET_KEYSTORE_PATH"))
        assertTrue(proc.exitValue() != 0)
    }
}
