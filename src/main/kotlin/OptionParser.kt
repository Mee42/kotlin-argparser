// Copyright © 2016 Laurence Gonsalves
//
// This file is part of kotlin-optionparser, a library which can be found at
// http://github.com/xenomachina/kotlin-optionparser
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by the
// Free Software Foundation; either version 2.1 of the License, or (at your
// option) any later version.
//
// This library is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
// for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this library; if not, see http://www.gnu.org/licenses/

package com.xenomachina.optionparser

import kotlin.reflect.KProperty

/**
 * A command-line option/argument parser.
 *
 * Example usage:
 *
 *     // Define class to hold parsed options
 *     class MyOptions(parser: OptionParser) {
 *         // boolean flags
 *         val verbose by parser.flagging("-v", "--verbose")
 *
 *         // simple options with arguments
 *         val name by parser.storing("-N", "--name",
 *             help="My Name")
 *         val size by parser.storing("-s", "--size"
 *             help="My Size"){toInt} = 8
 *
 *         // optional options
 *         val name by parser.storing("-O", "--output",
 *             help="Output location")
 *             .default("./")
 *
 *         // accumulating values (turns into a List)
 *         val includeDirs by parser.adding("-I",
 *             help="Directories to search for headers"
 *         ){
 *             File(this)
 *         }
 *
 *         // TODO: implement mapping()
 *         // map options to values
 *         val mode by parser.mapping(
 *                 "--fast" to Mode.FAST,
 *                 "--small" to Mode.SMALL,
 *                 "--quiet" to Mode.QUIET,
 *             default = Mode.FAST,
 *             help="Operating mode")
 *
 *         // All of these methods are based upon the "option" method, which
 *         // can do anything they can do and more (but is harder to use in the
 *         // common cases)
 *         val zaphod by parser.option("-z", "--zaphod"
 *             help="Directories to search for headers"
 *         ){
 *             return parseZaphod(name, value, argument)
 *         }
 *     }
 *
 *  Your main function can then look like this:
 *
 *     fun main(args : Array<String>) =
 *         MyOptions(args).runMain {
 *             // `this` is the MyOptions instance, and will already be parsed
 *             // and validated at this point.
 *             println("Hello, {name}!")
 *         }
 */
open class OptionParser(val progName: String, val args: Array<String>) {
    // TODO: add --help support
    // TODO: add addValidator method
    fun flagging(vararg names: String,
                 help: String? = null): Delegate<Boolean> =
            option<Boolean>(*names, help = help) { true }.default(false)

    fun <T> storing(vararg names: String,
                    help: String? = null,
                    parser: String.() -> T): Delegate<T> =
            option(*names, help = help) { parser(this.next()) }

    fun storing(vararg names: String,
                help: String? = null): Delegate<String> =
            storing(*names, help = help) { this }

    /**
     * Adds argument to a MutableCollection.
     */
    fun <E, T : MutableCollection<E>> adding(vararg names: String,
                                             help: String? = null,
                                             initialValue: T,
                                             parser: String.() -> E): Delegate<T> =
            option<T>(*names, help = help) {
                value!!.value.add(parser(next()))
                value.value
            }.default(initialValue)

    // TODO: figure out why this causes "cannot choose among the following candidates" errors everywhere.
    /**
    * Convenience for adding argument as an unmodified String to a MutableCollection.
    */
    //fun <T : MutableCollection<String>> adding(vararg names: String,
    //               help: String? = null,
    //               initialValue: T): Delegate<T> =
    //        adding(*names, help = help, initialValue = initialValue){this}

    /**
     * Convenience for adding argument to a MutableList.
     */
    fun <T> adding(vararg names: String,
                   help: String? = null,
                   parser: String.() -> T) =
            adding(*names, help = help, initialValue = mutableListOf(), parser = parser)

    /**
     * Convenience for adding argument as an unmodified String to a MutableList.
     */
    fun adding(vararg names: String,
               help: String? = null): Delegate<MutableList<String>> =
            adding(*names, help = help) { this }

    fun <T> option(vararg names: String,
                   help: String? = null,
                   handler: Delegate.Input<T>.() -> T): Delegate<T> {
        val delegate = Delegate<T>(this, help = help, handler = handler)
        // TODO: verify that there is at least one name
        for (name in names) {
            register(name, delegate)
        }
        return delegate
    }

    // TODO: add `argument` method for positional argument handling
    // TODO: verify that positional arguments have exactly one name

    class Delegate<T>(private val argParser: OptionParser, val help: String?, val handler: Input<T>.() -> T) {
        /**
         * Sets the value for this Delegate. Should be called prior to parsing.
         */
        fun default(value: T): Delegate<T> {
            // TODO: throw exception if parsing already complete?
            holder = Holder(value)
            return this
        }

        class Input<T>(val value: Holder<T>?,
                       val name: String,
                       val firstArg: String?,
                       val offset: Int,
                       val args: Array<String>) {

            internal var consumed = 0

            fun hasNext(): Boolean {
                TODO()
            }

            fun next(): String {
                val result: String
                if (firstArg == null) {
                    result = args[offset + consumed]
                } else {
                    result = if (consumed == 0) firstArg else args[offset + consumed - 1]
                }
                consumed++
                return result
            }

            fun peek(): String {
                TODO()
            }
        }

        private var holder: Holder<T>? = null

        internal fun parseOption(name: String, firstArg: String?, index: Int, args: Array<String>): Int {
            val input = Input(holder, name, firstArg, index, args)
            holder = Holder(handler(input))
            return input.consumed
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            argParser.parseOptions
            return holder!!.value
        }
    }

    private val shortOptions = mutableMapOf<Char, Delegate<*>>()
    private val longOptions = mutableMapOf<String, Delegate<*>>()

    private fun <T> register(name: String, delegate: OptionParser.Delegate<T>) {
        if (name.startsWith("--")) {
            if (name.length <= 2)
                throw IllegalArgumentException("illegal long option '$name' -- must have at least one character after hyphen")
            longOptions.put(name, delegate)
        } else if (name.startsWith("-")) {
            if (name.length != 2)
                throw IllegalArgumentException("illegal short option '$name' -- can only have one character after hyphen")
            val key = name.get(1)
            if (key in shortOptions)
                throw IllegalStateException("short option '$name' already in use")
            shortOptions.put(key, delegate)
        } else {
            TODO("registration of positional args")
        }

    }

    private val parseOptions by lazy {
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            i += when {
                arg.startsWith("--") ->
                    parseLongOpt(i, args)
                arg.startsWith("-") ->
                    parseShortOpts(i, args)
                else ->
                    parsePositionalArg(i, args)
            }
        }
        // TODO: throw exception if any holders are null
    }

    private fun parsePositionalArg(index: Int, args: Array<String>): Int {
        TODO("${args.slice(index..args.size)}")
    }

    /**
     * @param index index into args, starting at a long option, eg: "--verbose"
     * @param args array of command-line arguments
     * @return number of arguments that have been processed
     */
    private fun parseLongOpt(index: Int, args: Array<String>): Int {
        val name: String
        val firstArg: String?
        val m = NAME_EQUALS_VALUE_REGEX.matchEntire(args[index])
        if (m == null) {
            name = args[index]
            firstArg = null
        } else {
            name = m.groups[1]!!.value
            firstArg = m.groups[2]!!.value
        }
        val delegate = longOptions.get(name)
        if (delegate == null) {
            throw InvalidOptionException(progName, name)
        } else {
            var consumedArgs = delegate.parseOption(name, firstArg, index + 1, args)
            if (firstArg != null) {
                if (consumedArgs < 1) TODO("throw exception -- =argument not consumed")
                consumedArgs -= 1
            }
            return 1 + consumedArgs
        }
    }

    /**
     * @param index index into args, starting at a set of short options, eg: "-abXv"
     * @param args array of command-line arguments
     * @return number of arguments that have been processed
     */
    private fun parseShortOpts(index: Int, args: Array<String>): Int {
        val opts = args[index]
        var optIndex = 1
        while (optIndex < opts.length) {
            val optKey = opts[optIndex]
            val optName = "-$optKey"
            optIndex++ // optIndex now points just after optKey

            val delegate = shortOptions.get(optKey)
            if (delegate == null) {
                throw InvalidOptionException(progName, optName)
            } else {
                // TODO: move substring construction into Input.next()?
                val firstArg = if (optIndex >= opts.length) null else opts.substring(optIndex)
                val consumed = delegate.parseOption(optName, firstArg, index + 1, args)
                if (consumed > 0) {
                    return consumed + (if (firstArg == null) 1 else 0)
                }
            }
        }
        return 1
    }

    companion object {
        private val NAME_EQUALS_VALUE_REGEX = Regex("^([^=]+)=(.*)$")
    }
}

/**
 * Compensates for the fact that nullable types don't compose in Kotlin. If you want to be able to distinguish between a
 * T (where T may or may not be a nullable type) and lack of a T, use a Holder<T>?.
 */
data class Holder<T> (val value: T)

fun <T> Holder<T>?.orElse(f: () -> T) : T{
    if (this == null) {
        return f()
    } else {
        return value
    }
}

fun <T, R> T.runMain(f: T.() -> R): R {
    try {
        return f()
    } catch (e: UserErrorException) {
        e.printAndExit()
    }
}
