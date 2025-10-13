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

package pl.decodesoft.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.decodesoft.MMOGame
import pl.decodesoft.Strings
import pl.decodesoft.Strings.IP_ADDRESS
import pl.decodesoft.player.Race
import pl.decodesoft.player.skin.Direction
import pl.decodesoft.player.skin.PlayerSkinManager

@Serializable
data class CharactersListRequest(val userId: String)

@Serializable
data class CharactersListResponse(
    val success: Boolean,
    val message: String,
    val characters: List<CharacterInfo> = emptyList()
)

@Serializable
data class DeleteCharacterRequest(val userId: String, val characterId: String)

@Serializable
data class DeleteCharacterResponse(val success: Boolean, val message: String)

@Serializable
data class CharacterInfo(
    val id: String,
    val nickname: String,
    val characterClass: Int,
    val race: String = "HUMAN",
    val maxHealth: Int = 100,
    val currentHealth: Int = 100,
    val level: Int = 1,
    val experience: Int = 0,
    var lastX: Float = 500f,
    var lastY: Float = 600f,
    var spellPower: Int = 0,
    var strength: Int = 0,
    var agility: Int = 0,
    var stamina: Int = 0,
    val faction: String = "NONE"
)

@Serializable
data class ServerInfoRequest(val userId: String)

@Serializable
data class ServerInfoResponse(
    val success: Boolean,
    val serverInfo: ServerInfo? = null
)

@Serializable
data class ServerInfo(
    val realmName: String = "Solaris",
    val realmType: String = "PVP",
    val realmDescription: String = "Solaris – progresywny realm PvP pełen wyzwań.",
    val changelogText: String = "Brak informacji"
)

class CharacterSelectionScreen(
    private val game: MMOGame,
    private val userId: String,
    private val username: String
) : Screen {
    private lateinit var stage: Stage
    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var font: BitmapFont
    private lateinit var smallFont: BitmapFont
    private lateinit var skin: Skin

    private var skinPreviewBatch: SpriteBatch? = null
    private var skinManager: PlayerSkinManager? = null

    private lateinit var archerTexture: Texture
    private lateinit var mageTexture: Texture
    private lateinit var warriorTexture: Texture
    private lateinit var emptySlotTexture: Texture

    private val uiLock = Any()
    private var isUICreated = false

    private var characters = mutableListOf<CharacterInfo?>()
    private var selectedCharacterIndex: Int = -1

    private var serverInfo: ServerInfo = ServerInfo()

    private lateinit var characterImageContainer: Container<Actor>

    private var selectionScope = CoroutineScope(Dispatchers.IO)
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override fun show() {
        camera = OrthographicCamera()
        viewport = FitViewport(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), camera)
        batch = SpriteBatch()

        // Dodaj to:
        skinPreviewBatch = SpriteBatch()
        skinManager = PlayerSkinManager()

        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/ChakraPetch-SemiBold.ttf"))

        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = 32
            characters = FreeTypeFontGenerator.DEFAULT_CHARS + "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
            color = Color.WHITE
        }
        font = generator.generateFont(parameter)

        val smallParameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = 20
            characters = FreeTypeFontGenerator.DEFAULT_CHARS + "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
            color = Color.WHITE
        }
        smallFont = generator.generateFont(smallParameter)

        generator.dispose()

        stage = Stage(viewport, batch)
        Gdx.input.inputProcessor = stage

        skin = runCatching {
            Skin(Gdx.files.internal("assets/uiskin.json")).also {
                if (!it.has("title", Label.LabelStyle::class.java)) {
                    val titleStyle = Label.LabelStyle().apply {
                        font = this@CharacterSelectionScreen.font
                        fontColor = Color(1f, 0.9f, 0.6f, 1f)
                    }
                    it.add("title", titleStyle)
                }
            }
        }.getOrElse {
            createBasicSkin()
        }

        try {
            archerTexture = Texture(Gdx.files.internal("textures/archer/archer.png"))
            mageTexture = Texture(Gdx.files.internal("textures/mage/mage.png"))
            warriorTexture = Texture(Gdx.files.internal("textures/warrior/warrior.png"))
            emptySlotTexture = Texture(Gdx.files.internal("textures/empty_slot.png"))
        } catch (e: Exception) {
            Gdx.app.error("CharacterSelection", "Nie można załadować tekstur: ${e.message}")
        }

        for (i in 0 until 3) {
            characters.add(null)
        }

        loadServerInfo()
        loadCharacters()
    }

    private fun loadServerInfo() {
        selectionScope.launch {
            try {
                val response = httpClient.post("http://$IP_ADDRESS/server/info") {
                    contentType(ContentType.Application.Json)
                    setBody(ServerInfoRequest(userId))
                }

                val serverInfoResponse = response.body<ServerInfoResponse>()

                if (serverInfoResponse.success && serverInfoResponse.serverInfo != null) {
                    serverInfo = serverInfoResponse.serverInfo
                }
            } catch (e: Exception) {
                Gdx.app.error("CharacterSelection", "Błąd pobierania info o serwerze: ${e.message}")
            }
        }
    }

    private fun loadCharacters() {
        selectionScope.launch {
            try {
                val response = httpClient.post("http://$IP_ADDRESS/character/list") {
                    contentType(ContentType.Application.Json)
                    setBody(CharactersListRequest(userId))
                }

                val charactersResponse = response.body<CharactersListResponse>()

                if (charactersResponse.success) {
                    for (i in 0 until 3) {
                        characters[i] = null
                    }

                    charactersResponse.characters.forEachIndexed { index, character ->
                        if (index < 3) {
                            characters[index] = character
                        }
                    }

                    if (characters[0] != null) {
                        selectedCharacterIndex = 0
                    }

                    // ZMIANA: Zawsze resetuj flagę przed tworzeniem UI
                    Gdx.app.postRunnable {
                        synchronized(uiLock) {
                            if (::stage.isInitialized && ::skin.isInitialized) {
                                isUICreated = false  // RESETUJ flagę
                                createUI()
                                isUICreated = true   // Ustaw z powrotem
                            }
                        }
                    }
                } else {
                    Gdx.app.error("CharacterSelection", "Błąd pobierania postaci: ${charactersResponse.message}")
                    Gdx.app.postRunnable {
                        synchronized(uiLock) {
                            if (::stage.isInitialized && ::skin.isInitialized) {
                                isUICreated = false
                                createUI()
                                isUICreated = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("CharacterSelection", "Błąd połączenia: ${e.message}")
                Gdx.app.postRunnable {
                    synchronized(uiLock) {
                        if (::stage.isInitialized && ::skin.isInitialized) {
                            isUICreated = false
                            createUI()
                            isUICreated = true
                        }
                    }
                }
            }
        }
    }

    private fun createBasicSkin(): Skin {
        val skin = Skin()

        skin.add("default-font", font)
        skin.add("small-font", smallFont)

        val textButtonStyle = TextButton.TextButtonStyle().apply {
            font = smallFont
            fontColor = Color.WHITE
            downFontColor = Color.LIGHT_GRAY
            up = skin.newDrawable("white", Color(0.2f, 0.2f, 0.2f, 0.8f))
            down = skin.newDrawable("white", Color(0.3f, 0.3f, 0.3f, 0.9f))
            over = skin.newDrawable("white", Color(0.4f, 0.4f, 0.4f, 0.9f))
        }
        skin.add("default", textButtonStyle)

        val labelStyle = Label.LabelStyle().apply {
            font = smallFont
            fontColor = Color.WHITE
        }
        skin.add("default", labelStyle)

        val titleStyle = Label.LabelStyle().apply {
            font = this@CharacterSelectionScreen.font
            fontColor = Color(1f, 0.9f, 0.6f, 1f)
        }
        skin.add("title", titleStyle)

        return skin
    }

    private fun createUI() {
        stage.clear()

        val mainTable = Table()
        mainTable.setFillParent(true)
        mainTable.background = skin.newDrawable("white", Color(0.1176f, 0.1176f, 0.1176f, 1f))

        // LEWY PANEL - Informacje o serwerze
        val leftPanel = Table()
        leftPanel.background = skin.newDrawable("white", Color(0.149f, 0.149f, 0.149f, 1f))
        leftPanel.align(Align.top)

        val realmLabel = Label("Realm: ${serverInfo.realmName} (${serverInfo.realmType})", skin, "title")
        realmLabel.setFontScale(0.7f)
        leftPanel.add(realmLabel).pad(20f, 10f, 10f, 10f).top().left()
        leftPanel.row()

        val realmInfoLabel = Label(serverInfo.realmDescription, skin)
        leftPanel.add(realmInfoLabel).pad(5f, 10f, 20f, 10f).top().left()
        leftPanel.row()

        val serverInfoPanel = Table()
        serverInfoPanel.background = skin.newDrawable("white", Color(0.1176f, 0.1176f, 0.1176f, 1f))
        serverInfoPanel.pad(15f)

        val titleLabel = Label("Changelog", skin, "title")
        titleLabel.setFontScale(0.8f)
        serverInfoPanel.add(titleLabel).pad(10f).top().left()
        serverInfoPanel.row()

        val changelogLabel = Label(serverInfo.changelogText, skin)
        changelogLabel.wrap = true
        changelogLabel.setAlignment(Align.topLeft)
        serverInfoPanel.add(changelogLabel).expand().fill().pad(10f).top().left()

        leftPanel.add(serverInfoPanel).expand().fill().pad(10f)

        // ŚRODKOWY PANEL
        val centerPanel = Table()
        centerPanel.align(Align.center)

        characterImageContainer = Container<Actor>()
        updateCharacterImage()
        centerPanel.add(characterImageContainer).size(400f, 400f).center()
        centerPanel.row()

        val enterWorldButton = TextButton("Wejdź do gry", skin)
        enterWorldButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (selectedCharacterIndex >= 0 && characters[selectedCharacterIndex] != null) {
                    selectCharacter(selectedCharacterIndex, characters[selectedCharacterIndex]!!)
                }
            }
        })
        centerPanel.add(enterWorldButton).width(300f).height(60f).pad(20f).center()

        // PRAWY PANEL
        val rightPanel = Table()
        rightPanel.background = skin.newDrawable("white", Color(0.149f, 0.149f, 0.149f, 1f))
        rightPanel.align(Align.top)

        val characterListTable = Table()
        characterListTable.align(Align.top)
        characterListTable.background = skin.newDrawable("white", Color(0.1176f, 0.1176f, 0.1176f, 1f))
        characterListTable.pad(10f)

        val listTitleLabel = Label("Postacie", skin, "title")
        characterListTable.add(listTitleLabel).pad(20f, 10f, 15f, 10f).top()
        characterListTable.row()

        for (i in 0 until 3) {
            val characterSlot = createCharacterListItem(i)
            characterListTable.add(characterSlot).width(300f).height(80f).pad(5f)
            characterListTable.row()
        }

        characterListTable.add().expand().fill()
        characterListTable.row()

        val deleteButton = TextButton("Usuń postać", skin)
        deleteButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (selectedCharacterIndex >= 0 && characters[selectedCharacterIndex] != null) {
                    val character = characters[selectedCharacterIndex]!!
                    showConfirmDeleteDialog(character.nickname) {
                        selectionScope.launch {
                            deleteCharacter(userId, character.id)
                            loadCharacters()
                        }
                    }
                }
            }
        })
        characterListTable.add(deleteButton).width(280f).height(40f).pad(10f).bottom()

        // ScrollPane - nadpisanie tła, aby usunąć niebieską ramkę
        val scrollPane = ScrollPane(characterListTable, skin)
        scrollPane.setScrollingDisabled(true, false)
        scrollPane.setOverscroll(false, false)
        scrollPane.style.background = skin.newDrawable("white", Color(0f, 0f, 0f, 0f))

        rightPanel.add(scrollPane).expand().fill().top().pad(10f)
        rightPanel.row()

        val buttonPanel = Table()

        val createButton = TextButton("Stwórz nową postać", skin)
        val emptySlot = characters.indexOfFirst { it == null }

        if (emptySlot < 0) {
            createButton.isDisabled = true
            createButton.color = Color.GRAY
        }

        createButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val slot = characters.indexOfFirst { it == null }
                if (slot >= 0) {
                    createNewCharacter(slot)
                }
            }
        })

        val backButton = TextButton("Wyloguj", skin)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.switchToLoginScreen()
            }
        })

        buttonPanel.add(createButton).width(280f).height(45f).pad(5f)
        buttonPanel.row()
        buttonPanel.add(backButton).width(280f).height(40f).pad(5f)

        rightPanel.add(buttonPanel).pad(10f).bottom()

        mainTable.add(leftPanel).width(350f).expandY().fillY().pad(10f)
        mainTable.add(centerPanel).expand().fill()
        mainTable.add(rightPanel).width(350f).expandY().fillY().pad(10f)

        stage.addActor(mainTable)
    }

    private fun createCharacterListItem(slotIndex: Int): Table {
        val character = if (slotIndex < characters.size) characters[slotIndex] else null

        val itemTable = Table()
        val isSelected = selectedCharacterIndex == slotIndex

        itemTable.touchable = com.badlogic.gdx.scenes.scene2d.Touchable.enabled

        // Określ kolor tła na podstawie frakcji
        val backgroundColor = if (character != null) {
            when (character.faction) {
                "WATAHA" -> {
                    // Wataha - odcienie pomarańczowo-czerwone
                    if (isSelected) Color(0.8f, 0.4f, 0.2f, 1f) // Wyraźnie jaśniejszy gdy wybrany
                    else Color(0.6f, 0.25f, 0.12f, 0.85f) // Standardowy kolor Watahy
                }
                "ZAKON" -> {
                    // Zakon - odcienie niebieskie
                    if (isSelected) Color(0.3f, 0.5f, 0.85f, 1f) // Wyraźnie jaśniejszy gdy wybrany
                    else Color(0.2f, 0.35f, 0.65f, 0.85f) // Standardowy kolor Zakonu
                }
                else -> {
                    // Brak frakcji - neutralny szary
                    if (isSelected) Color(0.5f, 0.5f, 0.6f, 1f)
                    else Color(0.25f, 0.25f, 0.3f, 0.8f)
                }
            }
        } else {
            // Pusty slot - ciemny szary
            if (isSelected) Color(0.5f, 0.5f, 0.6f, 1f)
            else Color(0.149f, 0.149f, 0.149f, 1f)
        }

        itemTable.background = skin.newDrawable("white", backgroundColor)

        if (character != null) {
            val nameLabel = Label(character.nickname, skin, "title")
            nameLabel.setFontScale(0.8f)

            val levelLabel = Label("${Strings.LEVEL} ${character.level}", skin)
            val classLabel = Label(getClassName(character.characterClass), skin)
            classLabel.color = getClassColor(character.characterClass)

            itemTable.add(nameLabel).left().expandX().pad(10f, 10f, 5f, 10f)
            itemTable.row()

            val infoRow = Table()
            infoRow.add(levelLabel).left().padRight(5f)
            infoRow.add(classLabel).left()

            itemTable.add(infoRow).left().expandX().pad(0f, 10f, 10f, 10f)

            itemTable.addListener(object : com.badlogic.gdx.scenes.scene2d.InputListener() {
                override fun touchDown(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    return true
                }

                override fun touchUp(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                    selectedCharacterIndex = slotIndex
                    createUI()
                }
            })

        } else {
            val emptyLabel = Label("Pusty slot", skin)
            emptyLabel.color = Color(0.6f, 0.6f, 0.6f, 1f)
            itemTable.add(emptyLabel).expand().center().pad(30f)
        }

        return itemTable
    }

    private fun updateCharacterImage() {
        characterImageContainer.clear()

        if (selectedCharacterIndex >= 0 && characters[selectedCharacterIndex] != null) {
            val character = characters[selectedCharacterIndex]!!

            if (skinManager != null) {
                // Stwórz custom actor do renderowania skina
                val skinPreviewActor = object : Actor() {
                    override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch?, parentAlpha: Float) {
                        batch?.end()

                        // Użyj osobnego batcha dla skina
                        skinPreviewBatch?.let { skinBatch ->
                            if (batch != null) {
                                skinBatch.projectionMatrix = batch.projectionMatrix
                            }
                            skinBatch.begin()

                            // Pobierz rasę postaci
                            val previewRace = when (character.race) {
                                "GOBLIN" -> Race.GOBLIN
                                "HUMAN" -> Race.HUMAN
                                "UNDEAD" -> Race.UNDEAD
                                "ELF" -> Race.ELF
                                else -> Race.HUMAN
                            }

                            // Pobierz skin z cache
                            val skin = skinManager!!.getSkinForRace(previewRace, character.characterClass)

                            // Renderuj w centrum aktora
                            val centerX = x + width / 2
                            val centerY = y + height / 2 - 30f

                            skin?.renderIdle(skinBatch, centerX, centerY, 3f, Direction.DOWN)

                            skinBatch.end()
                        }

                        batch?.begin()
                    }
                }

                skinPreviewActor.setSize(400f, 400f)
                characterImageContainer.actor = skinPreviewActor
                characterImageContainer.size(400f, 400f)
                characterImageContainer.fill()
                return
            }

            // Fallback - użyj tekstur klas jeśli skinManager nie jest dostępny
            if (::archerTexture.isInitialized && ::mageTexture.isInitialized && ::warriorTexture.isInitialized) {
                val texture = when (character.characterClass) {
                    0 -> archerTexture
                    1 -> mageTexture
                    else -> warriorTexture
                }
                val image = Image(texture)
                characterImageContainer.actor = image
                characterImageContainer.size(400f, 400f)
            } else {
                val placeholder = Table()
                placeholder.background = skin.newDrawable("white", getClassColor(character.characterClass))
                characterImageContainer.actor = placeholder
                characterImageContainer.size(400f, 400f)
            }
        } else {
            val emptyPlaceholder = Table()
            emptyPlaceholder.background = skin.newDrawable("white", Color(0.2f, 0.2f, 0.2f, 0.5f))
            characterImageContainer.actor = emptyPlaceholder
            characterImageContainer.size(400f, 400f)
        }
    }

    private fun getClassName(classIndex: Int): String {
        return when (classIndex) {
            0 -> "Łucznik"
            1 -> "Mag"
            else -> "Wojownik"
        }
    }

    private fun getClassColor(classIndex: Int): Color {
        return when (classIndex) {
            0 -> Color(0.67f, 0.83f, 0.45f, 1f)
            1 -> Color(0.41f, 0.8f, 0.94f, 1f)
            else -> Color(0.78f, 0.61f, 0.43f, 1f)
        }
    }

    private fun selectCharacter(slotIndex: Int, character: CharacterInfo) {
        selectionScope.launch {
            try {
                val response = httpClient.post("http://$IP_ADDRESS/character/select") {
                    contentType(ContentType.Application.Json)
                    setBody(CharacterSelectRequest(userId, slotIndex))
                }

                val selectResponse = response.body<CharacterSelectResponse>()

                if (selectResponse.success) {
                    Gdx.app.postRunnable {
                        game.startGame(username, userId, character.characterClass, character.nickname,
                            level = character.level, experience = character.experience)
                    }
                } else {
                    Gdx.app.error("CharacterSelection", "Błąd wyboru postaci: ${selectResponse.message}")
                }
            } catch (e: Exception) {
                Gdx.app.error("CharacterSelection", "Błąd połączenia: ${e.message}")
            }
        }
    }

    private fun createNewCharacter(slotIndex: Int) {
        game.showCharacterCreationScreen(slotIndex)
    }

    private suspend fun deleteCharacter(userId: String, characterId: String) {
        try {
            val request = DeleteCharacterRequest(userId, characterId)
            val response = httpClient.post("http://$IP_ADDRESS/character/delete") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val deleteResponse = response.body<DeleteCharacterResponse>()

            if (deleteResponse.success) {
                selectedCharacterIndex = -1
                loadCharacters()
            } else {
                Gdx.app.postRunnable {
                    showErrorDialog(deleteResponse.message)
                }
            }
        } catch (e: Exception) {
            Gdx.app.postRunnable {
                showErrorDialog("Nie udało się połączyć z serwerem")
            }
        }
    }

    private fun showErrorDialog(message: String) {
        val dialog = Dialog("Błąd", skin)
        dialog.text(message)
        dialog.button("OK")
        dialog.isModal = true
        dialog.isMovable = false
        dialog.show(stage)
    }

    private fun showConfirmDeleteDialog(characterName: String, onConfirm: () -> Unit) {
        val dialog = object : Dialog("Usuń postać", skin) {
            override fun result(obj: Any?) {
                if (obj == true) {
                    onConfirm()
                }
            }
        }
        dialog.text("Czy na pewno chcesz usunąć postać '$characterName'?")
        dialog.button("Tak", true)
        dialog.button("Nie", false)
        dialog.isModal = true
        dialog.isMovable = false
        dialog.show(stage)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined

        // Dodaj update animacji skinów
        skinManager?.update(delta)

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        stage.dispose()
        batch.dispose()
        skinPreviewBatch?.dispose()
        skinManager?.dispose()
        font.dispose()
        smallFont.dispose()

        if (::archerTexture.isInitialized) archerTexture.dispose()
        if (::mageTexture.isInitialized) mageTexture.dispose()
        if (::warriorTexture.isInitialized) warriorTexture.dispose()
        if (::emptySlotTexture.isInitialized) emptySlotTexture.dispose()

        selectionScope.cancel()
        httpClient.close()
    }
}

@Serializable
data class CharacterSelectRequest(val userId: String, val characterSlot: Int)

@Serializable
data class CharacterSelectResponse(val success: Boolean, val message: String)