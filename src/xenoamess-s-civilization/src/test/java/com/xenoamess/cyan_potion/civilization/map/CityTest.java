package com.xenoamess.cyan_potion.civilization.map;

import com.xenoamess.cyan_potion.civilization.cache.PersonCache;
import com.xenoamess.cyan_potion.civilization.character.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityTest {

    @BeforeEach
    void setUp() {
        PersonCache.ALL_PERSON_CACHE.clear();
        PersonCache.LIKELY_ALIVE_PERSON_CACHE.clear();
    }

    @Test
    void testRulerIdGetterSetter() {
        City city = new City("city-1", "Test City", 0.5f, 0.5f, City.CityType.CITY);
        assertNull(city.getRulerId());

        city.setRulerId("person-1");
        assertEquals("person-1", city.getRulerId());
    }

    @Test
    void testGetRulerWhenNoRuler() {
        City city = new City("city-1", "Test City", 0.5f, 0.5f, City.CityType.CITY);
        assertNull(city.getRuler());
    }

    @Test
    void testGetRulerReturnsPerson() {
        Person person = new Person("person-1");
        person.setName("Ruler");
        PersonCache.ALL_PERSON_CACHE.put(person.getId(), person);

        City city = new City("city-1", "Test City", 0.5f, 0.5f, City.CityType.CITY);
        city.setRulerId("person-1");

        assertNotNull(city.getRuler());
        assertEquals("person-1", city.getRuler().getId());
        assertEquals("Ruler", city.getRuler().getName());
    }

    @Test
    void testGetRulerWhenPersonNotInCache() {
        City city = new City("city-1", "Test City", 0.5f, 0.5f, City.CityType.CITY);
        city.setRulerId("missing-person");

        assertNull(city.getRuler());
    }
}
