package com.jamal2367.styx.search.engine

import com.jamal2367.styx.R

/**
 * The Brave search engine.
 */
class BraveSearch : BaseSearchEngine(
    "file:///android_asset/brave.webp",
    "https://search.brave.com/search?q=",
    R.string.search_engine_brave
)
