package com.xenoamess.cyan_potion.base.mod

import groovy.test.GroovyTestCase

/**
 * Test for GroovyScriptEngine functionality.
 */
class GroovyScriptEngineTest extends GroovyTestCase {

    void testGroovyEnvironment() {
        // Verify Groovy is working
        def message = "Hello from Groovy!"
        assert message == "Hello from Groovy!"
    }

    void testGroovyScriptBasics() {
        // Test basic Groovy features that mods might use
        def list = [1, 2, 3, 4, 5]
        def doubled = list.collect { it * 2 }
        assert doubled == [2, 4, 6, 8, 10]
    }

    void testGroovyMaps() {
        def map = [name: "TestMod", version: "1.0.0"]
        assert map.name == "TestMod"
        assert map.version == "1.0.0"
    }

    void testGroovyClosures() {
        def greet = { name -> "Hello, $name!" }
        assert greet("Mod") == "Hello, Mod!"
    }
}
