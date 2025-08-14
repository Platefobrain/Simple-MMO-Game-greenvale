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
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import pl.decodesoft.MMOGame
import pl.decodesoft.Strings.IP_ADDRESS

// Model danych dla logowania/rejestracji
@Serializable
data class AuthRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val success: Boolean, val message: String, val userId: String = "")

// Ekran logowania i rejestracji
class LoginScreen(private val game: MMOGame) : Screen {
    private lateinit var stage: Stage
    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var font: BitmapFont
    private lateinit var skin: Skin
    private lateinit var shapeRenderer: ShapeRenderer

    private var loginScope = CoroutineScope(Dispatchers.IO)
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private var errorMessage: String? = null
    private var isLoading = false

    private lateinit var usernameField: TextField
    private lateinit var passwordField: TextField
    private lateinit var rememberMeCheckBox: CheckBox
    private lateinit var statusLabel: Label

    private val prefs = Gdx.app.getPreferences("GreenValePreferences")

    override fun show() {
        camera = OrthographicCamera()
        viewport = FitViewport(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), camera)
        batch = SpriteBatch()
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/ChakraPetch-SemiBold.ttf"))
        val parameter = FreeTypeFontParameter().apply {
            size = 24
            characters = FreeTypeFontGenerator.DEFAULT_CHARS + "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
            color = Color.WHITE
        }
        font = generator.generateFont(parameter)
        generator.dispose()
        shapeRenderer = ShapeRenderer()

        stage = Stage(viewport, batch)
        Gdx.input.inputProcessor = stage

        skin = runCatching {
            Skin(Gdx.files.internal("assets/uiskin.json"))
        }.getOrElse {
            createBasicSkin()
        }

        createUI()

        // Wczytanie zapisanego loginu i ustawienie checkboxa
        val savedLogin = prefs.getString("savedLogin", "")
        if (savedLogin.isNotBlank()) {
            usernameField.text = savedLogin
            rememberMeCheckBox.isChecked = true
        }
    }

    private fun createBasicSkin(): Skin {
        val skin = Skin()
        skin.add("default", font)

        val textFieldStyle = TextField.TextFieldStyle().apply {
            fontColor = Color.WHITE
            background = skin.newDrawable("white", Color.DARK_GRAY)
            cursor = skin.newDrawable("white", Color.WHITE)
            selection = skin.newDrawable("white", Color.BLUE)
        }
        skin.add("default", textFieldStyle)

        val labelStyle = Label.LabelStyle().apply {
            fontColor = Color.WHITE
        }
        skin.add("default", labelStyle)

        return skin
    }

    private fun createUI() {
        val table = Table()
        table.setFillParent(true)

        val titleLabel = Label("GreenVale !", skin)
        titleLabel.setFontScale(2f)

        val usernameLabel = Label("Login:", skin)
        usernameField = TextField("", skin)

        val passwordLabel = Label("Hasło:", skin)
        passwordField = TextField("", skin)
        passwordField.isPasswordMode = true

        rememberMeCheckBox = CheckBox("", skin) // checkbox bez tekstu
        val rememberLabel = Label("Zapamiętaj login", skin)

        val rememberGroup = HorizontalGroup()
        rememberGroup.space(10f) // odstęp między checkboxem a labelką
        rememberGroup.addActor(rememberMeCheckBox)
        rememberGroup.addActor(rememberLabel)

        val loginButton = TextButton("Zaloguj się", skin)
        val registerButton = TextButton("Zarejestruj się", skin)

        statusLabel = Label("", skin)
        statusLabel.setAlignment(Align.center)

        val enterListener = object : InputListener() {
            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                    loginButton.toggle()  // jeśli jest toggle, albo inaczej:
                    loginButton.fire(ChangeListener.ChangeEvent()) // wywołaj zdarzenie zmiany (kliknięcie)
                    return true
                }
                return false
            }
        }

        usernameField.addListener(enterListener)
        passwordField.addListener(enterListener)

        // Układ interfejsu
        table.add(titleLabel).colspan(2).pad(20f)
        table.row()
        table.add(usernameLabel).padBottom(3f)
        table.add(usernameField).width(200f).pad(5f)
        table.row()
        table.add(passwordLabel).padBottom(3f)
        table.add(passwordField).width(200f)
        table.row()
        table.add(rememberGroup).colspan(2).padTop(10f)
        table.row().pad(5f)
        table.add(loginButton).colspan(2).width(130f).padTop(50f)
        table.row()
        table.add(registerButton).colspan(2).width(130f)
        table.row()
        table.add(statusLabel).width(100f).colspan(2).pad(20f)

        // Obsługa przycisków
        loginButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val username = usernameField.text
                val password = passwordField.text

                if (username.isBlank() || password.isBlank()) {
                    statusLabel.setText("Proszę wpisać nazwę użytkownika i hasło")
                    statusLabel.color = Color.RED
                    return
                }

                if (rememberMeCheckBox.isChecked) {
                    prefs.putString("savedLogin", username)
                } else {
                    prefs.remove("savedLogin")
                }
                prefs.flush()

                loginUser(username, password, statusLabel)
            }
        })

        registerButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val username = usernameField.text
                val password = passwordField.text

                if (username.isBlank() || password.isBlank()) {
                    statusLabel.setText("Proszę wpisać nazwę użytkownika i hasło")
                    statusLabel.color = Color.RED
                    return
                }

                registerUser(username, password, statusLabel)
            }
        })

        stage.addActor(table)

        // Przyciski w prawym dolnym rogu
        val bottomRightTable = Table()
        bottomRightTable.bottom().right()
        bottomRightTable.setFillParent(true)

        val optionsButton = TextButton("Opcje", skin)
        val exitButton = TextButton("Exit Game", skin)

        bottomRightTable.add(optionsButton).width(150f).pad(0f, 0f, 5f, 30f).row()
        bottomRightTable.add(exitButton).width(150f).pad(0f, 0f, 30f, 30f).row()

        optionsButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                println("Opcje kliknięte")
                // Tu możesz dodać ekran opcji
            }
        })

        exitButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                Gdx.app.exit()
            }
        })

        stage.addActor(bottomRightTable)
    }

    // W klasie LoginScreen w metodzie loginUser, zmień:
    private fun loginUser(username: String, password: String, statusLabel: Label) {
        isLoading = true
        statusLabel.setText("Logowanie...")
        statusLabel.color = Color.YELLOW

        loginScope.launch {
            try {
                val response = httpClient.post("http://$IP_ADDRESS/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequest(username, password))
                }

                val authResponse = Json.decodeFromString<AuthResponse>(response.bodyAsText())

                if (authResponse.success) {
                    statusLabel.setText("Zalogowano pomyślnie!")
                    statusLabel.color = Color.GREEN

                    // Przejście do ekranu wyboru postaci po krótkim opóźnieniu
                    delay(1000)

                    // KLUCZOWA ZMIANA TUTAJ - bezpośrednio ustawiamy dane w MMOGame
                    Gdx.app.postRunnable {
                        // Ustaw dane użytkownika w głównej klasie gry PRZED przejściem do następnego ekranu
                        game.username = username
                        game.localPlayerId = authResponse.userId

                        println("Ustawiono dane użytkownika w MMOGame: ${game.username}, ID: ${game.localPlayerId}")

                        // Teraz dopiero tworzymy ekran wyboru postaci
                        game.showCharacterSelectionScreen()
                    }
                } else {
                    statusLabel.setText(authResponse.message)
                    statusLabel.color = Color.RED
                }
                isLoading = false
            } catch (e: Exception) {
                Gdx.app.postRunnable {
                    statusLabel.setText("Connection error: ${e.message}")
                    statusLabel.color = Color.RED
                    isLoading = false
                }
            }
        }
    }

    private fun registerUser(username: String, password: String, statusLabel: Label) {
        isLoading = true
        statusLabel.setText("Rejestrowanie...")
        statusLabel.color = Color.YELLOW

        loginScope.launch {
            try {
                val response = httpClient.post("http://$IP_ADDRESS/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequest(username, password))
                }

                val authResponse = Json.decodeFromString<AuthResponse>(response.bodyAsText())

                withContext(Dispatchers.Default) {
                    if (authResponse.success) {
                        statusLabel.setText("Rejestracja udana! Teraz możesz się zalogować.")
                        statusLabel.color = Color.GREEN
                    } else {
                        statusLabel.setText(authResponse.message)
                        statusLabel.color = Color.RED
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Default) {
                    statusLabel.setText("Connection error: ${e.message}")
                    statusLabel.color = Color.RED
                    isLoading = false
                }
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined

        stage.act(delta)
        stage.draw()

        // Wyświetlanie komunikatu o błędzie
        if (errorMessage != null) {
            batch.begin()
            font.color = Color.RED
            font.draw(batch, errorMessage, 400f, 100f, 0f, Align.center, false)
            batch.end()
        }

        // Animacja ładowania
        if (isLoading) {
            shapeRenderer.projectionMatrix = camera.combined
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            val time = (System.currentTimeMillis() % 2000) / 2000f
            val centerX = Gdx.graphics.width / 2f

            for (i in 0 until 3) {
                val alpha = (time + i * 0.33f) % 1f
                val size = 10f * (1f - abs(alpha * 2 - 1))
                shapeRenderer.color = Color(1f, 1f, 1f, 1f)
                // Center the dots horizontally (centerX) while keeping vertical position (50f)
                shapeRenderer.circle(centerX + (i - 1) * 30f, 50f, size)
            }
            shapeRenderer.end()
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
        stage.dispose()
        batch.dispose()
        font.dispose()
        shapeRenderer.dispose()
        loginScope.cancel()
        httpClient.close()
    }
}