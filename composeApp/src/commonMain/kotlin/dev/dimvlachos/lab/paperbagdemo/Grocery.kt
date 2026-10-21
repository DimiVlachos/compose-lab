package dev.dimvlachos.lab.paperbagdemo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.core.presentation.components.paperbag.BagArt
import dev.dimvlachos.lab.core.presentation.components.paperbag.BagCapacity
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.paperbag_apples
import dev.dimvlachos.lab.resources.paperbag_apples_unit
import dev.dimvlachos.lab.resources.paperbag_baguette
import dev.dimvlachos.lab.resources.paperbag_baguette_unit
import dev.dimvlachos.lab.resources.paperbag_celery
import dev.dimvlachos.lab.resources.paperbag_celery_unit
import dev.dimvlachos.lab.resources.paperbag_eggs
import dev.dimvlachos.lab.resources.paperbag_eggs_unit
import dev.dimvlachos.lab.resources.paperbag_milk
import dev.dimvlachos.lab.resources.paperbag_milk_unit
import dev.dimvlachos.lab.resources.paperbag_sunflowers
import dev.dimvlachos.lab.resources.paperbag_sunflowers_unit
import org.jetbrains.compose.resources.StringResource

/** A product on Fernwood Market's shelf: its [name], its [unit] line, its price and its art. */
internal class Product(
    val id: String,
    val name: StringResource,
    val unit: StringResource,
    val priceCents: Int,
    val art: BagArt,
)

/** The aisle, in the prototype's order. */
internal object Groceries {
    val all =
        listOf(
            Product(
                "bread",
                Res.string.paperbag_baguette,
                Res.string.paperbag_baguette_unit,
                349,
                BagArt.Baguette,
            ),
            Product(
                "celery",
                Res.string.paperbag_celery,
                Res.string.paperbag_celery_unit,
                229,
                BagArt.Celery,
            ),
            Product(
                "apples",
                Res.string.paperbag_apples,
                Res.string.paperbag_apples_unit,
                599,
                BagArt.Apples,
            ),
            Product(
                "flowers",
                Res.string.paperbag_sunflowers,
                Res.string.paperbag_sunflowers_unit,
                899,
                BagArt.Sunflowers,
            ),
            Product(
                "milk",
                Res.string.paperbag_milk,
                Res.string.paperbag_milk_unit,
                319,
                BagArt.Milk,
            ),
            Product(
                "eggs",
                Res.string.paperbag_eggs,
                Res.string.paperbag_eggs_unit,
                449,
                BagArt.Eggs,
            ),
        )

    fun byId(id: String): Product = all.first { it.id == id }
}

/** "$3.49". */
internal fun money(cents: Int): String =
    "$" + (cents / 100) + "." + (cents % 100).toString().padStart(2, '0')

/**
 * One thing in the bag, or on its way in: its own [key], so lifting it out or a later removal never
 * mixes it up with another of the same product, and the [slot] across the bag's mouth it stands in,
 * kept for as long as it is in the bag so nothing else moves when it leaves.
 */
internal class BagEntry(val key: Int, val product: Product, val slot: Int)

/**
 * The cart behind the bag. An add is [started] while the product flies to the bag, so it holds its
 * place and counts on its row at once, and [landed] when it drops in; a removal is [lifting] while
 * it rises out, and gone once [removed]. The bag holds [BagCapacity] at most, counting what is in
 * the air.
 */
@Stable
internal class GroceryCart {
    /** What is in the bag, in the order it landed. */
    val entries = mutableStateListOf<BagEntry>()

    /** What is flying to the bag. */
    val incoming = mutableStateListOf<BagEntry>()

    /** The keys of entries rising out of the bag. */
    val lifting = mutableStateListOf<Int>()

    private var nextKey by mutableIntStateOf(0)

    val count: Int
        get() = entries.size

    val full: Boolean
        get() = entries.size + incoming.size >= BagCapacity

    val totalCents: Int
        get() = entries.sumOf { it.product.priceCents }

    /** How many of [product] are in the bag or on their way, as its row says. */
    fun quantity(product: Product): Int =
        entries.count { it.product.id == product.id } +
            incoming.count { it.product.id == product.id }

    /** Starts [product] on its way into the bag, or null when the bag is full. */
    fun started(product: Product): BagEntry? {
        if (full) return null
        val taken = (entries + incoming).map { it.slot }.toSet()
        val slot = (0 until BagCapacity).first { it !in taken }
        return BagEntry(nextKey++, product, slot).also { incoming += it }
    }

    /** Drops the entry [key] into the bag. */
    fun landed(key: Int): BagEntry? {
        val entry = incoming.firstOrNull { it.key == key } ?: return null
        incoming.remove(entry)
        entries += entry
        return entry
    }

    /** Starts the entry [key] rising out of the bag, unless it already is. */
    fun lifting(key: Int): Boolean {
        if (key in lifting || entries.none { it.key == key }) return false
        lifting += key
        return true
    }

    /** Takes the entry [key] out of the bag. */
    fun removed(key: Int): BagEntry? {
        lifting.remove(key)
        val entry = entries.firstOrNull { it.key == key } ?: return null
        entries.remove(entry)
        return entry
    }

    /** Empties the cart at once. */
    fun clear() {
        entries.clear()
        incoming.clear()
        lifting.clear()
    }
}
