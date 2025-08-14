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

import pl.decodesoft.ui.ItemManager
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import pl.decodesoft.Strings.IP_ADDRESS
import pl.decodesoft.enemy.EnemyClient
import pl.decodesoft.klasy.Archer
import pl.decodesoft.klasy.Mage
import pl.decodesoft.klasy.Warrior
import pl.decodesoft.klasy.skile.SkileManager
import pl.decodesoft.map.GameMap
import pl.decodesoft.msg.ChatSystem
import pl.decodesoft.network.MessageManager
import pl.decodesoft.player.Player
import pl.decodesoft.player.PlayerController
import pl.decodesoft.screens.CharacterCreationScreen
import pl.decodesoft.screens.CharacterSelectionScreen
import pl.decodesoft.screens.DeathScreen
import pl.decodesoft.screens.LoginScreen
import pl.decodesoft.settings.Menu
import pl.decodesoft.states.*
import pl.decodesoft.ui.GameUI
import pl.decodesoft.ui.UISkin
import pl.decodesoft.ui.character.ClientItem
import pl.decodesoft.ui.inventory.InventoryItem
import java.util.concurrent.ConcurrentHashMap

// Główny kod gry
class MMOGame : ApplicationAdapter() {
    private lateinit var currentState: GameState
    lateinit var batch: SpriteBatch
    lateinit var uiBatch: SpriteBatch
    lateinit var shapeRenderer: ShapeRenderer
    lateinit var font: BitmapFont
    lateinit var camera: OrthographicCamera
    lateinit var gameMap: GameMap
    lateinit var chatSystem: ChatSystem
    lateinit var menu: Menu
    lateinit var messageManager: MessageManager
    private lateinit var skileManager: SkileManager
    private var deathScreen: DeathScreen? = null


    // itemmanager
    lateinit var itemManager: ItemManager

    var gameUI: GameUI? = null


    // Dane gracza
    var localPlayerId = "player_${System.currentTimeMillis()}"
    var username = "Guest"
    lateinit var localPlayer: Player
    val layout = GlyphLayout()

    // Mapa wszystkich graczy
    val players = ConcurrentHashMap<String, Player>()
    val pathTiles = mutableListOf<Pair<Int, Int>>()

    // Komunikacja
    lateinit var networkScope: CoroutineScope
    private var client: HttpClient? = null
    var session: DefaultWebSocketSession? = null

    // Obsługa gracza i umiejętności
    lateinit var playerController: PlayerController

    // wrogowie enemymanager
    val enemies = ConcurrentHashMap<String, EnemyClient>()
    var enemyUpdateTimer = 0f
    val enemyUpdateInterval = 1f

    private var loginScreen: LoginScreen? = null
    private var characterSelectionScreen: CharacterSelectionScreen? = null
    private var characterCreationScreen: CharacterCreationScreen? = null

    // Metoda wywoływana z ItemMessageHandler
    fun handleItemMoved(fromType: String, fromSlot: Int, toType: String, toSlot: Int, itemId: String) {
        println("DEBUG: MMOGame.handleItemMoved wywołane")

        try {
            itemManager.handleItemMoved(fromType, fromSlot, toType, toSlot, itemId)
            itemManager.debugPrintInventory()
        } catch (e: Exception) {
            println("DEBUG: ItemManager nie jest dostępny: ${e.message}")
        }
    }

    fun getItemDefinition(itemId: String): ClientItem? {
        return itemManager.getItemDefinition(itemId)
    }

    fun setInventoryItem(slot: Int, item: InventoryItem) {
        itemManager.setInventoryItem(slot, item)
    }

    // Odświeża UI ekwipunku postaci
    fun refreshEquipmentUI() {
        try {
            gameUI?.refreshCharacterWindow()
            println("DEBUG: Odświeżono UI ekwipunku")
        } catch (e: Exception) {
            println("DEBUG: Błąd odświeżania UI ekwipunku: ${e.message}")
        }
    }

    // Poproś serwer o aktualizację inventory
    fun requestInventoryUpdate() {
        try {
            sendWebSocketMessage("GET_PLAYER_INVENTORY|${localPlayer.id}")
            println("DEBUG: Wysłano żądanie aktualizacji inventory")
        } catch (e: Exception) {
            println("DEBUG: Błąd wysyłania żądania inventory: ${e.message}")
        }
    }

    fun refreshInventoryUI() {
        try {
            println("DEBUG: Odświeżam UI inventory")
        } catch (e: Exception) {
            println("DEBUG: Błąd odświeżania UI inventory: ${e.message}")
        }
    }

    override fun create() {
        deathScreen = DeathScreen(this)
        batch = SpriteBatch()
        uiBatch = SpriteBatch()
        shapeRenderer = ShapeRenderer()

        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/ChakraPetch-SemiBold.ttf"))
        val parameter = FreeTypeFontParameter().apply {
            size = 15
            characters = FreeTypeFontGenerator.DEFAULT_CHARS + "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
            color = Color.WHITE

            borderWidth = 0f // BOLD czcionki
            borderColor = Color.WHITE
        }
        font = generator.generateFont(parameter)
        font.data.markupEnabled = true
        generator.dispose()

        camera = OrthographicCamera()
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        // Inicjalizacja zakresu coroutine dla komunikacji sieciowej
        networkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // Inicjalizacja ekranu logowania
        loginScreen = LoginScreen(this)
        loginScreen?.show()

        // wiadomosci graczy
        messageManager = MessageManager(this)

        // menu gry
        menu = Menu(this)

        // ITEMMANAGER - inicjalizuj PRZED gameUI
        itemManager = ItemManager(this)
        itemManager.initialize()

        // GAMEUI - inicjalizuj PO itemManager
        gameUI = GameUI(this)

        // POŁĄCZ ItemManager z GameUI
        gameUI!!.connectItemManager()

        // Ustaw początkowy stan gry
        changeState(LoginState(this))
    }

    // metoda zmian stanu
    fun changeState(newState: GameState) {
        // Jeśli już istnieje stan, wywołaj wyjście
        if (::currentState.isInitialized) {
            currentState.exit()
        }

        // Ustawienie nowego stanu
        currentState = newState

        // Wywołanie wejścia do nowego stanu
        currentState.enter()
    }

    // Pokazuje ekran wyboru postaci
    fun showCharacterSelectionScreen() {
        changeState(CharacterSelectionState(this))
    }

    // Pokazuje ekran tworzenia postaci
    fun showCharacterCreationScreen(slotIndex: Int) {
        changeState(CharacterCreationState(this, slotIndex))
    }

    // Przełącza na ekran logowania
    fun switchToLoginScreen() {
        changeState(LoginState(this))
    }

    // Chat
    fun receiveNetworkChatMessage(senderId: String, senderName: String, content: String) {
        chatSystem.receiveMessage(senderId, senderName, content)
    }

    // combat log
    fun receiveNetworkCombatLog(content: String) {
        chatSystem.addLogMessage(content)
    }

    // metoda do aktualizacji przeciwników (w tym usuwania ciał)
    private fun updateEnemies(deltaTime: Float) {
        // Aktualizuj wszystkich przeciwników
        enemies.values.forEach { enemy ->
            enemy.update(deltaTime)
        }

        // Usuń przeciwników, których ciała powinny zniknąć
        val toRemove = enemies.filter { (_, enemy) -> enemy.canBeRemoved() }
        toRemove.forEach { (id, _) ->
            enemies.remove(id)
        }

        // Opcjonalnie: loguj usuwanie ciał (do debugowania)
        if (toRemove.isNotEmpty()) {
            Gdx.app.debug("EnemyManager", "Removed ${toRemove.size} corpses")
        }
    }

    // Aktualizuje istniejącego przeciwnika lub tworzy nowego
    fun updateEnemy(id: String, x: Float, y: Float, type: String, hp: Int, maxHp: Int, level: Int = 1, state: String = "IDLE"): EnemyClient {

        return enemies[id]?.let { existingEnemy ->
            existingEnemy.updateTargetPosition(x, y)
            existingEnemy.currentHealth = hp
            existingEnemy.maxHealth = maxHp
            existingEnemy.level = level
            existingEnemy.updateState(state)
            existingEnemy.isAlive = hp > 0
            existingEnemy

        } ?: run {
            val newEnemy = EnemyClient(id, x, y, type, hp, maxHp, level, state)
            newEnemy.isAlive = hp > 0
            enemies[id] = newEnemy
            newEnemy
        }
    }

    // Teleportuje przeciwnika do nowej pozycji (dla respawnu)
    fun respawnEnemy(id: String, x: Float, y: Float, type: String, hp: Int, maxHp: Int, level: Int = 1, state: String): EnemyClient {
        return enemies[id]?.let { enemy ->
            enemy.teleportToPosition(x, y)
            enemy.currentHealth = hp
            enemy.maxHealth = maxHp
            enemy.level = level
            enemy.updateState(state)
            enemy.isAlive = true
            enemy
        } ?: run {
            val newEnemy = EnemyClient(id, x, y, type, hp, maxHp, level, state)
            newEnemy.isAlive = true
            enemies[id] = newEnemy
            newEnemy
        }
    }

    // Aktualizuje zdrowie przeciwnika
    fun updateEnemyHealth(id: String, damage: Int): Boolean {
        return enemies[id]?.let { enemy ->
            enemy.currentHealth -= damage
            if (enemy.currentHealth <= 0) {
                enemy.currentHealth = 0
                enemy.markAsDead() // Użyj nowej metody markAsDead()
            }
            true
        } ?: false
    }

    // Oznacza przeciwnika jako martwego
    fun markEnemyAsDead(id: String): Boolean {
        return enemies[id]?.let { enemy ->
            enemy.markAsDead() // Użyj nowej metody markAsDead()
            enemy.isSelected = false
            true
        } ?: false
    }

    // Dodaje nowego gracza do gry
    fun addPlayer(
        id: String,
        x: Float,
        y: Float,
        username: String,
        characterClass: Int,
        currentHealth: Int,
        maxHealth: Int,
        currentMana: Int,
        maxMana: Int,
        level: Int = 1,
        experience: Int = 0
    ) {
        if (id != localPlayerId) {
            val newPlayer = Player(x, y, id, username, characterClass, level = level, experience = experience)
            newPlayer.currentHealth = currentHealth
            newPlayer.maxHealth = maxHealth
            newPlayer.currentMana = currentMana
            newPlayer.maxMana = maxMana
            players[id] = newPlayer
        }
    }

    // Wyświetla notyfikację graczowi
    fun showNotification(message: String, type: String) {
        val senderId = "system"
        val senderName = "System"

        val formattedMessage = when (type) {
            "levelup" -> message
            else -> message
        }

        println(formattedMessage)
        receiveNetworkChatMessage(senderId, senderName, formattedMessage)
    }

    fun startLogoutCountdown() {
        networkScope.launch {
            for (i in 5 downTo 1) {
                Gdx.app.postRunnable {
                    receiveNetworkChatMessage("SYSTEM", "System", "Wylogowanie za $i...")
                }
                delay(1000L)
            }
            Gdx.app.postRunnable {
                logoutToCharacterSelection()
            }
        }
    }

    private fun logoutToCharacterSelection() {
        // Wyślij informację do serwera
        session?.let {
            networkScope.launch {
                try {
                    it.send(Frame.Text("LEAVE_WORLD|${localPlayer.id}"))
                    delay(100) // Daj czas na wysłanie
                } catch (e: Exception) {
                    println("Błąd przy wysyłaniu LEAVE_WORLD: ${e.message}")
                } finally {
                    closeConnection()
                }
            }
        }

        removePlayer(localPlayer.id)
        showCharacterSelectionScreen()
    }

    // Aktualizuje pozycję gracza
    fun updatePlayerPosition(id: String, x: Float, y: Float) {
        players[id]?.let { player ->
            if (id == localPlayerId) {
                // Korekta serwera dla lokalnego gracza
                player.setServerPosition(x, y)
            } else {
                // Ustawienie celu ruchu dla innych graczy
                player.setMoveTarget(x, y)
            }
        }
    }

    //Obsługuje nieudany ruch gracza
    fun handleMoveFailed(playerId: String, reason: String) {
        if (playerId == localPlayerId) {
            // Anuluj ruch lokalnego gracza
            localPlayer.setMoveTarget(localPlayer.x, localPlayer.y)

            // Wyświetl informację dla gracza
            val messageText = when (reason) {
                "no_path" -> "Nie mogę tam dojść!"
                else -> "Ruch niemożliwy!"
            }
            playerController.addDamageText(localPlayer.x, localPlayer.y + 20f, messageText, Color.YELLOW)
        }
    }

    // Usuwa gracza z gry
    fun removePlayer(id: String) {
        if (players.containsKey(id)) {
            players.remove(id)
            println("Gracz $username wylogował się z gry (players size=${players.size})")
        } else {
            println("Nie znaleziono gracza $username do wylogowania (players size=${players.size})")
        }
    }

    // Aktualizuje zdrowie gracza
    fun updatePlayerHealth(playerId: String, currentHealth: Int, maxHealth: Int) {
        players[playerId]?.let { player ->
            player.currentHealth = currentHealth
            player.maxHealth = maxHealth
        }
    }

    // Aktualizuje zdrowie gracza
    fun updatePlayerMana(playerId: String, currentMana: Int, maxMana: Int) {
        players[playerId]?.let { player ->
            player.currentMana = currentMana
            player.maxMana = maxMana
        }
    }

    // Bezpośrednio ustawia zdrowie przeciwnika (bez odejmowania)
    fun updateEnemyHealthExplicit(enemyId: String, currentHealth: Int, maxHealth: Int) {
        enemies[enemyId]?.let { enemy ->
            enemy.currentHealth = currentHealth
            enemy.maxHealth = maxHealth
            if (currentHealth <= 0) {
                enemy.markAsDead() // Użyj nowej metody markAsDead()
            } else {
                enemy.isAlive = true
            }
        }
    }

    // Pobiera gracza po ID
    fun getPlayer(playerId: String): Player? {
        return players[playerId]
    }

    // Pobiera przeciwnika po ID
    fun getEnemy(enemyId: String): EnemyClient? {
        return enemies[enemyId]
    }

    // Dodaje efekt tekstowy obrażeń
    fun addDamageText(x: Float, y: Float, text: String, color: Color) {
        playerController.addDamageText(x, y, text, color)
    }

    // Obsługuje śmierć gracza
    fun handlePlayerDeath(playerId: String) {
        if (playerId == localPlayerId) {
            Gdx.app.postRunnable {
                changeState(DeadState(this))
                deathScreen?.show()
            }
        }
    }

    // pozycja gracza po respie
    fun respawnPlayerWithPosition(playerId: String, currentHealth: Int, maxHealth: Int, x: Float, y: Float) {
        players[playerId]?.let { player ->
            player.currentHealth = currentHealth
            player.maxHealth = maxHealth
            player.x = x
            player.y = y

            if (playerId == localPlayerId) {
                localPlayer.x = x
                localPlayer.y = y
                camera.position.set(x, y, 0f)
                camera.update()

                //  zmiana stanu gry
                if (currentState is DeadState && playerId == localPlayerId) {
                    Gdx.app.postRunnable {
                        changeState(PlayingState(this))
                    }
                }
            }
        }
    }

    // Obsługuje odrodzenie gracza
    fun respawnPlayerHealth(playerId: String, currentHealth: Int, maxHealth: Int) {
        players[playerId]?.let { player ->
            player.currentHealth = currentHealth
            player.maxHealth = maxHealth
        }
    }

    // Obsługuje wiadomość o ataku
    fun handleAttackMessage(attackType: String, parts: List<String>) {
        //println("DEBUG: handleAttackMessage called with: $attackType, parts: ${parts.joinToString()}")

        playerController.handleMessage(attackType, parts)

        // Przekaż wiadomość do SkileManager
        if (::skileManager.isInitialized) {
            skileManager.handleSkillMessage(attackType, parts)
            //println("DEBUG: Message passed to SkileManager")
        }
    }

    // Aktualizuje dane ścieżki pathfinding
    fun updatePathTiles(pathData: List<Pair<Int, Int>>) {
        pathTiles.clear()
        pathTiles.addAll(pathData)
    }

    fun sendWebSocketMessage(message: String) {
        networkScope.launch {
            try {
                val currentSession = session
                if (currentSession != null) {
                    currentSession.send(Frame.Text(message))
                } else {
                    Gdx.app.error("WebSocket", "Cannot send message - no active session")
                }
            } catch (e: Exception) {
                Gdx.app.error("WebSocket", "Error sending message: ${e.message}")
            }
        }
    }

    // Metoda wywoływana po pomyślnym wyborze postaci
    fun startGame(userUsername: String, userId: String, characterClass: Int = 2, nickname: String = userUsername, level: Int = 1, experience: Int = 0) {
        networkScope.coroutineContext.cancelChildren()
        networkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        username = userUsername
        localPlayerId = userId

        // Tworzenie gracza z wybraną klasą postaci i nickiem
        localPlayer = Player(
            id = localPlayerId,
            username = nickname,
            characterClass = characterClass,
            level = level,
            experience = experience
        )

        // Inicjalizacja czatu
        chatSystem = ChatSystem(
            localPlayerId = localPlayerId,
            username = nickname,
            networkScope = networkScope,
            getSession = { session }
        )

        // Dodajemy lokalnego gracza do mapy
        players[localPlayerId] = localPlayer

        // Inicjalizacja menedżera umiejętności
        skileManager = SkileManager(
            localPlayerId = localPlayerId,
            enemies = enemies
        )

        // Inicjalizacja GameUI - tworzymy tutaj, ale będzie używane w PlayingState
        val gameUIInstance = GameUI(this)
        gameUI = gameUIInstance

        //itemy
        gameUI!!.connectItemManager()

        // Inicjalizacja obsługi gracza i umiejętności
        playerController = PlayerController(
            localPlayer = localPlayer,
            players = players,
            enemies = enemies,
            camera = camera,
            networkScope = networkScope,
            getSession = { session },
            characterClass = when (localPlayer.characterClass) {
                0 -> Archer(localPlayer, networkScope, { session }, skileManager, messageManager)
                1 -> Mage(localPlayer, networkScope, { session }, skileManager, messageManager)
                else -> Warrior(localPlayer, networkScope, { session }, skileManager, messageManager)
            },
            skileManager = skileManager,
            font = font,
            gameUI = gameUIInstance,
            chatSystem = chatSystem
        )

        // Uruchomienie połączenia websocket
        connectToServer()

        // Zmiana stanu gry
        changeState(LoadingState(this))
    }

    private fun connectToServer() {
        networkScope.launch {
            try {
                client = HttpClient(CIO) {
                    install(WebSockets)
                }

                client?.webSocket("ws://$IP_ADDRESS/ws") {
                    session = this

                    // Wyślij JOIN
                    val characterClass = localPlayer.characterClass
                    val playerId = localPlayer.id
                    send(Frame.Text("JOIN|0|0|$playerId|$username|$characterClass"))

                    // === DODAJ TO - poproś o ekwipunek ===
                    send(Frame.Text("GET_PLAYER_EQUIPMENT|$playerId"))

                    // Załaduj przeciwników
                    send(Frame.Text("GET_ENEMIES"))

                    // Odbieraj wiadomości
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val message = frame.readText()
                            processMessage(message)
                        }
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("WebSocket", "Błąd połączenia: ${e.message}")
            }
        }
    }

    // zamyka połączenie bez reconnect
    private suspend fun closeConnection() {
        try {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Logout"))
            println("Sesja została zamknięta")
        } catch (e: Exception) {
            println("Błąd przy zamykaniu sesji: ${e.message}")
        }

        session = null
        client?.close()
        client = null
    }

    private fun processMessage(message: String) {
        messageManager.processMessage(message)
    }

    override fun render() {
        // Czyścimy ekran
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Obsługa wejścia aktualnego stanu
        currentState.handleInput()

        // Aktualizacja logiki
        updateEnemies(Gdx.graphics.deltaTime)
        currentState.update(Gdx.graphics.deltaTime)

        // Renderowanie aktualnego stanu
        currentState.render(Gdx.graphics.deltaTime)

        // Renderujemy menu
        menu.render(Gdx.graphics.deltaTime)

        //itemy
        try {
            gameUI?.handleDebugKeys()
        } catch (e: Exception) {
            // GameUI lub ItemManager jeszcze nie gotowe - ignoruj
        }
    }

    // podazanie za graczem
    fun updateCamera() {
        // Interpolacja liniowa dla płynnego śledzenia
        val lerpFactor = 0.5f // Wartość od 0 do 1, im bliżej 1, tym szybsze śledzenie
        val targetX = localPlayer.x
        val targetY = localPlayer.y

        camera.position.x += (targetX - camera.position.x) * lerpFactor
        camera.position.y += (targetY - camera.position.y) * lerpFactor
    }

    // Respawn graczy
    fun respawnPlayer() {
        Gdx.app.log("Respawn", "Respawn requested")

        // Pobierz aktualny stan gry
        val currentDeadState = currentState as? DeadState

        if (currentDeadState != null) {
            // Jeśli jesteśmy w stanie śmierci, użyj jego metody do odradzania
            currentDeadState.handleRespawn()
        } else {
            Gdx.app.error("Respawn", "Cannot respawn - not in death state")
        }
    }

    override fun resize(width: Int, height: Int) {
        // Przekierowanie do aktualnego stanu
        currentState.resize(width, height)
        menu.resize(width, height)
    }

    override fun dispose() {
        batch.dispose()
        uiBatch.dispose()
        shapeRenderer.dispose()
        font.dispose()
        loginScreen?.dispose()
        characterSelectionScreen?.dispose()
        characterCreationScreen?.dispose()
        deathScreen?.dispose()
        gameMap.dispose()
        menu.dispose()
        UISkin.dispose()

        // Zamknięcie połączenia websocket
        runBlocking {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "App dispose"))
            client?.close()
        }

        // Anulowanie wszystkich koroutyn
        networkScope.cancel()
    }

    // Punkt wejścia dla aplikacji desktopowej
    object Launcher {
        @JvmStatic
        fun main(args: Array<String>) {
            val config = Lwjgl3ApplicationConfiguration()
            config.setTitle("MMO Game")
            //config.setWindowedMode(900, 700)
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())
            config.setForegroundFPS(60)
            Lwjgl3Application(MMOGame(), config)
        }
    }
}