package com.worktime.app.security

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class ManifestSecurityInvariantTest {
    @Test
    fun `only launcher activity is exported and components declare exported explicitly`() {
        val document = parseManifest()
        val components = COMPONENT_TAGS.flatMap { tag -> document.elements(tag) }

        components.forEach { component ->
            val name = component.androidAttribute("name")
            assertTrue(
                component.hasAttributeNS(ANDROID_NAMESPACE, "exported"),
                "$taggedName(component, name) must declare android:exported explicitly",
            )
        }

        val exported = components
            .filter { it.androidAttribute("exported") == "true" }
            .map { "${it.tagName}:${it.androidAttribute("name")}" }
            .toSet()

        assertEquals(setOf("activity:.MainActivity"), exported)
    }

    @Test
    fun `launcher activity exposes no deep link surface`() {
        val document = parseManifest()
        val mainActivity = document.elements("activity")
            .single { it.androidAttribute("name") == ".MainActivity" }
        val filters = mainActivity.childElements("intent-filter")
        val actions = filters
            .flatMap { it.childElements("action") }
            .map { it.androidAttribute("name") }
            .toSet()
        val categories = filters
            .flatMap { it.childElements("category") }
            .map { it.androidAttribute("name") }
            .toSet()
        val dataElements = filters.flatMap { it.childElements("data") }

        assertEquals(setOf("android.intent.action.MAIN"), actions)
        assertEquals(setOf("android.intent.category.LAUNCHER"), categories)
        assertTrue(dataElements.isEmpty(), "MainActivity must not expose URI/deep-link data filters")
        assertFalse("android.intent.action.VIEW" in actions, "MainActivity must not accept VIEW intents")
        assertFalse(
            "android.intent.category.BROWSABLE" in categories,
            "MainActivity must not be browser-addressable",
        )
    }

    private fun parseManifest(): Document {
        val manifest = File("src/main/AndroidManifest.xml")
        assertTrue(manifest.isFile, "Source AndroidManifest.xml must exist")
        return DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifest)
    }

    private fun Document.elements(tagName: String): List<Element> =
        getElementsByTagName(tagName).asElementList()

    private fun Element.childElements(tagName: String): List<Element> =
        childNodes.asElementList().filter { it.tagName == tagName }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(ANDROID_NAMESPACE, name)

    private fun org.w3c.dom.NodeList.asElementList(): List<Element> =
        (0 until length).mapNotNull { index -> item(index) as? Element }

    private fun taggedName(component: Element, name: String): String =
        "${component.tagName} $name"

    companion object {
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        private val COMPONENT_TAGS = listOf("activity", "activity-alias", "service", "receiver", "provider")
    }
}
