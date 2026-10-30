package com.xenoamess.cyan_potion.civilization.gui;

import com.xenoamess.cyan_potion.civilization.cache.PersonCache;
import com.xenoamess.cyan_potion.civilization.character.Person;
import com.xenoamess.cyan_potion.civilization.map.City;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the city panel to person detail navigation flow.
 * <p>
 * Due to OpenGL dependency, these tests verify the data and callback integration
 * layers rather than direct UI instantiation.
 */
class CityPanelIntegrationTest {

    @BeforeEach
    void setUp() {
        PersonCache.ALL_PERSON_CACHE.clear();
        PersonCache.LIKELY_ALIVE_PERSON_CACHE.clear();
    }

    @Test
    void testCityRulerResolutionThroughCache() {
        Person ruler = new Person("ruler-1");
        ruler.setName("刘备");
        PersonCache.ALL_PERSON_CACHE.put(ruler.getId(), ruler);

        City city = new City("city-1", "成都", 0.5f, 0.5f, City.CityType.CAPITAL);
        city.setRulerId("ruler-1");

        assertNotNull(city.getRuler());
        assertEquals("ruler-1", city.getRuler().getId());
        assertEquals("刘备", city.getRuler().getName());
    }

    @Test
    void testNoRulerState() {
        City city = new City("city-1", "空城", 0.5f, 0.5f, City.CityType.VILLAGE);
        assertNull(city.getRulerId());
        assertNull(city.getRuler());
    }

    @Test
    void testRulerClickCallbackContract() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> clazz = Class.forName(
            "com.xenoamess.cyan_potion.civilization.gui.CityPanelComponent"
        );

        // Verify setOnRulerClick exists
        assertDoesNotThrow(() -> clazz.getMethod("setOnRulerClick", java.util.function.Consumer.class));

        // Verify getRulerButton exists
        assertDoesNotThrow(() -> clazz.getMethod("getRulerButton"));
    }

    @Test
    void testRulerIdUpdateReflectsNewRuler() {
        Person oldRuler = new Person("old-ruler");
        oldRuler.setName("Old");
        Person newRuler = new Person("new-ruler");
        newRuler.setName("New");

        PersonCache.ALL_PERSON_CACHE.put(oldRuler.getId(), oldRuler);
        PersonCache.ALL_PERSON_CACHE.put(newRuler.getId(), newRuler);

        City city = new City("city-1", "Test", 0.5f, 0.5f, City.CityType.CITY);
        city.setRulerId("old-ruler");
        assertEquals("Old", city.getRuler().getName());

        city.setRulerId("new-ruler");
        assertEquals("New", city.getRuler().getName());
    }
}
