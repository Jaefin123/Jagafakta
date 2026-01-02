package com.jagafakta.jagafakta.util


class Event<out T>(private val content: T) {
    private var handled = false

    fun getIfNotHandled(): T? {
        return if (handled) null
        else {
            handled = true
            content
        }
    }


    fun peek(): T = content
}
