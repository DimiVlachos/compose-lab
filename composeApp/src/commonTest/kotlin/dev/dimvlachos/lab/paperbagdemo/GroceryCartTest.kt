package dev.dimvlachos.lab.paperbagdemo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroceryCartTest {
    private val bread = Groceries.byId("bread")
    private val celery = Groceries.byId("celery")
    private val apples = Groceries.byId("apples")

    @Test
    fun anAddCountsOnItsRowWhileItFliesAndInTheBagOnceItLands() {
        val cart = GroceryCart()
        val entry = assertNotNull(cart.started(bread))
        assertEquals(1, cart.quantity(bread))
        assertEquals(0, cart.count)
        cart.landed(entry.key)
        assertEquals(1, cart.quantity(bread))
        assertEquals(1, cart.count)
        assertEquals(349, cart.totalCents)
    }

    @Test
    fun theBagHoldsNineCountingWhatIsInTheAir() {
        val cart = GroceryCart()
        repeat(8) { cart.landed(cart.started(apples)!!.key) }
        assertNotNull(cart.started(bread))
        assertTrue(cart.full)
        assertNull(cart.started(celery))
    }

    @Test
    fun entriesKeepTheirSlotsWhenAnEarlierOneLeaves() {
        val cart = GroceryCart()
        val keys = listOf(bread, celery, apples).map { cart.landed(cart.started(it)!!.key)!!.key }
        assertEquals(listOf(0, 1, 2), cart.entries.map { it.slot })
        cart.removed(keys[0])
        assertEquals(listOf(1, 2), cart.entries.map { it.slot })
        // The freed slot is the next one taken.
        assertEquals(0, cart.started(bread)!!.slot)
    }

    @Test
    fun aLiftIsByKeySoTwoOfAProductNeverMixUp() {
        val cart = GroceryCart()
        val first = cart.landed(cart.started(apples)!!.key)!!
        val second = cart.landed(cart.started(apples)!!.key)!!
        assertTrue(cart.lifting(second.key))
        assertFalse(cart.lifting(second.key), "already rising")
        cart.removed(second.key)
        assertEquals(listOf(first.key), cart.entries.map { it.key })
        assertTrue(cart.lifting.isEmpty())
    }

    @Test
    fun moneyIsDollarsAndCents() {
        assertEquals("$3.49", money(349))
        assertEquals("$10.05", money(1005))
        assertEquals("$0.00", money(0))
    }
}
