package app.quiet.ui.sheets

data class InterventionSheetModel(
    val surfaceLabel: String,
    val messagesFound: Boolean,
    val breakEnabled: Boolean,
) {
    val showGoToMessages: Boolean = messagesFound
}
