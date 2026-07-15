package com.simple.ui.precompute

object PrecomputeDemoData {

    data class WordItem(val word: String, val ipa: String, val meaning: String)
    data class ProfileItem(val name: String, val tag: String, val role: String)
    data class NoteItem(val title: String, val note: String)

    val items = listOf(
        WordItem("Hello World — a very long word to test text wrapping behavior", "/həˈloʊ wɜːrld/", "Xin chào thế giới"),
        WordItem("Google nói Advertising ID là mã định danh do Google Play services cung cấp, người dùng có thể reset hoặc xóa. Khi bị xóa, app đọc ID có thể chuỗi toàn số 0", "/ˈændrɔɪd/", "Hệ điều hành"),
        WordItem("Kotlin", "/ˈkɒtlɪn/", "Ngôn ngữ lập trình"),
        WordItem("Precompute", "/priːkəmˈpjuːt/", "Tính trước"),
    )

    val profiles = listOf(
        ProfileItem("Alice Nguyen", "#Android", "Senior Engineer  ·  Google"),
        ProfileItem("Bob Tran", "#Kotlin", "Staff Engineer  ·  JetBrains"),
        ProfileItem("Carol Le", "#Compose", "UI Engineer  ·  Meta"),
    )

    val notes = listOf(
        NoteItem("Meet", "Meet"),
        NoteItem("Design sync", "Review node spacing from the XML version"),
        NoteItem(
            "Long title wraps inside the remaining width",
            "The text column uses MatchParent after icon width and marginStart are reserved."
        )
    )
}
