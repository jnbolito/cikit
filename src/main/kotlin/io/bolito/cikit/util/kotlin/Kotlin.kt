package io.bolito.cikit.util.kotlin

inline fun <T> T.returnThis(body: () -> Unit): T {
    body()
    return this
}