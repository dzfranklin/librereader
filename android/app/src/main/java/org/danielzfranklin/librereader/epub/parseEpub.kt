package org.danielzfranklin.librereader.epub

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

fun parseEpub(input: InputStream, imageSaver: (ImageID) -> OutputStream) {
    ZipInputStream(input).use { zip ->
        val resources = mutableListOf<Resource>()

        var entry = zip.nextEntry
        while (entry != null) {
            when {
                entry.isDirectory -> Unit
                Image.matches(entry) -> resources.add(Image(entry, zip, imageSaver))
                Stylesheet.matches(entry) -> resources.add(Stylesheet(entry, zip))
                Container.matches(entry) -> resources.add(Container(entry, zip))
                else -> resources.add(UnknownInFirstPass(entry, zip))
            }

            entry = zip.nextEntry
        }

        val oebpsPackage = OpfPackage.find(resources)
            ?: throw ParseEpubError("Container missing OEBPS package and fallback not present")
    }
}

class ParseEpubError(message: String) : Error(message)

data class ImageID(val value: String) {
    companion object {
        fun create() = ImageID(UUID.randomUUID().toString())
    }
}

private interface Resource {
    val href: String
}

private class OpfPackage(
    res: UnknownInFirstPass,
    resources: List<Resource>,
    imageSaver: (ImageID) -> OutputStream
) : Resource {
    // NOTE: See <http://idpf.org/epub/20/spec/OPF_2.0_final_spec.html> and
    // <https://www.w3.org/publishing/epub3/epub-packages.html>

    override val href: String = res.href

    /** Combine identifier with modified to uniquely identify */
    data class Meta(
        val identifier: String,
        val modified: String,
        val title: String,
        // don't require language as we don't have much use for, even though spec mandates
        val language: String?,
        val contributor: String?,
        val coverage: String?,
        val creator: String?,
        val date: String?,
        val description: String?,
        val format: String?,
        val publisher: String?,
        val relation: String?,
        val rights: String?,
        val source: String?,
        val subject: String?,
        val type: String?,
    )

    val meta: Meta

    data class SpecialManifestItems(
        val cover: Image?,
        val nav: Nav,
    )

    /** legacy table of contents format */
    val ncx: NCX?

    val spine: List<ContentDoc>

    val specialManifestItems: SpecialManifestItems

    init {
        val text = res.bytes.toString(UTF_8)
        val tree = Jsoup.parse(text, "", Parser.xmlParser())

        val metaE = tree.selectFirstRequired("metadata")
        meta = Meta(
            identifier = metaE.selectFirstRequiredText("identifier"),
            modified = metaE.selectFirstRequiredText("meta[property=\"dcterms:modified\"]"),
            title = metaE.selectFirstRequiredText("title"),
            language = metaE.selectFirstOptionalText("dc:language"),
            contributor = metaE.selectFirstOptionalText("contributor"),
            coverage = metaE.selectFirstOptionalText("coverage"),
            creator = metaE.selectFirstOptionalText("creator"),
            date = metaE.selectFirstOptionalText("date"),
            description = metaE.selectFirstOptionalText("description"),
            format = metaE.selectFirstOptionalText("format"),
            publisher = metaE.selectFirstOptionalText("publisher"),
            relation = metaE.selectFirstOptionalText("relation"),
            rights = metaE.selectFirstOptionalText("rights"),
            source = metaE.selectFirstOptionalText("source"),
            subject = metaE.selectFirstOptionalText("subject"),
            type = metaE.selectFirstOptionalText("type"),
        )

        val manifestE = tree.selectFirstRequired("manifest")

        val navHref = manifestE.selectFirstRequired("item[properties=\"nav\"]").attrRequired("href")
        val nav = (resources.find { it.href == navHref } as? UnknownInFirstPass)
            ?.let { Nav(it) }
            ?: throw ParseEpubError("Nav href $href does not exist")

        val cover = manifestE.selectFirstOptional("item[properties=\"nav\"]")
            ?.attrOptional("href")?.let { coverHref ->
                val cover = resources.find { it.href == coverHref } as? UnknownInFirstPass
                if (cover != null) {
                    Image(cover, imageSaver)
                } else {
                    Timber.w("Cover specified in manifest, but href does not exist.")
                    null
                }
            }

        specialManifestItems = SpecialManifestItems(cover, nav)

        val manifestItemsById = mutableMapOf<String, Element>()
        for (item in manifestE.select("item")) {
            manifestItemsById[item.attrRequired("id")] = item
        }

        val spine = tree.selectFirstRequired("spine")

        val tocRef = spine.attrOptional("toc")
        TODO("parse toc")

        for (itemref in spine.select("itemref")) {
            val idref = itemref.attrRequired("idref")
            val linear = itemref.attrOptional("linear", "yes") == "yes"


        }
    }

    companion object {
        const val MEDIA_TYPE = "application/oebps-package+xml"
        const val FALLBACK_SEARCH_PATH = "OEBPS/content.opf"

        fun find(resources: List<Resource>): OpfPackage? {
            val container = resources.find { it is Container } as? Container
            var result: OpfPackage? = null
            if (container != null) {
                val href = container.oebpsPackageHref
                val res = resources.find { it.href == href } as? UnknownInFirstPass
                if (res != null) {
                    result = OpfPackage(res, resources)
                } else {
                    Timber.w("OEBPS package specified in container not found. Trying fallback.")
                }
            }
            if (result == null) {
                val res = resources.find { it.href == FALLBACK_SEARCH_PATH } as? UnknownInFirstPass
                if (res != null) {
                    result = OpfPackage(res, resources)
                }
            }
            return result
        }

        private fun resolveSpineContentDoc(id: String, manifestItems: Map<String, Element>, resources: List<Resource>): ContentDoc? {
            val item = manifestItems[id]
                ?: throw ParseEpubError("Item referenced in spine does not exist")

            var href = item.attrRequired("href")
            var type = item.attrRequired("media-type")
            return if (type == ContentDoc.MEDIA_TYPE) {
                val res = resources.find { it.href == href } as? UnknownInFirstPass
                ContentDoc(res)
            } else {
                val fallback = item.attrOptional("fallback")
                if (fallback != null) {
                    resolveSpineContentDoc(fallback, manifestItems)
                } else {
                    null
                }
            }
        }
    }
}

private class ContentDoc {
    init {
        TODO()
    }

    companion object {
        // See <https://idpf.github.io/epub-cmt/v3/#sec-cmt-supported>
        const val MEDIA_TYPE = "application/xhtml+xml"
    }
}

private class Nav(res: UnknownInFirstPass) : Resource {
    override val href = res.href

    init {
        val text = res.bytes.toString(UTF_8)
        val tree = Jsoup.parse(text, "", Parser.xmlParser())
        TODO()
    }
}

private class UnknownInFirstPass(entry: ZipEntry, stream: ZipInputStream) : Resource {
    override val href: String = entry.name

    val bytes = ByteArrayOutputStream()

    init {
        stream.writeTo(bytes)
    }
}

private class Container(entry: ZipEntry, stream: ZipInputStream) : Resource {
    override val href: String = entry.name

    val oebpsPackageHref: String?

    init {
        val bytes = ByteArrayOutputStream()
        stream.writeTo(bytes)
        val text = bytes.toString(UTF_8)

        val tree = Jsoup.parse(text, "", Parser.xmlParser())
        val oebpsPackages =
            tree.select("rootFiles > rootfile[media-type=\"${OpfPackage.MEDIA_TYPE}\"]")
        if (oebpsPackages.size > 1) {
            Timber.w("More than one OEBPS package in EPUB. Selecting first.")
        }
        val href = oebpsPackages.first()?.attr("full-path")
        if (href == "") {
            oebpsPackageHref = null
            Timber.w("OEBPS package full-path attr missing or empty. Trying fallback.")
        } else if (href == null) {
            Timber.w("Container missing OEBPS package. Trying fallback.")
            oebpsPackageHref = null
        } else {
            oebpsPackageHref = href
        }
    }

    companion object {
        fun matches(entry: ZipEntry) = entry.name == "META-INF/container.xml"
    }
}

private class Stylesheet(entry: ZipEntry, stream: ZipInputStream) : Resource {
    override val href: String = entry.name

    val text: String

    init {
        val out = ByteArrayOutputStream()
        stream.writeTo(out)
        text = out.toString(UTF_8)
    }

    companion object {
        fun matches(entry: ZipEntry) = matchesExt(entry.name, ".css")
    }
}

private class Image(
    override val href: String,
    stream: InputStream,
    saver: (ImageID) -> OutputStream
) : Resource {
    constructor(
        entry: ZipEntry,
        stream: ZipInputStream,
        saver: (ImageID) -> OutputStream
    ) : this(entry.name, stream, saver)

    constructor(res: UnknownInFirstPass, saver: (ImageID) -> OutputStream) : this(
        res.href,
        ByteArrayInputStream(res.bytes.toByteArray()),
        saver
    )

    val id = ImageID.create()

    init {
        stream.writeTo(saver(id))
    }

    companion object {
        fun matches(entry: ZipEntry) = matchesExt(entry.name, EXTS)

        private val EXTS =
            listOf(".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp", ".bmp", ".heif")
    }
}

fun Element.selectFirstOptional(sel: String): Element? = selectFirst(sel)

fun Element.selectFirstRequired(sel: String): Element =
    selectFirst(sel) ?: throw ParseEpubError("Missing required element matching: $sel")

fun Element.selectFirstOptionalText(sel: String): String? =
    selectFirst(sel)?.text()

fun Element.selectFirstRequiredText(sel: String): String =
    selectFirstOptionalText(sel) ?: throw ParseEpubError("Missing required element matching: $sel")

fun Element.attrOptional(name: String, default: String? = null): String? =
    attr(name) ?: default

fun Element.attrRequired(name: String): String = attrOptional(name)
    ?: throw ParseEpubError("Missing required attr: $name on element $this ${this.cssSelector()}")

fun matchesExt(filename: String, ext: String): Boolean {
    val normalized = filename.toLowerCase(Locale.ROOT)
    return normalized.endsWith(ext)
}

fun matchesExt(filename: String, exts: List<String>): Boolean {
    val normalized = filename.toLowerCase(Locale.ROOT)
    return exts.any { normalized.endsWith(it) }
}

private fun InputStream.writeTo(output: OutputStream) {
    val buf = ByteArray(COPY_BUFFER_SIZE)
    while (true) {
        val readSize = this.read(buf)
        if (readSize < 0) {
            break
        }
        output.write(buf, 0, readSize)

    }
    output.flush()
}

private const val COPY_BUFFER_SIZE = 1024 * 10

private const val UTF_8 = "UTF-8"