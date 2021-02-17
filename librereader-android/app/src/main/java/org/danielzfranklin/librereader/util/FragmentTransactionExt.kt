package org.danielzfranklin.librereader.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

inline fun<reified T: Fragment> FragmentManager.find(tag: String): T? {
    val result = findFragmentByTag(tag)
    if (result !is T) return null
    return result
}