package pl.decodesoft.player

// Enum dla frakcji - klient
enum class Faction(val displayName: String) {
    WATAHA("Wataha"),
    ZAKON("Zakon"),
    NONE("Brak frakcji");

    companion object {
        fun fromString(value: String): Faction {
            return entries.find { it.name == value } ?: NONE
        }
    }
}