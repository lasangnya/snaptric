package com.snaptric.feature.properties.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Villa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PropertyIconProviderTest {

    @Test
    fun `getIcon with key home returns Home icon`() {
        val icon = PropertyIconProvider.getIcon("home")
        assertEquals(Icons.Default.Home, icon)
    }

    @Test
    fun `getIcon with key apartment returns Apartment icon`() {
        val icon = PropertyIconProvider.getIcon("apartment")
        assertEquals(Icons.Default.Apartment, icon)
    }

    @Test
    fun `getIcon with key business returns Business icon`() {
        val icon = PropertyIconProvider.getIcon("business")
        assertEquals(Icons.Default.Business, icon)
    }

    @Test
    fun `getIcon with key cottage returns Cottage icon`() {
        val icon = PropertyIconProvider.getIcon("cottage")
        assertEquals(Icons.Default.Cottage, icon)
    }

    @Test
    fun `getIcon with key villa returns Villa icon`() {
        val icon = PropertyIconProvider.getIcon("villa")
        assertEquals(Icons.Default.Villa, icon)
    }

    @Test
    fun `getIcon with unknown key returns Home icon as default`() {
        val icon = PropertyIconProvider.getIcon("unknown_key")
        assertEquals(Icons.Default.Home, icon)
    }

    @Test
    fun `getIcon with empty string returns Home icon as default`() {
        val icon = PropertyIconProvider.getIcon("")
        assertEquals(Icons.Default.Home, icon)
    }

    @Test
    fun `IconsList contains exactly 5 entries`() {
        assertEquals(5, PropertyIconProvider.IconsList.size)
    }

    @Test
    fun `IconsList entries are all non-null`() {
        for ((key, icon) in PropertyIconProvider.IconsList) {
            assertNotNull("Icon for key '$key' should not be null", icon)
        }
    }
}
