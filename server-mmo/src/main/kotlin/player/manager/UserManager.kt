/*
 * This file is part of [GreenVale]
 *
 * [GreenVale] is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * [GreenVale] is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with [GreenVale].  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.decodesoft.player.manager

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.transactions.transaction
import pl.decodesoft.items.model.EquippedItems
import pl.decodesoft.player.model.CharacterClass
import pl.decodesoft.player.model.CharacterInfo
import pl.decodesoft.player.model.User
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// Tabela użytkowników
object Users : UUIDTable() {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 60)
    val characters = json<List<CharacterInfo>>("characters", Json.Default)
    val selectedCharacterSlot = integer("selected_character_slot").nullable()
}

class UserManager {
    private val users = ConcurrentHashMap<String, User>()

    // Mapa do szybkiego dostępu: characterId -> (user, character)
    private val characterCache = mutableMapOf<String, Pair<User, CharacterInfo>>()

    init {
        // Tworzenie tabeli jeśli nie istnieje
        transaction {
            SchemaUtils.create(Users)
        }

        loadUsers()
        refreshCharacterCache()
    }

    private fun loadUsers() {
        transaction {
            Users.selectAll().forEach { row ->
                val charactersFromDb = row[Users.characters]

                // Migracja starych postaci bez pola equippedItems
                val migratedCharacters = charactersFromDb.map { character ->
                    if (character.equippedItems == EquippedItems()) {
                        // Postać już ma pole equippedItems lub jest nowa
                        character
                    } else {
                        // Dla bezpieczeństwa, upewnij się że pole istnieje
                        character.copy(equippedItems = character.equippedItems)
                    }
                }.toMutableList()

                val user = User(
                    id = row[Users.id].value.toString(),
                    username = row[Users.username],
                    passwordHash = row[Users.passwordHash],
                    characters = migratedCharacters,
                    selectedCharacterSlot = row[Users.selectedCharacterSlot]
                )
                users[user.username.lowercase()] = user
            }
        }
        println("Loaded ${users.size} users from PostgreSQL database")
    }

    // Odśwież cache po załadowaniu użytkowników
    private fun refreshCharacterCache() {
        characterCache.clear()
        users.values.forEach { user ->
            user.characters.forEach { character ->
                characterCache[character.id] = Pair(user, character)
            }
        }
        println("Refreshed character cache with ${characterCache.size} characters")
    }

    fun saveUsers() {
        transaction {
            users.values.forEach { user ->
                Users.upsert {
                    it[id] = UUID.fromString(user.id)
                    it[username] = user.username
                    it[passwordHash] = user.passwordHash
                    it[characters] = user.characters
                    it[selectedCharacterSlot] = user.selectedCharacterSlot
                }
            }
        }
        println("Saved ${users.size} users to PostgreSQL database")
    }

    fun registerUser(username: String, password: String): Result<User> {
        if (username.length < 3) {
            return Result.failure(Exception("Nazwa użytkownika musi mieć co najmniej 3 znaki"))
        }

        if (password.length < 6) {
            return Result.failure(Exception("Hasło musi mieć co najmniej 6 znaków"))
        }

        val normalizedUsername = username.lowercase()

        if (users.containsKey(normalizedUsername)) {
            return Result.failure(Exception("Nazwa użytkownika już istnieje"))
        }

        // Hash hasła za pomocą bcrypt
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())

        // Tworzenie nowego użytkownika z unikalnym ID
        val userId = UUID.randomUUID().toString()
        val user = User(userId, username, passwordHash)

        // Zapisz do bazy danych
        transaction {
            Users.insert {
                it[id] = UUID.fromString(userId)
                it[Users.username] = username
                it[Users.passwordHash] = passwordHash
                it[characters] = emptyList()
                it[selectedCharacterSlot] = null
            }
        }

        users[normalizedUsername] = user

        return Result.success(user)
    }

    fun authenticateUser(username: String, password: String): Result<User> {
        val normalizedUsername = username.lowercase()
        val user = users[normalizedUsername] ?: return Result.failure(Exception("Nie znaleziono użytkownika"))

        // Weryfikacja hasła
        val result = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)

        return if (result.verified) {
            Result.success(user)
        } else {
            Result.failure(Exception("Nieprawidłowe hasło"))
        }
    }

    fun getUserById(id: String): User? {
        return users.values.find { it.id == id }
    }

    fun updateUser(updatedUser: User) {
        val normalizedUsername = updatedUser.username.lowercase()
        if (users.containsKey(normalizedUsername)) {
            users[normalizedUsername] = updatedUser

            // Zapisz do bazy danych
            transaction {
                Users.update({ Users.id eq UUID.fromString(updatedUser.id) }) {
                    it[characters] = updatedUser.characters
                    it[selectedCharacterSlot] = updatedUser.selectedCharacterSlot
                }
            }

            // Odśwież cache po aktualizacji
            refreshCharacterCache()
        }
    }

    // Sprawdza, czy nazwa postaci jest już zajęta
    private fun isNicknameTaken(nickname: String): Boolean {
        return users.values.any { user ->
            user.characters.any { character ->
                character.nickname.equals(nickname, ignoreCase = true)
            }
        }
    }

    // Tworzy nową postać dla użytkownika
    fun createCharacter(userId: String, nickname: String, characterClassOrdinal: Int): Result<CharacterInfo> {
        val user = getUserById(userId) ?: return Result.failure(Exception("Nie znaleziono użytkownika"))

        if (user.characters.size >= 3) {
            return Result.failure(Exception("Osiągnięto maksymalną liczbę postaci (3)"))
        }

        if (isNicknameTaken(nickname)) {
            return Result.failure(Exception("Nazwa postaci jest już zajęta"))
        }

        // Określ klasę postaci na podstawie wartości liczbowej
        val characterClass = CharacterClass.entries.toTypedArray()
            .getOrElse(characterClassOrdinal) { CharacterClass.WARRIOR }

        // Ustal maksymalne zdrowie dla klasy
        val maxHealth = characterClass.baseHealth

        // Pobierz domyślną pozycję spawnu
        val defaultSpawn = SpawnManager.getDefaultSpawn()

        // Tworzenie nowej postaci z pustym ekwipunkiem
        val characterId = UUID.randomUUID().toString()
        val newCharacter = CharacterInfo(
            id = characterId,
            nickname = nickname,
            characterClass = characterClassOrdinal,
            maxHealth = maxHealth,
            currentHealth = maxHealth,
            level = 1,
            experience = 0,
            lastX = defaultSpawn.first,  // Ustaw domyślną pozycję X
            lastY = defaultSpawn.second, // Ustaw domyślną pozycję Y
            equippedItems = EquippedItems() // Pusty ekwipunek na start
        )

        // Dodaj postać do konta użytkownika
        user.characters.add(newCharacter)
        updateUser(user)

        return Result.success(newCharacter)
    }

    // Pobiera informacje o aktywnej postaci użytkownika na podstawie indeksu slotu
    fun getCharacterBySlot(userId: String, slotIndex: Int): Result<CharacterInfo> {
        val user = getUserById(userId) ?: return Result.failure(Exception("Nie znaleziono użytkownika"))

        if (slotIndex < 0 || slotIndex >= user.characters.size) {
            return Result.failure(Exception("Nieprawidłowy slot postaci"))
        }

        return Result.success(user.characters[slotIndex])
    }

    // Usuwa postać z konta użytkownika
    fun deleteCharacter(userId: String, characterId: String): Result<Boolean> {
        val user = getUserById(userId) ?: return Result.failure(Exception("Nie znaleziono użytkownika"))

        val characterToRemove = user.characters.find { it.id == characterId }
            ?: return Result.failure(Exception("Nie znaleziono postaci"))

        user.characters.remove(characterToRemove)

        // Jeśli usuwamy aktualnie wybraną postać, zresetuj wybór
        if (user.selectedCharacterSlot == user.characters.indexOf(characterToRemove)) {
            user.selectedCharacterSlot = null
        }

        updateUser(user)

        return Result.success(true)
    }
}