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

object Strings {

    // Konfiguracja serwera
    const val IP_ADDRESS = "localhost:8081" // Adres IP serwera 83.168.71.31

    // Character classes
    const val CLASS_ARCHER = "Łucznik"
    const val CLASS_MAGE = "Mag"
    const val CLASS_WARRIOR = "Wojownik"

    // Stats
    const val STAT_STRENGTH = "Siła"
    const val STAT_AGILITY = "Agility"
    const val STAT_SPELL_POWER = "Spell Power"
    const val STAT_STAMINA = "Stamina"
    const val STAT_MANA = "Mana"


    // Attack names
    const val ATTACK_ARROW_SHOT = "Strzał"
    const val ATTACK_FIREBALL = "Kula ognia"
    const val ATTACK_SWORD_STRIKE = "Atak mieczem"

    // UI Elements
    const val PLAYERS_ONLINE = "Gracze online"
    const val LOGGED_AS = "Zalogowany jako"
    const val LEVEL = "Level"
    const val SELECTED_PLAYER = "Zaznaczony gracz"

    // Character creation
    const val CREATE_CHARACTER = "Stwórz nową postać"
    const val CHARACTER_NAME = "Nazwa postaci"
    const val CREATE_BUTTON = "Stwórz postać"
    const val CANCEL_BUTTON = "Anuluj"

    // Class descriptions
    const val ARCHER_DESC = "Specjalizuje się w atakach dystansowych\ni wysokich obrażeniach pojedynczego celu."
    const val MAGE_DESC = "Włada potężną magią obszarową\ni potrafi kontrolować pole bitwy."
    const val WARRIOR_DESC = "Wytrzymały czempion walczący wręcz,\nidealny do obrony sojuszników."

    // Help text
    const val KEYBOARD_SHORTCUTS = "Skróty klawiszowe"
    const val HELP_ESC = "ESC - Pokaż/ukryj pomoc"
    const val HELP_RIGHT_MOUSE = "Prawy przycisk myszy - Atak/ruch"
    const val HELP_WARRIOR_CHARGE = "Q - Szarża w kierunku zaznaczonego celu lub kursora"

    // Enemy types
    const val ENEMY_SHEEP = "Mała Owca"
    const val ENEMY_WOLF = "Wilk"
    const val ENEMY_BEAR = "Niedźwiedź"
    const val ENEMY_SPIDER = "Pajong"

    // Combat messages
    const val CANT_REACH = "Nie mogę tam dojść!"
    const val MOVE_IMPOSSIBLE = "Ruch niemożliwy!"
    const val RESPAWN_BUTTON = "Respawn"
    const val PLAYER_DIED = "Zginąłeś!"

    // Chat
    const val CHAT_HINT = "Naciśnij Enter, aby czatować"
    const val SAY_PREFIX = "Say"
}