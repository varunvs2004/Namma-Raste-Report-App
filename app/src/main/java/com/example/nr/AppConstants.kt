package com.example.nr

const val ADMIN_EMAIL = "admin@gmail.com"
const val REPORT_COLLECTION = "reports"

val IssueTypes = listOf("Pothole", "Street Light", "Garbage")
val Priorities = listOf("Low", "Medium", "High", "Emergency")
val ReportStatuses = listOf("Pending", "Under Review", "In Progress", "Completed", "Rejected")
val TimelineStatuses = listOf("Submitted", "Under Review", "In Progress", "Completed", "Rejected")

fun ticketPrefixForIssue(type: String): String {
    return when (type.trim().lowercase()) {
        "pothole" -> "NRP"
        "street light", "light", "streetlight" -> "NRL"
        "garbage" -> "NRG"
        else -> "NRR"
    }
}
