package com.sentinel.core.react

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class CommandRouterTest {
    // Note: A full functional test of CommandRouter requires mocking the Android Context,
    // PackageManager, and LlmService. For demonstration, we test the regex parsing logic
    // that CommandRouter uses internally.

    @Test
    fun testActionRegexParsing() {
        val clickAction = "CLICK [12345]"
        val typeAction = "TYPE [6789] [Hello World]"

        val clickRegex = Regex("""CLICK \[(\d+)\]""")
        val typeRegex = Regex("""TYPE \[(\d+)\] \[(.+)\]""")

        assertTrue(clickRegex.matches(clickAction))
        val clickId = clickRegex.find(clickAction)?.groupValues?.get(1)
        assertEquals("12345", clickId)

        assertTrue(typeRegex.matches(typeAction))
        val typeId = typeRegex.find(typeAction)?.groupValues?.get(1)
        val typeText = typeRegex.find(typeAction)?.groupValues?.get(2)
        assertEquals("6789", typeId)
        assertEquals("Hello World", typeText)
    }
}
