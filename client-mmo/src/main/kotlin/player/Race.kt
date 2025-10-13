package pl.decodesoft.player

// Enum dla ras - klient
enum class Race(val displayName: String) {
    HUMAN("Człowiek"),
    ELF("Elf"),
    GOBLIN("Goblin"),
    UNDEAD("Nieumarły");

    companion object {
        fun fromString(value: String): Race {
            return entries.find { it.name == value } ?: HUMAN
        }
    }
}