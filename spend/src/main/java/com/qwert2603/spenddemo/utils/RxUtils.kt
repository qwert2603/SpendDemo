package com.qwert2603.spenddemo.utils

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.lang.NumberFormatException

fun <T : Any, R> Observable<T>.castAndFilter(toClass: Class<R>): Observable<R> = this
        .filter { it.javaClass.isAssignableFrom(toClass) }
        .cast(toClass)

fun Observable<String>.mapToInt(): Observable<Int> = this.map {
    try {
        it.toInt()
    } catch (e: NumberFormatException) {
        0
    }
}

fun <T, U> makePair() = BiFunction { t: T, u: U -> Pair(t, u) }
fun <T, U> firstOfTwo() = BiFunction { t: T, _: U -> t }
fun <T, U> secondOfTwo() = BiFunction { _: T, u: U -> u }