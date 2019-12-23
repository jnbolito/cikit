package io.bolito.cikit.internal.util.kotlin

inline fun <T> T.returnThis(body: () -> Unit): T {
    body()
    return this
}