package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.BookPageSetting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookDimensions
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.layoutShelf
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_TEXTURE_SCALE = 4.2f

internal class ShelfAssets(
    val layout: ShelfLayout,
    val maps: ShelfLightMaps,
    val spines: Map<Int, Bitmap>,
    val size: IntSize,
)

@Composable
internal fun rememberShelfAssets(
    content: ShelfContent,
    size: IntSize,
    density: Float,
    lighting: ShelfLighting,
    kit: ShelfKit,
    synchronous: Boolean,
): ShelfAssets? {
    var assets by remember { mutableStateOf<ShelfAssets?>(null) }
    if (size == IntSize.Zero) return null
    if (synchronous) {
        return remember(content, size, lighting) {
            bakeShelfAssets(content, size, density, lighting, kit)
        }
    }
    LaunchedEffect(content, size, lighting) {
        assets =
            withContext(ShelfTextBakers) {
                bakeShelfAssets(content, size, density, lighting, kit)
            }
    }
    return assets?.takeIf { it.size == size }
}

private fun bakeShelfAssets(
    content: ShelfContent,
    size: IntSize,
    density: Float,
    lighting: ShelfLighting,
    kit: ShelfKit,
): ShelfAssets {
    val layout =
        layoutShelf(
            size.width / density,
            size.height / density,
            content.planks.map { plank -> plank.map { it.spec } },
        )
    val k = layout.scale
    val spines =
        layout.planks
            .flatMap { it.slots }
            .mapNotNull { slot ->
                content.entries[slot.bookId]?.let { entry ->
                    slot.bookId to
                        createSpineBitmap(
                            entry.visual,
                            slot.thickness / k,
                            slot.height / k,
                            k * density,
                            kit,
                        )
                }
            }
            .toMap()
    return ShelfAssets(layout, bakeShelfLight(layout, lighting), spines, size)
}

@Composable
internal fun rememberShadowLift(
    assets: ShelfAssets?,
    lighting: ShelfLighting,
    bookId: Int?,
    synchronous: Boolean,
): ShadowLift? {
    val cache = remember(assets) { mutableStateMapOf<Int, ShadowLift>() }
    if (assets == null || bookId == null) return null
    if (synchronous) {
        return remember(assets, bookId) {
            bakeShadowLift(assets.layout, lighting, assets.maps, bookId)
        }
    }
    LaunchedEffect(assets, bookId) {
        if (bookId in cache) return@LaunchedEffect
        cache[bookId] =
            withContext(Dispatchers.Default) {
                bakeShadowLift(assets.layout, lighting, assets.maps, bookId)
            }
    }
    return cache[bookId]
}

@Composable
internal fun rememberBookShell(
    assets: ShelfAssets?,
    content: ShelfContent,
    bookId: Int?,
    coverFor: (Int) -> Bitmap?,
    density: Float,
    kit: ShelfKit,
    synchronous: Boolean,
): BookShell? {
    val cache = remember(assets) { mutableStateMapOf<Int, BookShell>() }
    val request = textureRequest(assets, content, bookId, density) ?: return null
    val id = request.entry.visual.id
    val cover = coverFor(id)
    val cached = cache[id]?.takeIf { it.isBakedWith(cover) }
    if (synchronous) {
        return cached ?: remember(request, cover) { request.shell(cover, kit) }
    }
    LaunchedEffect(request, cover) {
        if (cache[id].isBakedWith(cover)) return@LaunchedEffect
        val shell = withContext(ShelfTextBakers) { request.shell(cover, kit) }
        val keep = setOf(id, content.readingBookId)
        cache.keys.filter { it !in keep }.dropLast(1).forEach(cache::remove)
        cache[id] = shell
    }
    return cached
}

@Composable
internal fun rememberBookPages(
    assets: ShelfAssets?,
    content: ShelfContent,
    bookId: Int?,
    pagesFor: (Int, BookPageSetting?) -> OpenBookPages?,
    metricsDp: PageMetrics,
    typeset: PageTypeset?,
    bookSetting: BookPageSetting?,
    density: Float,
    kit: ShelfKit,
    synchronous: Boolean,
): BookPages? {
    var pages by remember { mutableStateOf<BookPages?>(null) }
    val request = textureRequest(assets, content, bookId, density) ?: return null
    val wanted = bookId?.let { pagesFor(it, bookSetting) } ?: return null
    val metrics = request.toUnits(metricsDp)
    if (synchronous) {
        return remember(request, wanted, metrics, typeset, bookSetting) {
            request.pages(wanted, metrics, typeset, bookSetting, kit)
        }
    }
    LaunchedEffect(request, wanted, metrics, typeset, bookSetting) {
        pages =
            withContext(ShelfTextBakers) {
                request.pages(wanted, metrics, typeset, bookSetting, kit)
            }
    }
    return pages?.takeIf { it.bookId == bookId }
}

@Composable
internal fun rememberRibbonBand(
    assets: ShelfAssets?,
    content: ShelfContent,
    bookId: Int?,
    density: Float,
): Bitmap? {
    var band by remember { mutableStateOf<Pair<TextureRequest, Bitmap>?>(null) }
    val request = textureRequest(assets, content, bookId, density) ?: return null
    LaunchedEffect(request) {
        if (band?.first == request) return@LaunchedEffect
        band = request to withContext(Dispatchers.Default) { request.ribbonBand() }
    }
    return band?.takeIf { it.first == request }?.second
}

internal data class LeafKey(val page: Int, val right: Boolean)

internal class LeafFace(val content: PageContent, val bitmap: Bitmap, val exact: Boolean = true)

private data class FaceKey(val content: PageContent, val right: Boolean)

private class BakedFaces(val request: TextureRequest?, val metrics: PageMetrics?) {
    val bitmaps = mutableStateMapOf<FaceKey, Bitmap>()
    val lastShown = HashMap<LeafKey, LeafFace>()
}

@Composable
internal fun rememberLeafFaces(
    assets: ShelfAssets?,
    content: ShelfContent,
    bookId: Int?,
    wanted: List<LeafKey>,
    leafFor: (Int, BookPageSetting?, LeafKey) -> PageContent?,
    metricsDp: PageMetrics,
    bookSetting: BookPageSetting?,
    density: Float,
    kit: ShelfKit,
): Map<LeafKey, LeafFace> {
    val request = textureRequest(assets, content, bookId, density)
    val metrics = request?.toUnits(metricsDp)
    val baked = remember(request, metrics, bookSetting) { BakedFaces(request, metrics) }
    val contents =
        if (bookId == null) emptyList()
        else wanted.mapNotNull { key -> leafFor(bookId, bookSetting, key)?.let { key to it } }
    LaunchedEffect(baked, contents) {
        val bakeRequest = baked.request ?: return@LaunchedEffect
        val bakeMetrics = baked.metrics ?: return@LaunchedEffect
        val keep = contents.mapTo(HashSet()) { (key, page) -> FaceKey(page, key.right) }
        baked.bitmaps.keys.filter { it !in keep }.forEach(baked.bitmaps::remove)
        contents
            .map { (key, page) -> FaceKey(page, key.right) }
            .distinct()
            .filter { it !in baked.bitmaps }
            .forEach { face ->
                launch {
                    baked.bitmaps[face] =
                        withContext(ShelfTextBakers) {
                            bakeRequest.face(
                                face.content,
                                face.right,
                                bakeMetrics,
                                bookSetting,
                                kit,
                            )
                        }
                }
            }
    }
    val faces =
        contents
            .mapNotNull { (key, page) ->
                val bitmap = baked.bitmaps[FaceKey(page, key.right)]
                val face =
                    if (bitmap != null) LeafFace(page, bitmap)
                    else
                        baked.lastShown[key]?.let { LeafFace(it.content, it.bitmap, exact = false) }
                face?.let { key to it }
            }
            .toMap()
    SideEffect {
        baked.lastShown.clear()
        faces.forEach { (key, face) -> if (face.exact) baked.lastShown[key] = face }
    }
    return faces
}

private data class TextureRequest(
    val entry: ShelfEntry,
    val dims: BookDimensions,
    val pxPerUnit: Float,
    val pagePxPerUnit: Float,
    val dpPerUnit: Float,
    val spine: Bitmap,
) {
    fun shell(cover: Bitmap?, kit: ShelfKit) =
        createBookShell(entry.visual, dims, pxPerUnit, spine, cover, kit)

    fun pages(
        pages: OpenBookPages,
        metrics: PageMetrics,
        typeset: PageTypeset?,
        bookSetting: BookPageSetting?,
        kit: ShelfKit,
    ) =
        createBookPages(
            entry.visual,
            dims,
            pagePxPerUnit,
            pages,
            metrics,
            typeset,
            bookSetting,
            kit,
        )

    fun face(
        page: PageContent,
        rightSide: Boolean,
        metrics: PageMetrics,
        bookSetting: BookPageSetting?,
        kit: ShelfKit,
    ) =
        createPageFace(
            entry.visual,
            dims,
            pagePxPerUnit,
            page,
            rightSide,
            metrics,
            bookSetting,
            kit,
        )

    fun ribbonBand() = createRibbonBand(dims, pagePxPerUnit)

    fun toUnits(metrics: PageMetrics) =
        metrics.copy(
            bodySize = metrics.bodySize / dpPerUnit,
            lineHeight = metrics.lineHeight / dpPerUnit,
            captionSize = metrics.captionSize / dpPerUnit,
        )
}

private fun textureRequest(
    assets: ShelfAssets?,
    content: ShelfContent,
    bookId: Int?,
    density: Float,
): TextureRequest? {
    if (assets == null || bookId == null) return null
    val entry = content.entries[bookId] ?: return null
    val geometry = heldGeometry(assets.layout, bookId) ?: return null
    val spine = assets.spines[bookId] ?: return null
    val k = assets.layout.scale
    val pxPerUnit = minOf(MAX_TEXTURE_SCALE, geometry.held.scale * density * 1.05f) * k
    val dims =
        BookDimensions(
            geometry.slot.thickness / k,
            geometry.slot.height / k,
            geometry.slot.depth / k,
        )
    return TextureRequest(
        entry,
        dims,
        pxPerUnit,
        geometry.held.scale * density * k,
        k * geometry.held.scale,
        spine,
    )
}
