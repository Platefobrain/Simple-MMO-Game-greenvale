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

package pl.decodesoft

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.decodesoft.enemy.manager.EnemyManager
import pl.decodesoft.items.manager.ItemManager
import pl.decodesoft.items.model.ItemType
import pl.decodesoft.map.GameMap
import pl.decodesoft.msg.ChatManager
import pl.decodesoft.pathfinding.findPath
import pl.decodesoft.player.api.requests.CharacterCreateRequest
import pl.decodesoft.player.api.requests.CharacterSelectRequest
import pl.decodesoft.player.api.requests.CharactersListRequest
import pl.decodesoft.player.api.responses.CharacterCreateResponse
import pl.decodesoft.player.api.responses.CharacterSelectResponse
import pl.decodesoft.player.api.responses.CharactersListResponse
import pl.decodesoft.player.combat.PlayerCombatManager
import pl.decodesoft.player.manager.RespawnManager
import pl.decodesoft.player.manager.SpawnManager
import pl.decodesoft.player.manager.UserManager
import pl.decodesoft.player.model.CharacterClass
import pl.decodesoft.player.model.PlayerData
import pl.decodesoft.player.movement.MovementTarget
import pl.decodesoft.player.movement.PlayerMovementManager
import player.database.DatabaseConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

// Modele danych do obsługi uwierzytelniania
@Serializable
data class AuthRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val success: Boolean, val message: String, val userId: String = "")

// Modele usuwania postaci
@Serializable
data class DeleteCharacterRequest(val userId: String, val characterId: String)

@Serializable
data class DeleteCharacterResponse(val success: Boolean, val message: String)

// Wysyła wiadomość do wszystkich oprócz nadawcy
suspend fun broadcastToOthers(connections: ConcurrentHashMap<String, DefaultWebSocketSession>,
                              senderSessionId: String, message: String) {
    connections.forEach { (sessionId, session) ->
        if (sessionId != senderSessionId) {
            session.send(message)
        }
    }
}

// Wysyła wiadomość do wszystkich
suspend fun broadcastToAll(connections: ConcurrentHashMap<String, DefaultWebSocketSession>, message: String) {
    connections.forEach { (_, session) ->
        session.send(message)
    }
}

fun handleItemMove(
    playerId: String,
    fromType: String,
    fromSlot: Int,
    toType: String,
    toSlot: Int,
    itemId: String,
    playerStates: ConcurrentHashMap<String, PlayerData>,
    userManager: UserManager,
    itemManager: ItemManager
): Result<String> {
    return try {
        // Użyj nowej metody z ItemManager
        itemManager.moveItemBetweenSlots(playerId, fromType, fromSlot, toType, toSlot, itemId, playerStates, userManager)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Główna funkcja serwera
fun main() {
    // Inicjalizacja i konfiguracja
    DatabaseConfig.initialize()                 // Inicjalizacja bazy danych
    val autoSaveInterval = 5L                   // Interwał zapisu w minutach
    val updateInterval = 17L                    // Czas między update'ami (ok. 17 ms = ~60 FPS)

    // Kolekcje do przechowywania stanów i sesji
    val connections = ConcurrentHashMap<String, DefaultWebSocketSession>()      // Aktywne sesje WebSocket
    val playerSessions = mutableMapOf<String, String>()                         // Mapowanie graczId -> sesjaId
    val playerStates = ConcurrentHashMap<String, PlayerData>()                  // Dane o stanie graczy
    val playerMovementTargets = ConcurrentHashMap<String, MovementTarget>()     // Cele ruchu graczy
    val sessionCharacters = ConcurrentHashMap<String, String>()                 // Mapowanie sesji do postaci

    // Menedżery
    val userManager = UserManager()                                              // Zarządzanie użytkownikami
    val itemManager = ItemManager()                                              // Zarządzanie itemami
    val chatManager = ChatManager()                                              // Zarządzanie czatem
    val enemyManager = EnemyManager()                                            // Zarządzanie przeciwnikami
    val playerMovementManager = PlayerMovementManager(connections, playerStates, playerMovementTargets)  // Zarządzanie ruchem graczy
    val playerCombatManager = PlayerCombatManager(connections, playerStates, userManager, enemyManager)  // Walka graczy

    // Regeneracja zdrowia
    val regenCooldown = 5f        // Czas od ostatnich obrażeń do startu regeneracji (sekundy)
    val regenAmount = 2           // Ilość regenerowanego życia na jeden tick
    val regenInterval = 2f        // Interwał między kolejnymi regeneracjami (sekundy)
    var regenTimer = 0f           // Timer odliczający do kolejnej regeneracji

    // Regeneracja many
    val manaRegenCooldown = 5f        // Czas od ostatniego użycia many do startu regeneracji
    val manaRegenAmount = 2           // Ilość regenerowanej many na jeden tick
    val manaRegenInterval = 2f        // Interwał między regeneracjami many
    var manaRegenTimer = 0f           // Timer regeneracji many

    // Spawn przeciwników
    // Sheep lvl 1 około 900x700, rozrzut ±60px
    enemyManager.spawnSheep(800f, 800f, level = 1)
    enemyManager.spawnSheep(850f, 800f, level = 1)
    enemyManager.spawnSheep(900f, 800f, level = 1)
    enemyManager.spawnSheep(800f, 850f, level = 1)
    enemyManager.spawnSheep(850f, 850f, level = 1)
    enemyManager.spawnSheep(900f, 850f, level = 1)

    // Wolf lvl 2 około 950x800, rozrzut ±70px
    enemyManager.spawnWolf(1100f, 1000f, level = 2)
    enemyManager.spawnWolf(1150f, 1000f, level = 2)
    enemyManager.spawnWolf(1200f, 1000f, level = 2)
    enemyManager.spawnWolf(1100f, 1050f, level = 2)
    enemyManager.spawnWolf(1150f, 1050f, level = 2)

    // Bear lvl 3 od 1000 do 1300, rozrzut ±70px - 100px
    enemyManager.spawnBear(1400f, 1200f, level = 3)
    enemyManager.spawnBear(1450f, 1200f, level = 3)
    enemyManager.spawnBear(1500f, 1200f, level = 3)
    enemyManager.spawnBear(1400f, 1250f, level = 3)
    enemyManager.spawnBear(1450f, 1250f, level = 3)
    enemyManager.spawnBear(1500f, 1250f, level = 3)

    // Scope do korutyn petla gry, sieć
    val networkScope = CoroutineScope(Dispatchers.IO)

    // Zapis danych jednego gracza pozycja/zdrowie/lvl/exp/itemy
    fun saveSinglePlayerData(
        playerId: String,
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager
    ) {
        try {
            val playerData = playerStates[playerId]
            if (playerData != null) {
                val user = userManager.getUserById(playerId)
                val character = user?.getSelectedCharacter()

                if (user != null && character != null) {
                    // Zapisz podstawowe dane
                    character.lastX = playerData.x
                    character.lastY = playerData.y
                    character.currentHealth = playerData.currentHealth
                    character.maxHealth = playerData.maxHealth
                    character.currentMana = playerData.currentMana
                    character.maxMana = playerData.maxMana
                    character.level = playerData.level
                    character.experience = playerData.experience

                    // Zapisz statystyki bazowe (bez bonusów z itemów)
                    character.spellPower = playerData.spellPower
                    character.strength = playerData.strength
                    character.agility = playerData.agility
                    character.stamina = playerData.stamina

                    // === NOWE: Zapisz ekwipunek gracza ===
                    character.equippedItems = playerData.equippedItems

                    userManager.updateUser(user)

                    // Log z informacją o itemach
                    val equippedCount = playerData.equippedItems.getAllEquippedItems().size
                    println("Zapisano dane gracza: ${character.nickname} - " +
                            "Pozycja: (${playerData.x}, ${playerData.y}), " +
                            "HP: ${playerData.currentHealth}/${playerData.maxHealth}, " +
                            "Level: ${playerData.level}, XP: ${playerData.experience}, " +
                            "Stats: Str=${playerData.strength} Agi=${playerData.agility} " +
                            "SP=${playerData.spellPower} Sta=${playerData.stamina}, " +
                            "Itemy: $equippedCount założonych") // Log liczby itemów
                } else {
                    println("Nie znaleziono użytkownika lub postaci dla gracza: $playerId")
                }
            } else {
                println("Brak danych stanu dla gracza: $playerId")
            }
        } catch (e: Exception) {
            println("Błąd zapisu danych gracza $playerId: ${e.message}")
        }
    }

    // Zapis danych wszystkich graczy pozycja/zdrowie/lvl/exp/itemy
    fun saveAllPlayerData(
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager
    ) {
        try {
            var savedCount = 0
            var errorCount = 0
            var totalItemsCount = 0 // Licznik wszystkich itemów

            playerStates.forEach { (playerId, playerData) ->
                try {
                    val user = userManager.getUserById(playerId)
                    val character = user?.getSelectedCharacter()

                    if (user != null && character != null) {
                        // Zapisz podstawowe dane
                        character.lastX = playerData.x
                        character.lastY = playerData.y
                        character.currentHealth = playerData.currentHealth
                        character.maxHealth = playerData.maxHealth
                        character.currentMana = playerData.currentMana
                        character.maxMana = playerData.maxMana
                        character.level = playerData.level
                        character.experience = playerData.experience

                        // Zapisz statystyki bazowe
                        character.spellPower = playerData.spellPower
                        character.strength = playerData.strength
                        character.agility = playerData.agility
                        character.stamina = playerData.stamina

                        // === NOWE: Zapisz ekwipunek gracza ===
                        character.equippedItems = playerData.equippedItems
                        totalItemsCount += playerData.equippedItems.getAllEquippedItems().size

                        userManager.updateUser(user)
                        savedCount++

                        // Wyświetlanie statów z informacją o itemach
                        val equippedCount = playerData.equippedItems.getAllEquippedItems().size
                        println("Zapisano dane gracza: ${character.nickname} - " +
                                "Pozycja: (${playerData.x}, ${playerData.y}), " +
                                "HP: ${playerData.currentHealth}/${playerData.maxHealth}, " +
                                "Level: ${playerData.level}, XP: ${playerData.experience}, " +
                                "Stats: Str=${playerData.strength} Agi=${playerData.agility} " +
                                "SP=${playerData.spellPower} Sta=${playerData.stamina}, " +
                                "Itemy: $equippedCount założonych")
                    } else {
                        println("Nie znaleziono użytkownika lub postaci dla gracza: $playerId")
                        errorCount++
                    }
                } catch (e: Exception) {
                    println("Błąd zapisu danych gracza $playerId: ${e.message}")
                    errorCount++
                }
            }

            println("Globalny zapis zakończony - zapisano: $savedCount graczy, " +
                    "błędów: $errorCount, łącznie itemów: $totalItemsCount") // Log z itemami
        } catch (e: Exception) {
            println("Błąd podczas globalnego zapisu danych graczy: ${e.message}")
        }
    }

    // automatyczny zapis danych wszystkich graczy pozycja/zdrowie
    fun startAutoSaveLoop(
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager,
        networkScope: CoroutineScope
    ) {
        networkScope.launch {
            println("Uruchomiono automatyczny zapis danych graczy co $autoSaveInterval minut")

            while (true) {
                try {
                    // Czekaj określony interwał
                    delay(autoSaveInterval * 60 * 1000L)

                    // Wykonaj zapis tylko jeśli są aktywni gracze
                    if (playerStates.isNotEmpty()) {
                        println("Wykonywanie automatycznego zapisu danych ${playerStates.size} graczy...")
                        saveAllPlayerData(playerStates, userManager)
                    } else {
                        println("Brak aktywnych graczy - pomijanie zapisu")
                    }

                } catch (e: Exception) {
                    println("Błąd w pętli automatycznego zapisu: ${e.message}")
                    // Kontynuuj działanie mimo błędu
                }
            }
        }
    }

    //Funkcja do ręcznego zapisu wszystkich danych (np. przy wyłączaniu serwera)
    fun forceSaveAllPlayerData(
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager
    ) {
        println("Wymuszony zapis wszystkich danych graczy...")
        saveAllPlayerData(playerStates, userManager)

        // Dodatkowo zapisz wszystkich użytkowników na wszelki wypadek
        userManager.saveUsers()
        println("Wymuszony zapis zakończony")
    }

    // Funkcja pomocnicza do obsługi różnych typów ataków
    suspend fun handleAttack(
        parts: List<String>,
        sessionId: String,
        netType: String,
        dmgType: String,
        attackDescription: String
    ) {
        if (parts.size < 7) return

        val startX     = parts[1].toFloat()
        val startY     = parts[2].toFloat()
        val attackerId = parts[5]
        val targetId   = parts[6]

        // Ustal targetX / targetY (bez zmian)
        val (targetX, targetY) = if (targetId.startsWith("enemy_")) {
            enemyManager.getEnemy(targetId.removePrefix("enemy_"))?.let { it.x to it.y } ?: return
        } else {
            playerStates[targetId]?.let { it.x to it.y } ?: return
        }

        val attackMsg = "$netType|$startX|$startY|$targetX|$targetY|$attackerId|$targetId"
        broadcastToAll(connections, attackMsg)

        // natychmiastowe obrażenia
        playerCombatManager.processHitMessage(targetId, attackerId, dmgType)

        println("Wykonano $attackDescription od $attackerId do $targetId " +
                "($startX,$startY → $targetX,$targetY)")

    }

    val gameMap = GameMap(120, 120, 16)
    val mapCsv = object {}.javaClass.getResourceAsStream("/map.csv")?.bufferedReader()?.readText()
        ?: error("Nie znaleziono pliku map.csv")
    gameMap.loadFromCsv(mapCsv)

    // Start the game loop to continuously update player positions
    fun startGameLoop() {

        networkScope.launch {
            while (true) {
                val deltaTime = updateInterval / 1000f

                // regeneracja zdrowia
                playerStates.values.forEach { player ->
                    if (!player.isDead && player.currentHealth < player.maxHealth) {
                        player.timeSinceLastDamage += deltaTime
                    }
                }

                regenTimer += deltaTime
                if (regenTimer >= regenInterval) {
                    playerStates.forEach { (playerId, player) ->
                        if (!player.isDead &&
                            player.currentHealth < player.maxHealth &&
                            player.timeSinceLastDamage >= regenCooldown
                        ) {
                            val before = player.currentHealth
                            player.regenerateHealth(regenAmount)

                            if (player.currentHealth != before) {
                                // Wyślij aktualizację HP
                                val healthUpdateMessage =
                                    "HEALTH_UPDATE|$playerId|${player.currentHealth}|${player.maxHealth}"
                                broadcastToAll(connections, healthUpdateMessage)
                            }
                        }
                    }
                    regenTimer = 0f
                }

                // regeneracja many
                playerStates.values.forEach { player ->
                    if (player.currentMana < player.maxMana) {
                        player.timeSinceLastManaUse += deltaTime
                    }
                }

                manaRegenTimer += deltaTime
                if (manaRegenTimer >= manaRegenInterval) {
                    playerStates.forEach { (playerId, player) ->
                        if (player.currentMana < player.maxMana &&
                            player.timeSinceLastManaUse >= manaRegenCooldown
                        ) {
                            val before = player.currentMana
                            player.regenerateMana(manaRegenAmount)

                            if (player.currentMana != before) {
                                // Wyślij aktualizację many
                                val manaUpdateMessage = "MANA_UPDATE|$playerId|${player.currentMana}|${player.maxMana}"
                                broadcastToAll(connections, manaUpdateMessage)
                            }
                        }
                    }
                    manaRegenTimer = 0f
                }

                playerMovementManager.updatePlayerPositions(gameMap, deltaTime, 120f)

                enemyManager.updateEnemyTargets(playerStates, gameMap, deltaTime)
                enemyManager.updateEnemyAttacks(playerStates, deltaTime, playerCombatManager)
                enemyManager.updateHealthRegen(deltaTime)

                val respawnedEnemies = enemyManager.updateRespawnTimers(deltaTime)

                if (respawnedEnemies.isNotEmpty()) {
                    val respawnInfo = respawnedEnemies.joinToString(";") { enemy ->
                        "${enemy.id},${enemy.x},${enemy.y},${enemy.type},${enemy.currentHealth},${enemy.maxHealth},${enemyManager.getEnemyState(enemy.id)},${enemy.level}"
                    }

                    val respawnMessage = "ENEMY_RESPAWN|$respawnInfo"
                    broadcastToAll(connections, respawnMessage)
                }

                val updatedEnemies = enemyManager.updateEnemyPositions(gameMap, deltaTime)

                if (updatedEnemies.isNotEmpty()) {
                    val enemyUpdates = updatedEnemies.joinToString(";") {
                        val enemyState = enemyManager.getEnemyState(it.id)
                        "${it.id},${it.x},${it.y},${it.type},${it.currentHealth},${it.maxHealth},${enemyState},${it.level}"
                    }

                    val enemyUpdateMessage = "ENEMY_POSITIONS|$enemyUpdates"
                    broadcastToAll(connections, enemyUpdateMessage)
                }

                delay(updateInterval)
            }
        }
    }

    // połączenie
    embeddedServer(Netty, port = 8081) {
        install(WebSockets)
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        routing {
            // endpojnty uwierzytelniania
            route("/auth") {
                post("/register") {
                    val request = call.receive<AuthRequest>()
                    val result = userManager.registerUser(request.username, request.password)

                    if (result.isSuccess) {
                        val user = result.getOrNull()!!
                        call.respond(AuthResponse(true, "Rejestracja przebiegła pomyślnie", user.id))
                    } else {
                        call.respond(AuthResponse(false, result.exceptionOrNull()?.message ?: "Unknown error"))
                    }
                }

                post("/login") {
                    val request = call.receive<AuthRequest>()
                    val result = userManager.authenticateUser(request.username, request.password)

                    if (result.isSuccess) {
                        val user = result.getOrNull()!!
                        call.respond(AuthResponse(true, "Logowanie powiodło się", user.id))
                    } else {
                        call.respond(AuthResponse(false, result.exceptionOrNull()?.message ?: "Unknown error"))
                    }
                }
            }

            // Nowy endpoint do wyboru klasy postaci
            route("/character") {
                // Endpoint do pobierania listy postaci
                post("/list") {
                    val request = call.receive<CharactersListRequest>()
                    val user = userManager.getUserById(request.userId)

                    if (user != null) {
                        call.respond(
                            CharactersListResponse(
                                success = true,
                                message = "Lista postaci pobrana pomyślnie",
                                characters = user.characters
                            )
                        )
                    } else {
                        call.respond(
                            CharactersListResponse(
                                success = false,
                                message = "Nie znaleziono użytkownika"
                            )
                        )
                    }
                }

                // Endpoint do tworzenia nowej postaci
                post("/create") {
                    val request = call.receive<CharacterCreateRequest>()
                    val result = userManager.createCharacter(
                        request.userId,
                        request.nickname,
                        request.characterClass
                    )

                    if (result.isSuccess) {
                        call.respond(
                            CharacterCreateResponse(
                                success = true,
                                message = "Postać została stworzona pomyślnie"
                            )
                        )
                    } else {
                        call.respond(
                            CharacterCreateResponse(
                                success = false,
                                message = result.exceptionOrNull()?.message ?: "Nieznany błąd"
                            )
                        )
                    }
                }

                // Endpoint do wyboru postaci do gry
                post("/select") {
                    val request = call.receive<CharacterSelectRequest>()
                    val result = userManager.getCharacterBySlot(request.userId, request.characterSlot)

                    if (result.isSuccess) {
                        // Tylko ustaw który slot jest wybrany:
                        val user = userManager.getUserById(request.userId)!!
                        user.selectedCharacterSlot = request.characterSlot
                        userManager.updateUser(user)

                        call.respond(
                            CharacterSelectResponse(
                                success = true,
                                message = "Postać wybrana pomyślnie"
                            )
                        )
                    } else {
                        call.respond(
                            CharacterSelectResponse(
                                success = false,
                                message = result.exceptionOrNull()?.message ?: "Nieznany błąd"
                            )
                        )
                    }
                }

                // Endpoint do usuwania postaci
                post("/delete") {
                    val request = call.receive<DeleteCharacterRequest>()
                    println("Usuwanie postaci: userId=${request.userId}, characterId=${request.characterId}")

                    val result = userManager.deleteCharacter(request.userId, request.characterId)

                    if (result.isSuccess) {
                        println("Postać usunięta pomyślnie")
                        call.respond(
                            DeleteCharacterResponse(
                                success = true,
                                message = "Postać została usunięta pomyślnie"
                            )
                        )
                    } else {
                        println("Błąd podczas usuwania: ${result.exceptionOrNull()?.message}")
                        call.respond(
                            DeleteCharacterResponse(
                                success = false,
                                message = result.exceptionOrNull()?.message ?: "Nieznany błąd"
                            )
                        )
                    }
                }
            }

            // websocket dla komunikacji w grze
            webSocket("/ws") {
                val sessionId = call.request.local.remoteHost + "_" + System.currentTimeMillis()
                var playerId = "" // ID gracza zostanie ustawione po otrzymaniu JOIN
                var username = "" // Nazwa użytkownika

                try {
                    // nowe połączenie
                    connections[sessionId] = this
                    println("Nowe połączenie: $sessionId (Aktywni gracze: ${connections.size})")

                    send("CHAT|SERVER|Serwer|Witaj w na serwerze GreenVale")

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val message = frame.readText()
                            println("Odebrano: $message")

                            // Analiza wiadomości
                            val parts = message.split("|")
                            when (parts[0]) {
                                "JOIN" -> {
                                    if (parts.size >= 4) {
                                        playerId = parts[3]
                                        username = if (parts.size >= 5) parts[4] else "Unknown"

                                        val user = userManager.getUserById(playerId)
                                        if (user != null && user.hasSelectedCharacter()) {
                                            val selectedCharacter = user.getSelectedCharacter()!!

                                            // Sesja
                                            playerSessions[playerId] = sessionId

                                            // Zmapuj
                                            sessionCharacters[sessionId] = selectedCharacter.id
                                            println("Zmapowano sesję $sessionId -> postać ${selectedCharacter.nickname} (${selectedCharacter.id})")

                                            // Pobierz spawn z danych postaci (z bazy)
                                            val (x, y) = SpawnManager.getSpawnForCharacter(selectedCharacter)

                                            // Wczytaj gracza
                                            playerStates[playerId] = PlayerData(
                                                x, y, playerId, selectedCharacter.nickname,
                                                CharacterClass.entries[selectedCharacter.characterClass],

                                                // HEALTH I MANA OBOK SIEBIE:
                                                selectedCharacter.maxHealth,
                                                selectedCharacter.currentHealth,
                                                selectedCharacter.maxMana,
                                                selectedCharacter.currentMana,

                                                selectedCharacter.level,
                                                selectedCharacter.experience,
                                                isDead = false,
                                                timeSinceLastDamage = 0f,
                                                timeSinceLastManaUse = 0f,

                                                selectedCharacter.spellPower,
                                                selectedCharacter.strength,
                                                selectedCharacter.agility,
                                                selectedCharacter.stamina
                                            )

                                            // Załaduj ekwipunek gracza
                                            playerStates[playerId]?.loadEquipmentFromCharacter(selectedCharacter)
                                            println("Załadowano ekwipunek dla gracza ${selectedCharacter.nickname}: ${selectedCharacter.equippedItems.getAllEquippedItems().size} itemów")

                                            // zestawy startowe dla każdej klasy
                                            if (!selectedCharacter.hasReceivedStarterItems) {
                                                when (selectedCharacter.characterClass) {
                                                    CharacterClass.WARRIOR.ordinal -> {
                                                        // Zestaw dla WOJOWNIKA
                                                        selectedCharacter.inventory[0] = "weapon_starter_warrior"    // Żelazny Miecz Nowicjusza
                                                        selectedCharacter.inventory[1] = "armor_basic_warrior"       // Płytowa Zbroja
                                                        selectedCharacter.inventory[2] = "helmet_basic_warrior"      // Żelazny Hełm
                                                        selectedCharacter.inventory[3] = "pants_basic_warrior"       // Żelazne Nogawice
                                                        selectedCharacter.inventory[4] = "boots_basic_warrior"       // Ciężkie Buty
                                                        println("Przydzielono zestaw WOJOWNIKA dla ${selectedCharacter.nickname}")
                                                    }

                                                    CharacterClass.ARCHER.ordinal -> {
                                                        // Zestaw dla ŁUCZNIKA
                                                        selectedCharacter.inventory[0] = "weapon_starter_archer"     // Drewniany Łuk Nowicjusza
                                                        selectedCharacter.inventory[1] = "armor_basic_archer"        // Skórzana Zbroja
                                                        selectedCharacter.inventory[2] = "helmet_basic_archer"       // Skórzana Czapka
                                                        selectedCharacter.inventory[3] = "pants_basic_archer"        // Skórzane Spodnie
                                                        selectedCharacter.inventory[4] = "boots_basic_archer"        // Szybkie Buty
                                                        println("Przydzielono zestaw ŁUCZNIKA dla ${selectedCharacter.nickname}")
                                                    }

                                                    CharacterClass.MAGE.ordinal -> {
                                                        // Zestaw dla MAGA
                                                        selectedCharacter.inventory[0] = "weapon_starter_mage"       // Różdżka Nowicjusza
                                                        selectedCharacter.inventory[1] = "armor_basic_mage"          // Magiczna Szata
                                                        selectedCharacter.inventory[2] = "helmet_basic_mage"         // Magiczny Kaptur
                                                        selectedCharacter.inventory[3] = "pants_basic_mage"          // Magiczne Spodnie
                                                        selectedCharacter.inventory[4] = "boots_basic_mage"          // Magiczne Buty
                                                        println("Przydzielono zestaw MAGA dla ${selectedCharacter.nickname}")
                                                    }

                                                    else -> {
                                                        // Fallback - domyślny zestaw wojownika
                                                        selectedCharacter.inventory[0] = "weapon_starter_warrior"
                                                        selectedCharacter.inventory[1] = "armor_basic_warrior"
                                                        selectedCharacter.inventory[2] = "helmet_basic_warrior"
                                                        selectedCharacter.inventory[3] = "pants_basic_warrior"
                                                        selectedCharacter.inventory[4] = "boots_basic_warrior"
                                                        println("Przydzielono domyślny zestaw WOJOWNIKA dla ${selectedCharacter.nickname}")
                                                    }
                                                }

                                                // Dodaj uniwersalne itemy do dalszych slotów
                                                // selectedCharacter.inventory[5] = "sword_01"      // Dodatkowy miecz
                                                // selectedCharacter.inventory[6] = "armor_01"      // Dodatkowa skórzana zbroja

                                                selectedCharacter.hasReceivedStarterItems = true
                                                userManager.updateUser(user)

                                                println("Kompletny zestaw startowy przydzielony dla ${selectedCharacter.nickname} (klasa: ${selectedCharacter.characterClass})")
                                            }

                                            // Wyślij inventory do klienta
                                            val inventoryInfo = selectedCharacter.inventory.map { (slot, itemId) ->
                                                "$slot:$itemId:1"
                                            }.joinToString(";")

                                            if (inventoryInfo.isNotEmpty()) {
                                                send("PLAYER_INVENTORY|$inventoryInfo")
                                                println("Wysłano inventory do gracza: $inventoryInfo")
                                            }

                                            // Wyślij dane nowemu graczowi (jego własne dane)
                                            // send("JOIN|$x|$y|$playerId|${selectedCharacter.nickname}|${selectedCharacter.characterClass}|${selectedCharacter.currentHealth}|${selectedCharacter.maxHealth}|${selectedCharacter.level}|${selectedCharacter.experience}")
                                            send("JOIN|$x|$y|$playerId|${selectedCharacter.nickname}|${selectedCharacter.characterClass}|${selectedCharacter.currentHealth}|${selectedCharacter.maxHealth}|${selectedCharacter.currentMana}|${selectedCharacter.maxMana}|${selectedCharacter.level}|${selectedCharacter.experience}")

                                            // Wyślij innym graczom o tym graczu
                                            broadcastToOthers(
                                                connections, sessionId,
                                                "JOIN|$x|$y|$playerId|${selectedCharacter.nickname}|${selectedCharacter.characterClass}|${selectedCharacter.currentHealth}|${selectedCharacter.maxHealth}|${selectedCharacter.currentMana}|${selectedCharacter.maxMana}|${selectedCharacter.level}|${selectedCharacter.experience}"
                                            )

                                            // Wyślij nowemu dane o innych graczach
                                            playerStates.forEach { (id, player) ->
                                                if (id != playerId) {
                                                    send("JOIN|${player.x}|${player.y}|${player.id}|${player.username}|${player.characterClass.ordinal}|${player.currentHealth}|${player.maxHealth}|${player.currentMana}|${player.maxMana}|${player.level}|${player.experience}")
                                                }
                                            }
                                        } else {
                                            send("ERROR|Musisz najpierw wybrać postać")
                                            return@webSocket
                                        }
                                    }
                                }

                                "LEAVE_WORLD" -> {
                                    if (parts.size >= 2) {
                                        val leavingPlayerId = parts[1]
                                        val sessionIdToRemove = playerSessions[leavingPlayerId]

                                        if (sessionIdToRemove != null) {
                                            // Zapisz dane tego gracza przed usunięciem
                                            saveSinglePlayerData(leavingPlayerId, playerStates, userManager)

                                            // Usuń dane gracza
                                            playerStates.remove(leavingPlayerId)
                                            playerMovementTargets.remove(leavingPlayerId)
                                            connections.remove(sessionIdToRemove)
                                            sessionCharacters.remove(sessionIdToRemove)
                                            playerSessions.remove(leavingPlayerId)

                                            broadcastToAll(connections, "LEAVE_WORLD|$leavingPlayerId")
                                        } else {
                                            println("Nie znaleziono sesji dla gracza $leavingPlayerId")
                                        }
                                    }
                                }

                                "PATHFIND" -> {
                                    if (parts.size >= 5) {
                                        val startX = parts[1].toInt()
                                        val startY = parts[2].toInt()
                                        val endX = parts[3].toInt()
                                        val endY = parts[4].toInt()

                                        val path = findPath(gameMap, startX, startY, endX, endY)

                                        // Przekształć do prostego formatu np. PATH|x1:y1,x2:y2,...
                                        val pathString = path.joinToString(",") { "${it.first}:${it.second}" }
                                        send("PATH|$pathString")
                                    } else {
                                        send("PATH|ERROR|Nieprawidłowa liczba argumentów")
                                    }
                                }

                                "MOVE_TO" -> {
                                    if (parts.size >= 5) {
                                        val targetX = parts[1].toFloat()
                                        val targetY = parts[2].toFloat()
                                        val moveToRange = parts[3].toFloat()
                                        val playerId = parts[4]

                                        playerStates[playerId]?.let { player ->
                                            // Konwertuj na koordynaty mapy
                                            val startTileX = (player.x / gameMap.tileSize).toInt()
                                            val startTileY = (player.y / gameMap.tileSize).toInt()
                                            val endTileX = (targetX / gameMap.tileSize).toInt()
                                            val endTileY = (targetY / gameMap.tileSize).toInt()

                                            // Znajdź ścieżkę
                                            val path = findPath(gameMap, startTileX, startTileY, endTileX, endTileY)

                                            if (path.isNotEmpty()) {
                                                // Zapisz ścieżkę w celu ruchu
                                                val movementTarget = MovementTarget(
                                                    targetX = targetX,
                                                    targetY = targetY,
                                                    moveToRange = moveToRange,
                                                    path = path.toMutableList(),
                                                    currentPathIndex = 0
                                                )
                                                playerMovementTargets[playerId] = movementTarget

                                                // Wyślij ścieżkę do klienta dla wizualizacji
                                                val pathString = path.joinToString(",") { "${it.first}:${it.second}" }
                                                send("PATH|$pathString")
                                            } else {
                                                // Brak ścieżki
                                                send("MOVE_FAILED|$playerId|no_path")
                                            }
                                        }
                                    }
                                }

                                "GET_ITEMS" -> {
                                    // Pobiera wszystkie dostępne itemy w grze
                                    if (parts.size >= 2) {
                                        val requestingPlayerId = parts[1]
                                        val allItems = itemManager.getAllItems()

                                        // Format: ITEM_LIST|item1_id:name:type:str:agi:sp:sta;item2_id:name:type:str:agi:sp:sta;...
                                        val itemsData = allItems.joinToString(";") { item ->
                                            "${item.id}:${item.name}:${item.type}:${item.strengthBonus}:${item.agilityBonus}:${item.spellPowerBonus}:${item.staminaBonus}"
                                        }

                                        send("ITEM_LIST|$itemsData")
                                        println("Wysłano listę ${allItems.size} itemów do gracza $requestingPlayerId") // Log wysłania listy
                                    }
                                }

                                "GET_PLAYER_EQUIPMENT" -> {
                                    // Pobiera aktualny ekwipunek gracza
                                    if (parts.size >= 2) {
                                        val requestingPlayerId = parts[1]
                                        val equipment = itemManager.getDetailedEquipment(requestingPlayerId, playerStates)

                                        // Format: PLAYER_EQUIPMENT|helmet_id|armor_id|pants_id|boots_id|weapon_id
                                        val equipmentData = listOf(
                                            equipment[ItemType.HELMET]?.id ?: "none",
                                            equipment[ItemType.ARMOR]?.id ?: "none",
                                            equipment[ItemType.PANTS]?.id ?: "none",
                                            equipment[ItemType.BOOTS]?.id ?: "none",
                                            equipment[ItemType.WEAPON]?.id ?: "none"
                                        ).joinToString("|")

                                        send("PLAYER_EQUIPMENT|$equipmentData")
                                        println("Wysłano ekwipunek gracza $requestingPlayerId") // Log wysłania ekwipunku
                                    }
                                }

                                "GET_PLAYER_INVENTORY" -> {
                                    // Pobiera aktualny inwentarz gracza
                                    if (parts.size >= 2) {
                                        val requestingPlayerId = parts[1]
                                        val user = userManager.getUserById(requestingPlayerId)
                                        val character = user?.getSelectedCharacter()

                                        if (character != null) {
                                            // Format: PLAYER_INVENTORY|0:item_id:1;1:item_id:1;...
                                            val inventoryInfo = character.inventory.map { (slot, itemId) ->
                                                "$slot:$itemId:1"
                                            }.joinToString(";")

                                            if (inventoryInfo.isNotEmpty()) {
                                                send("PLAYER_INVENTORY|$inventoryInfo")
                                                println("Wysłano inventory do gracza $requestingPlayerId: $inventoryInfo")
                                            } else {
                                                send("PLAYER_INVENTORY|")
                                                println("Wysłano puste inventory do gracza $requestingPlayerId")
                                            }
                                        } else {
                                            send("PLAYER_INVENTORY|")
                                            println("Nie znaleziono postaci dla gracza $requestingPlayerId")
                                        }
                                    }
                                }

                                "EQUIP_ITEM" -> {
                                    // Zakłada item graczowi
                                    if (parts.size >= 3) {
                                        val requestingPlayerId = parts[1]
                                        val itemId = parts[2]

                                        val result = itemManager.equipItem(requestingPlayerId, itemId, playerStates, userManager)

                                        if (result.isSuccess) {
                                            val previousItem = result.getOrNull()
                                            val playerData = playerStates[requestingPlayerId]

                                            // Wyślij potwierdzenie do gracza
                                            send("ITEM_EQUIPPED|$itemId|${previousItem?.id ?: "none"}")

                                            // Wyślij aktualizację statystyk do gracza
                                            if (playerData != null) {
                                                val statsUpdate = "STATS_UPDATE|${playerData.getTotalStrength()}|${playerData.getTotalAgility()}|${playerData.getTotalSpellPower()}|${playerData.getTotalStamina()}|${playerData.maxHealth}"
                                                send(statsUpdate)

                                                // Rozgłoś aktualizację HP do wszystkich (jeśli się zmieniło)
                                                val healthUpdate = "HEALTH_UPDATE|$requestingPlayerId|${playerData.currentHealth}|${playerData.maxHealth}"
                                                broadcastToAll(connections, healthUpdate)

                                                val manaUpdate = "MANA_UPDATE|$playerId|${playerData.currentMana}|${playerData.maxMana}"
                                                broadcastToAll(connections, manaUpdate)
                                            }

                                            println("Gracz $requestingPlayerId założył item $itemId") // Log założenia itemu
                                        } else {
                                            // Wyślij błąd do gracza
                                            send("ITEM_EQUIP_FAILED|${result.exceptionOrNull()?.message ?: "Nieznany błąd"}")
                                            println("Błąd zakładania itemu $itemId dla gracza $requestingPlayerId: ${result.exceptionOrNull()?.message}") // Log błędu
                                        }
                                    }
                                }

                                "UNEQUIP_ITEM" -> {
                                    // Zdejmuje item z gracza
                                    if (parts.size >= 3) {
                                        val requestingPlayerId = parts[1]
                                        val itemTypeString = parts[2]

                                        try {
                                            val itemType = ItemType.valueOf(itemTypeString) // Konwersja string -> enum
                                            val result = itemManager.unequipItem(requestingPlayerId, itemType, playerStates, userManager)

                                            if (result.isSuccess) {
                                                val unequippedItem = result.getOrNull()
                                                val playerData = playerStates[requestingPlayerId]

                                                // Wyślij potwierdzenie do gracza
                                                send("ITEM_UNEQUIPPED|${unequippedItem?.id ?: "none"}|$itemTypeString")

                                                // Wyślij aktualizację statystyk do gracza
                                                if (playerData != null) {
                                                    val statsUpdate = "STATS_UPDATE|${playerData.getTotalStrength()}|${playerData.getTotalAgility()}|${playerData.getTotalSpellPower()}|${playerData.getTotalStamina()}|${playerData.maxHealth}"
                                                    send(statsUpdate)

                                                    // Rozgłoś aktualizację HP do wszystkich (jeśli się zmieniło)
                                                    val healthUpdate = "HEALTH_UPDATE|$requestingPlayerId|${playerData.currentHealth}|${playerData.maxHealth}"
                                                    broadcastToAll(connections, healthUpdate)

                                                    val manaUpdate = "MANA_UPDATE|$playerId|${playerData.currentMana}|${playerData.maxMana}"
                                                    broadcastToAll(connections, manaUpdate)
                                                }

                                                println("Gracz $requestingPlayerId zdjął item typu $itemTypeString") // Log zdjęcia itemu
                                            } else {
                                                // Wyślij błąd do gracza
                                                send("ITEM_UNEQUIP_FAILED|${result.exceptionOrNull()?.message ?: "Nieznany błąd"}")
                                                println("Błąd zdejmowania itemu $itemTypeString dla gracza $requestingPlayerId: ${result.exceptionOrNull()?.message}") // Log błędu
                                            }
                                        } catch (e: IllegalArgumentException) {
                                            send("ITEM_UNEQUIP_FAILED|Nieprawidłowy typ itemu: $itemTypeString")
                                            println("Nieprawidłowy typ itemu: $itemTypeString dla gracza $requestingPlayerId") // Log błędnego typu
                                        }
                                    }
                                }

                                "GIVE_ITEM" -> {
                                    // Admin command - daje item graczowi (do testowania)
                                    if (parts.size >= 3) {
                                        val targetPlayerId = parts[1]
                                        val itemId = parts[2]

                                        val result = itemManager.giveItemToPlayer(targetPlayerId, itemId, playerStates, userManager)

                                        if (result.isSuccess) {
                                            send("ITEM_GIVEN|$itemId|${result.getOrNull()}")
                                            println("Dano item $itemId graczowi $targetPlayerId") // Log dania itemu
                                        } else {
                                            send("ITEM_GIVE_FAILED|${result.exceptionOrNull()?.message ?: "Nieznany błąd"}")
                                            println("Błąd dawania itemu $itemId graczowi $targetPlayerId: ${result.exceptionOrNull()?.message}") // Log błędu
                                        }
                                    }
                                }

                                "MOVE_ITEM" -> {
                                    if (parts.size >= 7) {
                                        val playerId = parts[1]
                                        val fromType = parts[2] // "INVENTORY" lub "EQUIPMENT"
                                        val fromSlot = parts[3].toIntOrNull() ?: -1
                                        val toType = parts[4] // "INVENTORY" lub "EQUIPMENT"
                                        val toSlot = parts[5].toIntOrNull() ?: -1
                                        val itemId = parts[6]

                                        val result = handleItemMove(playerId, fromType, fromSlot, toType, toSlot, itemId, playerStates, userManager, itemManager)

                                        if (result.isSuccess) {
                                            val playerData = playerStates[playerId]
                                            send("ITEM_MOVED|$fromType|$fromSlot|$toType|$toSlot|$itemId")

                                            // Wyślij aktualizację statystyk jeśli przenoszenie dotyczyło ekwipunku
                                            if (fromType == "EQUIPMENT" || toType == "EQUIPMENT") {
                                                if (playerData != null) {
                                                    val statsUpdate = "STATS_UPDATE|${playerData.getTotalStrength()}|${playerData.getTotalAgility()}|${playerData.getTotalSpellPower()}|${playerData.getTotalStamina()}|${playerData.maxHealth}"
                                                    send(statsUpdate)

                                                    val healthUpdate = "HEALTH_UPDATE|$playerId|${playerData.currentHealth}|${playerData.maxHealth}"
                                                    broadcastToAll(connections, healthUpdate)

                                                    val manaUpdate = "MANA_UPDATE|$playerId|${playerData.currentMana}|${playerData.maxMana}"
                                                    broadcastToAll(connections, manaUpdate)
                                                }
                                            }

                                            println("Przeniesiono item $itemId z $fromType:$fromSlot do $toType:$toSlot dla gracza $playerId")
                                        } else {
                                            send("ITEM_MOVE_FAILED|${result.exceptionOrNull()?.message ?: "Nieznany błąd"}")
                                            println("Błąd przenoszenia itemu: ${result.exceptionOrNull()?.message}")
                                        }
                                    }
                                }

                                // do rozgłaszania tylko
                                "MOVE" -> {}

                                "GET_ENEMIES" -> {
                                    val list = enemyManager.getEnemies()
                                        .joinToString(";") { "${it.id},${it.x},${it.y},${it.type},${it.currentHealth},${it.maxHealth},${it.level}" }
                                    send("ENEMY_LIST|$list")
                                }

                                "DAMAGE_ENEMY" -> {
                                    if (parts.size >= 3) {
                                        val enemyId = parts[1]
                                        val damage = parts[2].toIntOrNull() ?: 0
                                        val died = enemyManager.damageEnemy(enemyId, damage)
                                        broadcastToAll(connections, "ENEMY_HIT|$enemyId|$damage")

                                        if (died) {
                                            broadcastToAll(connections, "ENEMY_DIED|$enemyId")
                                        }
                                    }
                                }

                                "RANGED_ATTACK" -> handleAttack(parts, sessionId,
                                    netType = "RANGED_ATTACK",
                                    dmgType = "ARROW",
                                    attackDescription = "strzał")

                                "SPELL_ATTACK"  -> handleAttack(parts, sessionId,
                                    netType = "SPELL_ATTACK",
                                    dmgType = "FIREBALL",
                                    attackDescription = "atak magiczny")

                                "MELEE_ATTACK"  -> handleAttack(parts, sessionId,
                                    netType = "MELEE_ATTACK",
                                    dmgType = "MELEE",
                                    attackDescription = "atak wręcz")

                                "HIT" -> {
                                    if (parts.size >= 4) {
                                        println("Otrzymano wiadomość HIT od klienta: $message")
                                    }
                                }

                                "DAMAGE" -> {
                                    if (parts.size >= 3) {
                                        val targetId = parts[1]
                                        val damage = parts[2].toIntOrNull() ?: 0

                                        playerStates[targetId]?.let { targetPlayer ->
                                            targetPlayer.takeDamage(damage)

                                            // Aktualizuj zdrowie w wybranej postaci użytkownika
                                            userManager.getUserById(targetId)?.let { user ->
                                                user.getSelectedCharacter()?.let { character ->
                                                    character.currentHealth = targetPlayer.currentHealth
                                                    userManager.updateUser(user)
                                                }
                                            }

                                            val healthUpdateMessage =
                                                "HEALTH_UPDATE|$targetId|${targetPlayer.currentHealth}|${targetPlayer.maxHealth}"
                                            broadcastToAll(connections, healthUpdateMessage)
                                        }
                                    }
                                }

                                "HEAL" -> {
                                    if (parts.size >= 3) {
                                        val targetId = parts[1]
                                        val healAmount = parts[2].toIntOrNull() ?: 0

                                        playerStates[targetId]?.let { targetPlayer ->
                                            targetPlayer.heal(healAmount)

                                            // Aktualizuj zdrowie w wybranej postaci
                                            userManager.getUserById(targetId)?.let { user ->
                                                user.getSelectedCharacter()?.let { character ->
                                                    character.currentHealth = targetPlayer.currentHealth
                                                    userManager.updateUser(user)
                                                }
                                            }

                                            val healthUpdateMessage =
                                                "HEALTH_UPDATE|$targetId|${targetPlayer.currentHealth}|${targetPlayer.maxHealth}"
                                            broadcastToAll(connections, healthUpdateMessage)
                                        }
                                    }
                                }

                                "HEALTH_UPDATE" -> {
                                    if (parts.size >= 3) {
                                        val targetId = parts[1]
                                        val health = parts[2].toIntOrNull() ?: 0

                                        playerStates[targetId]?.let { targetPlayer ->
                                            // Ustawiamy nowe zdrowie
                                            targetPlayer.setHealthBase(health)

                                            // Aktualizuj zdrowie w wybranej postaci
                                            userManager.getUserById(targetId)?.let { user ->
                                                user.getSelectedCharacter()?.let { character ->
                                                    character.currentHealth = targetPlayer.currentHealth
                                                    userManager.updateUser(user)
                                                }
                                            }

                                            // Przekazujemy aktualizację wszystkim
                                            broadcastToAll(connections, message)
                                        }
                                    }
                                }

                                "MANA_UPDATE" -> {
                                    if (parts.size >= 3) {
                                        val targetId = parts[1]
                                        val mana = parts[2].toIntOrNull() ?: 0

                                        playerStates[targetId]?.let { targetPlayer ->
                                            // Ustawiamy nową manę
                                            targetPlayer.setManaBase(mana)

                                            // Aktualizuj manę w wybranej postaci
                                            userManager.getUserById(targetId)?.let { user ->
                                                user.getSelectedCharacter()?.let { character ->
                                                    character.currentMana = targetPlayer.currentMana
                                                    userManager.updateUser(user)
                                                }
                                            }

                                            // Przekazujemy aktualizację wszystkim
                                            broadcastToAll(connections, message)
                                        }
                                    }
                                }

                                "RESPAWN" -> {
                                    if (parts.size >= 2) {
                                        val respawningPlayerId = parts[1]

                                        playerStates[respawningPlayerId]?.let { player ->
                                            // NAJPIERW pobierz pozycję respawnu
                                            val (respawnX, respawnY) = RespawnManager.getRespawnPoint()

                                            // Ustaw nową pozycję PRZED wywołaniem respawn()
                                            player.x = respawnX
                                            player.y = respawnY

                                            // Teraz wywołaj respawn (resetuje HP i isDead)
                                            player.respawn()

                                            // Aktualizuj zdrowie w wybranej postaci
                                            userManager.getUserById(respawningPlayerId)?.let { user ->
                                                user.getSelectedCharacter()?.let { character ->
                                                    character.currentHealth = player.maxHealth
                                                    userManager.updateUser(user)
                                                }
                                            }

                                            // Wysyłaj wiadomość z NOWYMI współrzędnymi respawnu
                                            val respawnMessage =
                                                "RESPAWN|$respawningPlayerId|${player.currentHealth}|${player.maxHealth}|${player.x}|${player.y}"
                                            broadcastToAll(connections, respawnMessage)

                                            println("Gracz $respawningPlayerId został respawnowany na (${player.x}, ${player.y})")
                                        }
                                    }
                                }

                                "CHAT" -> {
                                    if (parts.size >= 4) {
                                        val senderId = parts[1]
                                        val senderName = parts[2]
                                        val content = parts.subList(3, parts.size).joinToString("|")

                                        // === KOMENDY ===

                                        if (content.startsWith("/give ")) {
                                            val commandParts = content.split(" ")
                                            if (commandParts.size >= 2) {
                                                val itemId = commandParts[1]
                                                val result = itemManager.giveItemToPlayer(senderId, itemId, playerStates, userManager)

                                                if (result.isSuccess) {
                                                    send("CHAT|SERVER|System|${result.getOrNull()}")

                                                    // *** DODAJ AKTUALIZACJĘ STATYSTYK PO /give ***
                                                    val playerData = playerStates[senderId]
                                                    if (playerData != null) {
                                                        println("=== WYSYŁAM STATYSTYKI PO /give ===")
                                                        println("Player: $senderName, Item: $itemId")

                                                        // Wyślij aktualizację statystyk do gracza
                                                        val statsUpdate = "STATS_UPDATE|${playerData.getTotalStrength()}|${playerData.getTotalAgility()}|${playerData.getTotalSpellPower()}|${playerData.getTotalStamina()}|${playerData.maxHealth}"
                                                        println("Wysyłam: $statsUpdate")
                                                        send(statsUpdate)

                                                        // Rozgłoś aktualizację HP do wszystkich
                                                        val healthUpdate = "HEALTH_UPDATE|$senderId|${playerData.currentHealth}|${playerData.maxHealth}"
                                                        println("Wysyłam: $healthUpdate")
                                                        broadcastToAll(connections, healthUpdate)
                                                    } else {
                                                        println("ERROR: playerData is null dla gracza $senderId")
                                                    }
                                                } else {
                                                    send("CHAT|SERVER|System|Błąd: ${result.exceptionOrNull()?.message}")
                                                }
                                            }

                                        } else if (content.startsWith("/unequip ")) {
                                            val commandParts = content.split(" ")
                                            if (commandParts.size >= 2) {
                                                val itemType = commandParts[1].uppercase()
                                                try {
                                                    val itemTypeEnum = ItemType.valueOf(itemType)
                                                    val result = itemManager.unequipItem(senderId, itemTypeEnum, playerStates, userManager)
                                                    if (result.isSuccess) {
                                                        send("CHAT|SERVER|System|Zdjęto item z slotu $itemType")

                                                        // *** DODAJ AKTUALIZACJĘ STATYSTYK PO /unequip ***
                                                        val playerData = playerStates[senderId]
                                                        if (playerData != null) {
                                                            // Wyślij aktualizację statystyk do gracza
                                                            val statsUpdate = "STATS_UPDATE|${playerData.getTotalStrength()}|${playerData.getTotalAgility()}|${playerData.getTotalSpellPower()}|${playerData.getTotalStamina()}|${playerData.maxHealth}"
                                                            send(statsUpdate)

                                                            // Rozgłoś aktualizację HP do wszystkich
                                                            val healthUpdate = "HEALTH_UPDATE|$senderId|${playerData.currentHealth}|${playerData.maxHealth}"
                                                            broadcastToAll(connections, healthUpdate)
                                                        }
                                                    } else {
                                                        send("CHAT|SERVER|System|Błąd: ${result.exceptionOrNull()?.message}")
                                                    }
                                                } catch (e: Exception) {
                                                    send("CHAT|SERVER|System|Nieprawidłowy typ: $itemType (użyj: HELMET, ARMOR, PANTS, BOOTS, WEAPON)")
                                                }
                                            } else {
                                                send("CHAT|SERVER|System|Użyj: /unequip HELMET|ARMOR|PANTS|BOOTS|WEAPON")
                                            }

                                        } else if (content == "/lista" || content == "/items") {
                                            val allItems = itemManager.getAllItems()
                                            val itemsList = allItems.take(5).joinToString(", ") { it.id }
                                            send("CHAT|SERVER|System|Dostępne itemy: $itemsList (i ${allItems.size - 5} więcej)")

                                        } else if (content.startsWith("/manause ")) {
                                            val commandParts = content.split(" ")
                                            if (commandParts.size >= 2) {
                                                val amount = commandParts[1].toIntOrNull()
                                                if (amount != null && amount > 0) {
                                                    playerStates[senderId]?.let { player ->
                                                        if (player.useMana(amount)) {
                                                            // Aktualizuj mana w bazie danych
                                                            userManager.getUserById(senderId)?.let { user ->
                                                                user.getSelectedCharacter()?.let { character ->
                                                                    character.currentMana = player.currentMana
                                                                    userManager.updateUser(user)
                                                                }
                                                            }

                                                            // Wyślij aktualizację many
                                                            val manaUpdateMessage = "MANA_UPDATE|$senderId|${player.currentMana}|${player.maxMana}"
                                                            broadcastToAll(connections, manaUpdateMessage)

                                                            send("CHAT|SERVER|System|Użyto $amount many. Aktualna mana: ${player.currentMana}/${player.maxMana}")
                                                        } else {
                                                            send("CHAT|SERVER|System|Nie masz wystarczająco many! Aktualna: ${player.currentMana}/${player.maxMana}, potrzeba: $amount")
                                                        }
                                                    }
                                                } else {
                                                    send("CHAT|SERVER|System|Podaj prawidłową ilość many (liczba > 0). Użyj: /manause [ilość]")
                                                }
                                            } else {
                                                send("CHAT|SERVER|System|Użyj: /manause [ilość] - np. /manause 20")
                                            }
                                        } else {
                                            // === NORMALNA WIADOMOŚĆ CZATU ===
                                            val chatMessage = ChatManager.ChatMessage(senderId, senderName, content)
                                            chatManager.broadcastMessage(connections, chatMessage)
                                        }
                                    }
                                }

                                else -> {
                                    // Echo dla nieznanych komend
                                    send("Echo: $message")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Błąd: ${e.localizedMessage}")
                } finally {
                    // Usuwamy rozłączonego gracza
                    connections.remove(sessionId)
                    sessionCharacters.remove(sessionId)
                    playerSessions.remove(playerId)

                    if (playerId.isNotEmpty()) {
                        playerStates.remove(playerId)
                        playerMovementTargets.remove(playerId)
                        println("Gracz $username rozłączony (Pozostali gracze: ${playerStates.size})")

                        // Informujemy innych graczy o wyjściu
                        broadcastToAll(connections, "LEAVE|$playerId")
                    }
                    // Następnie zapisz wszystkich użytkowników
                    userManager.saveUsers()
                    println("Dane zostały zapisane")
                }
            }
        }

        // Uruchom automatyczny zapis danych graczy
        startAutoSaveLoop(playerStates, userManager, networkScope)

        // Start the game loop
        startGameLoop()

        // Dodaj obsługę zamknięcia
        Runtime.getRuntime().addShutdownHook(Thread {
            println("Zatrzymywanie serwera, zapisywanie danych użytkowników...")

            // Najpierw wykonaj wymuszony zapis aktywnych graczy
            runBlocking {
                forceSaveAllPlayerData(playerStates, userManager)
            }
            println("Dane zostały zapisane")
        })

    }.start(wait = true)
}