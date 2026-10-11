package app.quiet.snapshot

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import quiet.core.rules.AppRules
import quiet.core.snapshot.SnapshotNode
import quiet.core.snapshot.SnapshotRedactor
import java.io.File

object SnapshotCapture {
    sealed class Result {
        data class Success(val file: File) : Result()
        data class WrongApp(val message: String) : Result()
        data class Failed(val message: String) : Result()
    }

    fun capture(
        service: AccessibilityService,
        rulesFor: (String) -> AppRules?,
    ): Result {
        val active = service.rootInActiveWindow ?: return Result.Failed("No active window")
        val pkg = active.packageName?.toString()
        val rules = pkg?.let { rulesFor(it) }
        if (rules == null) {
            active.recycle()
            return Result.WrongApp("Switch to Instagram or TikTok first.")
        }
        try {
            val tree = buildTree(active, depth = 0)
            val redacted = SnapshotRedactor.redactNode(tree)
            val json = JSONObject()
                .put("package", pkg)
                .put("tree", nodeToJson(redacted))
            val dir = File(service.filesDir, "snapshots").apply { mkdirs() }
            val file = File(dir, "snapshot-${System.currentTimeMillis()}.json")
            file.writeText(json.toString(2))
            return Result.Success(file)
        } catch (_: Throwable) {
            return Result.Failed("Capture failed")
        } finally {
            active.recycle()
        }
    }

    private fun buildTree(node: AccessibilityNodeInfo, depth: Int): SnapshotNode {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        val children = JSONArray()
        val childNodes = mutableListOf<SnapshotNode>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            childNodes.add(buildTree(child, depth + 1))
            child.recycle()
        }
        return SnapshotNode(
            className = node.className?.toString(),
            bounds = rect.toShortString(),
            depth = depth,
            clickable = node.isClickable,
            enabled = node.isEnabled,
            selected = node.isSelected,
            viewId = node.viewIdResourceName,
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            children = childNodes,
        )
    }

    private fun nodeToJson(node: SnapshotNode): JSONObject {
        val arr = JSONArray()
        node.children.forEach { arr.put(nodeToJson(it)) }
        return JSONObject()
            .put("className", node.className)
            .put("bounds", node.bounds)
            .put("depth", node.depth)
            .put("clickable", node.clickable)
            .put("enabled", node.enabled)
            .put("selected", node.selected)
            .put("viewId", node.viewId)
            .put("text", node.text)
            .put("contentDescription", node.contentDescription)
            .put("children", arr)
    }
}
