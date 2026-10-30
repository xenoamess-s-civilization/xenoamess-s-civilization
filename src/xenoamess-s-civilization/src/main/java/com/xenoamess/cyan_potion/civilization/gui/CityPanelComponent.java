/**
 * Copyright (C) 2020 XenoAmess
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xenoamess.cyan_potion.civilization.gui;

import com.xenoamess.cyan_potion.base.GameWindow;
import com.xenoamess.cyan_potion.base.game_window_components.controllable_game_window_components.AbstractControllableGameWindowComponent;
import com.xenoamess.cyan_potion.base.game_window_components.controllable_game_window_components.Button;
import com.xenoamess.cyan_potion.base.render.Texture;
import com.xenoamess.cyan_potion.base.visual.Picture;
import com.xenoamess.cyan_potion.civilization.character.Person;
import com.xenoamess.cyan_potion.civilization.map.City;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector4f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static com.xenoamess.cyan_potion.base.render.Texture.STRING_PURE_COLOR;

/**
 * A panel component that displays city information and its ruler.
 *
 * @author XenoAmess
 * @version 0.167.3-SNAPSHOT
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
@ToString
public class CityPanelComponent extends AbstractControllableGameWindowComponent {

    @Getter
    @Setter
    private City city;

    @Getter
    @Setter
    private boolean show = false;

    @Getter
    private final Button rulerButton;

    @Getter
    @Setter
    private Consumer<Person> onRulerClick;

    private final Texture backgroundTexture;
    private final Picture backgroundPicture = new Picture();

    // Colors
    private static final Vector4f COLOR_TITLE = new Vector4f(1.0f, 0.9f, 0.6f, 1.0f);
    private static final Vector4f COLOR_LABEL = new Vector4f(0.7f, 0.7f, 0.7f, 1.0f);
    private static final Vector4f COLOR_VALUE = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector4f COLOR_RULER_HOVER = new Vector4f(0.9f, 0.8f, 0.5f, 1.0f);

    /**
     * Creates a new CityPanelComponent.
     *
     * @param gameWindow the game window
     */
    public CityPanelComponent(GameWindow gameWindow) {
        super(gameWindow);

        this.backgroundTexture = this.getResourceManager().fetchResource(
            Texture.class,
            STRING_PURE_COLOR,
            "",
            "0.1,0.1,0.12,0.98"
        );
        this.backgroundPicture.setBindable(backgroundTexture);

        this.rulerButton = new Button(gameWindow, null, "");
        this.rulerButton.registerOnMouseButtonLeftDownCallback(event -> {
            if (onRulerClick != null && city != null && city.getRuler() != null) {
                onRulerClick.accept(city.getRuler());
            }
            return null;
        });
    }

    /**
     * Shows the panel for the specified city.
     *
     * @param city the city to display
     */
    public void show(@NotNull City city) {
        this.city = city;
        this.show = true;
        updateRulerButton();
        log.debug("Showing city panel for: {}", city.getName());
    }

    /**
     * Hides the panel.
     */
    public void hide() {
        this.show = false;
        this.city = null;
        log.debug("Hiding city panel");
    }

    /**
     * Updates the ruler button text and visibility based on current city.
     */
    private void updateRulerButton() {
        if (city == null || city.getRulerId() == null) {
            rulerButton.setVisible(false);
            rulerButton.setActive(false);
            rulerButton.setButtonText("");
        } else {
            Person ruler = city.getRuler();
            String rulerName = ruler != null ? ruler.getName() : "未知";
            rulerButton.setButtonText(rulerName);
            rulerButton.setVisible(true);
            rulerButton.setActive(true);
        }
    }

    @Override
    public boolean ifVisibleThenDraw() {
        if (!show || city == null) {
            return false;
        }

        // Draw background
        backgroundPicture.cover(this);
        backgroundPicture.draw(getGameWindow());

        drawCityInfo();
        drawRulerSection();

        return super.ifVisibleThenDraw();
    }

    private void drawCityInfo() {
        float x = getLeftTopPosX();
        float y = getLeftTopPosY();
        float centerX = x + getWidth() / 2;

        // City name (title)
        getGameWindow().drawTextCenter(
            null,
            centerX,
            y + 30,
            20,
            0,
            COLOR_TITLE,
            city.getName()
        );

        // City type
        String typeText = "类型: " + city.getType().getDisplayName();
        getGameWindow().drawTextCenter(
            null,
            centerX,
            y + 65,
            14,
            0,
            COLOR_VALUE,
            typeText
        );

        // Resident count
        String residentText = "人口: " + city.getResidentCount();
        getGameWindow().drawTextCenter(
            null,
            centerX,
            y + 90,
            14,
            0,
            COLOR_VALUE,
            residentText
        );
    }

    private void drawRulerSection() {
        float x = getLeftTopPosX();
        float y = getLeftTopPosY();
        float centerX = x + getWidth() / 2;

        // Ruler label
        getGameWindow().drawTextCenter(
            null,
            centerX,
            y + 130,
            14,
            0,
            COLOR_LABEL,
            "控制人"
        );

        if (city == null || city.getRulerId() == null) {
            getGameWindow().drawTextCenter(
                null,
                centerX,
                y + 155,
                14,
                0,
                COLOR_VALUE,
                "无主"
            );
        } else {
            // Draw ruler button
            float buttonWidth = Math.min(getWidth() - 40, 200);
            float buttonHeight = 30;
            float buttonX = centerX - buttonWidth / 2;
            float buttonY = y + 145;

            rulerButton.setLeftTopPos(buttonX, buttonY);
            rulerButton.setSize(buttonWidth, buttonHeight);

            // Update hover state
            float mouseX = getGameWindow().getMousePosX();
            float mouseY = getGameWindow().getMousePosY();
            boolean isHover = mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                    && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;

            if (isHover) {
                rulerButton.setTextColor(COLOR_RULER_HOVER);
            } else {
                rulerButton.setTextColor(COLOR_VALUE);
            }
        }
    }

    @Override
    public boolean isVisible() {
        return show;
    }

    @Override
    public void setVisible(boolean visible) {
        this.show = visible;
        super.setVisible(visible);
    }

    @Override
    public boolean update() {
        if (show && rulerButton.isVisible()) {
            rulerButton.update();
        }
        return super.update();
    }

    @Override
    public com.xenoamess.cyan_potion.base.events.Event process(com.xenoamess.cyan_potion.base.events.Event event) {
        if (show && rulerButton.isVisible() && rulerButton.isActive()) {
            event = rulerButton.process(event);
            if (event == null) {
                return null;
            }
        }
        return super.process(event);
    }

    @Override
    public void addToGameWindowComponentTree(com.xenoamess.cyan_potion.base.game_window_components.GameWindowComponentTreeNode node) {
        super.addToGameWindowComponentTree(node);
        this.rulerButton.addToGameWindowComponentTree(this.getGameWindowComponentTreeNode());
    }

    @Override
    public void close() {
        rulerButton.close();
        super.close();
    }
}
