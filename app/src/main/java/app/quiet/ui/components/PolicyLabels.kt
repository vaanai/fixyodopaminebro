package app.quiet.ui.components

import quiet.core.rules.Policy

fun policySegmentLabel(policy: Policy): String = when (policy) {
    Policy.ALLOW -> "Allow"
    Policy.GATE -> "Gate"
    Policy.BLOCK -> "Block"
    Policy.OPEN_ONLY -> "Open-only"
}
