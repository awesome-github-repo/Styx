@file:JvmName("UrlUtils")

package com.jamal2367.styx.utils

import android.net.Uri
import com.jamal2367.styx.BrowserApp
import com.jamal2367.styx.constant.FILE
import com.jamal2367.styx.constant.Uris
import com.jamal2367.styx.html.bookmark.BookmarkPageFactory
import com.jamal2367.styx.html.download.DownloadPageFactory
import com.jamal2367.styx.html.history.HistoryPageFactory
import com.jamal2367.styx.html.homepage.HomePageFactory
import android.util.Patterns
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Attempts to determine whether user input is a URL or search terms.  Anything with a space is
 * passed to search if [canBeSearch] is true.
 *
 * Converts to lowercase any mistakenly upper-cased scheme (i.e., "Http://" converts to
 * "http://")
 *
 * @param canBeSearch if true, will return a search url if it isn't a valid  URL. If false,
 * invalid URLs will return null.
 * @return original or modified URL.
 */
fun smartUrlFilter(url: String, canBeSearch: Boolean, searchUrl: String): Pair<String,Boolean> {
    var inUrl = url.trim()
    val hasSpace = inUrl.contains(' ')
    val matcher = ACCEPTED_URI_SCHEMA.matcher(inUrl)
    if (matcher.matches()) {
        // force scheme to lowercase
        val scheme = requireNotNull(matcher.group(1)) { "matches() implies this is non null" }
        val lcScheme = scheme.toLowerCase(Locale.getDefault())
        if (lcScheme != scheme) {
            inUrl = lcScheme + matcher.group(2)
        }
        if (hasSpace && Patterns.WEB_URL.matcher(inUrl).matches()) {
            inUrl = inUrl.replace(" ", URL_ENCODED_SPACE)
        }
        return Pair(inUrl,false)
    }
    if (!hasSpace) {
        if (Patterns.WEB_URL.matcher(inUrl).matches()) {
            return Pair(URLUtil.guessUrl(inUrl),false)
        }
    }

    return if (canBeSearch) {
        Pair(URLUtil.composeSearchUrl(inUrl, searchUrl, QUERY_PLACE_HOLDER),true)
    } else {
        Pair("",false)
    }
}

/**
 * Determines if the url is a url for the bookmark page.
 *
 * @return true if the url is a bookmark url, false otherwise.
 */
fun String?.isBookmarkUri(): Boolean =
        this == Uris.StyxBookmarks || this == Uris.AboutBookmarks

/**
 * Determines if the url is a url for the bookmark page.
 *
 * @return true if the url is a bookmark url, false otherwise.
 */
fun String?.isHomeUri(): Boolean =
        this == Uris.StyxHome || this == Uris.AboutHome

/**
 * Determines if the url is a url for the bookmark page.
 *
 * @return true if the url is a bookmark url, false otherwise.
 */
fun String?.isHistoryUri(): Boolean =
        this == Uris.StyxHistory || this == Uris.AboutHistory

/**
 * Returns whether the given url is the bookmarks/history page or a normal website
 */
fun String?.isSpecialUrl(): Boolean =
    this != null
        && this.startsWith(FILE + BrowserApp.instance.filesDir)
        && (this.endsWith(BookmarkPageFactory.FILENAME)
        || this.endsWith(DownloadPageFactory.FILENAME)
        || this.endsWith(HistoryPageFactory.FILENAME)
        || this.endsWith(HomePageFactory.FILENAME))

/**
 * Determines if the url is a url for the bookmark page.
 *
 * @return true if the url is a bookmark url, false otherwise.
 */
fun String?.isBookmarkUrl(): Boolean =
    this != null && this.startsWith(FILE) && this.endsWith(BookmarkPageFactory.FILENAME)

/**
 * Determines if the url is a url for the bookmark page.
 *
 * @return true if the url is a bookmark url, false otherwise.
 */
fun String?.isDownloadsUrl(): Boolean =
    this != null && this.startsWith(FILE) && this.endsWith(DownloadPageFactory.FILENAME)

/**
 * Determines if the url is a url for the history page.
 *
 * @return true if the url is a history url, false otherwise.
 */
fun String?.isHistoryUrl(): Boolean =
    this != null && this.startsWith(FILE) && this.endsWith(HistoryPageFactory.FILENAME)

/**
 * Determines if the url is a url for the start page.
 *
 * @return true if the url is a start page url, false otherwise.
 */
fun String?.isStartPageUrl(): Boolean =
    this != null && this.startsWith(FILE) && this.endsWith(HomePageFactory.FILENAME)

private val ACCEPTED_URI_SCHEMA = Pattern.compile("(?i)((?:http|https|file)://|(?:inline|data|about|javascript|styx):|(?:.*:.*@))(.*)")
const val QUERY_PLACE_HOLDER = "%s"
private const val URL_ENCODED_SPACE = "%20"

fun guessFileName(
        url: String?,
        contentDisposition: String?,
        mimeType: String?): String {
    var filename: String? = null
    var extension: String? = null

    // If we couldn't do anything with the hint, move toward the content disposition
    if (filename == null && contentDisposition != null) {
        filename = parseContentDisposition(contentDisposition)
        if (filename != null) {
            val index = filename.lastIndexOf('/') + 1
            if (index > 0) {
                filename = filename.substring(index)
            }
        }
    }

    // If all the other http-related approaches failed, use the plain uri
    if (filename == null) {
        var decodedUrl = Uri.decode(url)
        if (decodedUrl != null) {
            val queryIndex = decodedUrl.indexOf('?')
            // If there is a query string strip it, same as desktop browsers
            if (queryIndex > 0) {
                decodedUrl = decodedUrl.substring(0, queryIndex)
            }
            if (!decodedUrl.endsWith("/")) {
                val index = decodedUrl.lastIndexOf('/') + 1
                if (index > 0) {
                    filename = decodedUrl.substring(index)
                }
            }
        }
    }

    // Finally, if couldn't get filename from URI, get a generic filename
    if (filename == null) {
        filename = "downloadfile"
    }

    // Split filename between base and extension
    // Add an extension if filename does not have one
    val dotIndex = filename.lastIndexOf('.')
    if (dotIndex < 0) {
        if (mimeType != null) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (extension != null) {
                extension = ".$extension"
            }
        }
        if (extension == null) {
            extension = if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("text/")) {
                if (mimeType.equals("text/html", ignoreCase = true)) {
                    ".html"
                } else {
                    ".txt"
                }
            } else {
                ".bin"
            }
        }
    } else {
        if (mimeType != null) {
            // Compare the last segment of the extension against the mime type.
            // If there's a mismatch, discard the entire extension.
            val lastDotIndex = filename.lastIndexOf('.')
            val typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    filename.substring(lastDotIndex + 1))
            if (typeFromExt != null && !typeFromExt.equals(mimeType, ignoreCase = true) && !"application/octet-stream".equals(mimeType, ignoreCase = true)) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                if (extension != null) {
                    extension = ".$extension"
                }
            }
        }
        if (extension == null) {
            extension = filename.substring(dotIndex)
        }
        filename = filename.substring(0, dotIndex)
    }
    return filename + extension
}

/** Regex used to parse content-disposition headers  */
private val CONTENT_DISPOSITION_PATTERN = Pattern.compile("attachment;\\s*filename\\s*=\\s*\"([^\"]*)\"")

fun parseContentDisposition(contentDisposition: String?): String? {
    try {
        val m: Matcher = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition)
        if (m.find()) {
            return m.group(1)
        }
    } catch (ex: IllegalStateException) {
        // This function is defined as returning null when it can't parse the header
    }
    return null
}
