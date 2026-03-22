/*
 * MIT License
 *
 * Copyright (c) 2020 XenoAmess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xenoamess.cyan_potion.demo;

import com.xenoamess.cyan_potion.base.GameManager;
import com.xenoamess.cyan_potion.base.game_window_components.AbstractGameWindowComponent;
import com.xenoamess.cyan_potion.base.game_window_components.GameWindowComponentTreeNode;
import com.xenoamess.cyan_potion.base.game_window_components.controllable_game_window_components.Panel;
import com.xenoamess.cyan_potion.base.game_window_components.zsupport.CoordinateSystemMode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Example demonstrating the Z-axis coordinate system feature.
 * <p>
 * This example shows how to:
 * <ul>
 *   <li>Create components with different Z coordinates</li>
 *   <li>Use Z_AXIS_MODE for layered rendering</li>
 *   <li>Create modal dialogs with high Z values</li>
 *   <li>Maintain backward compatibility with LEGACY_MODE</li>
 * </ul>
 * </p>
 *
 * @author XenoAmess
 * @version 0.167.4
 * @since 2026-03-22
 */
public class ZAxisExample {

    /**
     * Main entry point for the Z-axis example.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Create configuration using Map
        Map<String, String> argsMap = new HashMap<>();
        argsMap.put("Title", "Z-Axis Example");
        argsMap.put("WindowWidth", "800");
        argsMap.put("WindowHeight", "600");

        GameManager gameManager = new GameManager(argsMap);
        
        // Create a component that initializes the example scene on its first update
        new ZAxisExampleInitializer(gameManager);
        
        gameManager.startup();
    }

    /**
     * Initializer component that creates the example scene after startup.
     */
    private static class ZAxisExampleInitializer extends AbstractGameWindowComponent {
        private final AtomicBoolean initialized = new AtomicBoolean(false);

        public ZAxisExampleInitializer(GameManager gameManager) {
            super(gameManager.getGameWindow());
        }

        @Override
        protected void initProcessors() {
            // No special processors needed
        }

        @Override
        public boolean update() {
            // Initialize scene on first update (after startup completes)
            if (initialized.compareAndSet(false, true)) {
                createExampleScene();
                // Close this initializer after setup
                this.close();
            }
            return super.update();
        }

        /**
         * Create the example scene with Z-axis components.
         */
        private void createExampleScene() {
            GameWindowComponentTreeNode root = this.getGameManager().getGameWindowComponentTree().getRoot();
            if (root == null) {
                System.err.println("Root node is null - cannot create example scene");
                return;
            }

            // Create a panel that uses Z-axis mode
            Panel zPanel = new Panel(this.getGameWindow());
            zPanel.setCoordinateSystemMode(CoordinateSystemMode.Z_AXIS_MODE);
            zPanel.setLeftTopPosX(50);
            zPanel.setLeftTopPosY(50);
            zPanel.setWidth(700);
            zPanel.setHeight(500);
            zPanel.addToGameWindowComponentTree(root);

            // Create layered components with different Z values
            createLayeredComponents(zPanel);

            // Create a modal dialog with high Z value
            createModalDialog(zPanel);
        }

        /**
         * Create components demonstrating Z-layering.
         *
         * @param parent the parent component
         */
        private void createLayeredComponents(Panel parent) {
            GameWindowComponentTreeNode parentNode = parent.getGameWindowComponentTreeNode();
            if (parentNode == null) {
                return;
            }

            // Background layer (Z = 0)
            Panel background = new Panel(this.getGameWindow());
            background.setZ(0.0f);  // Z = 0 (back)
            background.setLeftTopPosX(10);
            background.setLeftTopPosY(10);
            background.setWidth(680);
            background.setHeight(480);
            background.addToGameWindowComponentTree(parentNode);

            // Middle layer (Z = 10)
            Panel middle = new Panel(this.getGameWindow());
            middle.setZ(10.0f);  // Z = 10 (middle)
            middle.setLeftTopPosX(100);
            middle.setLeftTopPosY(100);
            middle.setWidth(500);
            middle.setHeight(300);
            middle.addToGameWindowComponentTree(parentNode);

            // Front layer (Z = 20)
            Panel front = new Panel(this.getGameWindow());
            front.setZ(20.0f);  // Z = 20 (front)
            front.setLeftTopPosX(200);
            front.setLeftTopPosY(200);
            front.setWidth(300);
            front.setHeight(200);
            front.addToGameWindowComponentTree(parentNode);

            System.out.println("Created layered components with Z values: 0, 10, 20");
        }

        /**
         * Create a modal dialog with very high Z value.
         *
         * @param parent the parent component
         */
        private void createModalDialog(Panel parent) {
            GameWindowComponentTreeNode parentNode = parent.getGameWindowComponentTreeNode();
            if (parentNode == null) {
                return;
            }

            // Modal dialog with Z = 1000 (appears on top of everything)
            Panel modalDialog = new Panel(this.getGameWindow());
            modalDialog.setZ(1000.0f);  // Very high Z to appear on top
            modalDialog.setLeftTopPosX(250);
            modalDialog.setLeftTopPosY(200);
            modalDialog.setWidth(300);
            modalDialog.setHeight(150);
            modalDialog.addToGameWindowComponentTree(parentNode);

            System.out.println("Created modal dialog with Z = 1000");
        }
    }
}
