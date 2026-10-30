package com.xenoamess.cyan_potion.civilization.gui;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CityPanelComponent.
 * <p>
 * Due to the OpenGL dependency required by GameWindow, direct instantiation
 * of CityPanelComponent is not feasible in a headless test environment.
 * These tests verify the public API contract via reflection.
 */
class CityPanelComponentTest {

    @Test
    void testClassExists() {
        assertDoesNotThrow(() -> Class.forName(
            "com.xenoamess.cyan_potion.civilization.gui.CityPanelComponent"
        ));
    }

    @Test
    void testShowMethodExists() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> clazz = Class.forName(
            "com.xenoamess.cyan_potion.civilization.gui.CityPanelComponent"
        );
        Method showMethod = clazz.getMethod("show",
            Class.forName("com.xenoamess.cyan_potion.civilization.map.City")
        );
        assertNotNull(showMethod);
        assertEquals(void.class, showMethod.getReturnType());
    }

    @Test
    void testHideMethodExists() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> clazz = Class.forName(
            "com.xenoamess.cyan_potion.civilization.gui.CityPanelComponent"
        );
        Method hideMethod = clazz.getMethod("hide");
        assertNotNull(hideMethod);
        assertEquals(void.class, hideMethod.getReturnType());
    }

    @Test
    void testSetOnRulerClickMethodExists() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> clazz = Class.forName(
            "com.xenoamess.cyan_potion.civilization.gui.CityPanelComponent"
        );
        Method method = clazz.getMethod("setOnRulerClick", java.util.function.Consumer.class);
        assertNotNull(method);
        assertEquals(void.class, method.getReturnType());
    }

    @Test
    void testIsVisibleMethodExists() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> clazz = Class.forName(
            "com.xenoamess.cyan_potion.civilization.gui.CityPanelComponent"
        );
        Method method = clazz.getMethod("isVisible");
        assertNotNull(method);
        assertEquals(boolean.class, method.getReturnType());
    }
}
