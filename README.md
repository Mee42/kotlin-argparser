# This is a fork that has support for --version. Storing it locally(ie, under my account) so I don't have to worry about changes


# <img alt="Kotlin --argparser" src="https://rawgit.com/xenomachina/kotlin-argparser/master/logo.svg" style="transform:scale(.6)" >

<!--
Removed until either Bintray makes a badge with a configurable label, or
shields.io can make a reliable bintray badge.

[![Bintray](https://img.shields.io/bintray/v/xenomachina/maven/kotlin-argparser.svg)](https://bintray.com/xenomachina/maven/kotlin-argparser/%5FlatestVersion)
-->

[![Maven Central](https://img.shields.io/maven-central/v/com.xenomachina/kotlin-argparser.svg)](https://mvnrepository.com/artifact/com.xenomachina/kotlin-argparser)
[![Build Status](https://travis-ci.org/xenomachina/kotlin-argparser.svg?branch=master)](https://travis-ci.org/xenomachina/kotlin-argparser)
[![codebeat badge](https://codebeat.co/badges/902174e2-31be-4f9d-a4ba-40178b075d2a)](https://codebeat.co/projects/github-com-xenomachina-kotlin-argparser-master)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin)
[![Javadocs](https://www.javadoc.io/badge/com.xenomachina/kotlin-argparser.svg)](https://www.javadoc.io/doc/com.xenomachina/kotlin-argparser)
[![License: LGPL 2.1](https://img.shields.io/badge/license-LGPL--2.1-blue.svg)](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html)


This is a library for parsing command-line arguments.  It can parse both
options and positional arguments.  It aims to be easy to use and concise yet
powerful and robust.


## Overview

Defining options and positional arguments is as simple as:

```kotlin
import com.xenomachina.argparser.ArgParser

class MyArgs(parser: ArgParser) {
    val v by parser.flagging("enable verbose mode")

    val name by parser.storing("name of the user")

    val count by parser.storing("number of the widgets") { toInt() }

    val source by parser.positional("source filename")

    val destination by parser.positional("destination filename")
}
```

An instance of `MyArgs` will represent the set of parsed arguments. Each option
and positional argument is declared as a property that delegates through a
delegate factory method on an instance of `ArgParser`.

The name of an option is inferred from the name of the property it is bound to.
The options above are named `-v`, `--name` and `--count`, respectively. There
are also two positional arguments.

Direct control over an option's name is also possible, and for most types of
options it is also possible to have multiple names, like a short and long name:


```kotlin
class MyArgs(parser: ArgParser) {
    val verbose by parser.flagging(
        "-v", "--verbose",
        help = "enable verbose mode")

    val name by parser.storing(
        "-N", "--name",
        help = "name of the user")

    val count by parser.storing(
        "-c", "--count",
        help = "number of widgets") { toInt() }

    val source by parser.positional(
        "SOURCE",
        help = "source filename")

    val destination by parser.positional(
        "DEST",
        help = "destination filename")
}
```

The unparsed command-line arguments are passed to the `ArgParser` instance at
construction:

```kotlin
fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::MyArgs).run {
        println("Hello, ${name}!")
        println("I'm going to move ${count} widgets from ${source} to ${destination}.")
        // TODO: move widgets
    }
}
```

See [kotlin-argparser-example](https://github.com/xenomachina/kotlin-argparser-example)
for a complete example project.


## Nomenclature

Options, arguments, flags... what's the difference?

An application's `main` function is passed an array of strings. These are
the *unparsed command-line arguments*, or *unparsed arguments* for short.

The unparsed arguments can then be parsed into *options*, which start
with a hyphen ("`-`"), and *positional arguments*. For example, in the command
`ls -l /tmp/`, the unparsed arguments would be `"-l", "/tmp"` where `-l`
is an option, while `/tmp/` is a positional argument.

Options can also have *option arguments*. In the command `ls --time-style=iso`,
the option is `--time-style` and that options argument is `iso`. Note that in
parsing a single unparsed argument can be split into an option and an option
argument, or even into multiple options in some cases.

A *flag* is a boolean option which has no arguments and which is false if not
provided, but true if provided. The `-l` option of `ls` is a flag.

## Option Types

### Boolean Flags

Boolean flags are created by asking the parser for a `flagging` delegate.  One
or more option names, may be provided:

```kotlin
val verbose by parser.flagging("-v", "--verbose",
                               help = "enable verbose mode")
```

Here the presence of either `-v` or `--verbose` options in the
arguments will cause the `Boolean` property `verbose` to be `true`, otherwise
it will be `false`.

### Storing a Single Argument

Single argument options are created by asking the parser for a
`storing` delegate.

```kotlin
val name by parser.storing("-N", "--name",
                           help = "name of the user")
```

Here either `-N` or `--name` with an argument will cause the `name` property to
have that argument as its value.

A function can also be supplied to transform the argument into the desired
type. Here the `size` property will be an `Int` rather than a `String`:

```kotlin
val size by parser.storing("-c", "--count",
                           help = "number of widgets") { toInt() }
```

### Adding to a Collection

Options that add to a `Collection` each time they appear in the arguments are
created with using the `adding` delegate. Just like `storing` delegates, a
transform function may optionally be supplied:

```kotlin
val includeDirs by parser.adding(
        "-I", help = "directory to search for header files") { File(this) }
```

Now each time the `-I` option appears, its transformed argument is appended to
`includeDirs`.

### Mapping from an option to a fixed value

For choosing between a fixed set of values (typically, but not necessarily,
from an enum), a `mapping` delegate can be used:

```kotlin
val mode by parser.mapping(
        "--fast" to Mode.FAST,
        "--small" to Mode.SMALL,
        "--quiet" to Mode.QUIET,
        help = "mode of operation")
```

Here the `mode` property will be set to the corresponding `ArgParser.Mode` value depending
on which of `--fast`, `--small`, and `--quiet` appears (last) in the arguments.

`mapping` is one of the few cases where it is not possible to infer the option
name from the property name.

### More advanced options

For all other types of options, the `option` method should be used. The
methods mentioned above are, in fact, convenience methods built on top of the
`option` method.

For example, it is possible to create an option that has multiple arguments:

```kotlin
  fun ArgParser.putting(vararg names: String, help: String) =
          option<MutableMap<String, String>>(*names,
                  argNames = listOf("KEY", "VALUE"),
                  help = help) {
              value.orElse { mutableMapOf<String, String>() }.apply {
                  put(arguments.first(), arguments.last()) }
          }
```

Note that the `option` method does not have an auto-naming overload. If you
need this capability, create a `DelegateProvider` that creates your `Delegate`:

```kotlin
  fun ArgParser.putting(help: String) =
          ArgParser.DelegateProvider { identifier ->
              putting(identifierToOptionName(identifier), help = help) }
```


## Positional Arguments

Positional arguments are collected by using the `positional` and
`positionalList` methods.

For a single positional argument:

```kotlin
val destination by parser.positional("destination filename")
```

An explicit name may also be specified:

```kotlin
val destination by parser.positional("DEST",
                                     help = "destination filename")
```

The name ("DEST", here) is used in error handling and help text.

For a list of positional arguments:

```kotlin
val sources by parser.positionalList("SOURCE", 1..Int.MAX_VALUE,
                                     help = "source filename")
```

The range indicates how many arguments should be collected, and defaults to the
value shown in this example. As the name suggests, the resulting property will
be a `List`.

Both of these methods accept an optional transform function for converting
arguments from `String` to whatever type is actually desired:

```kotlin
val destination by parser.positional("DEST",
                                     help = "...") { File(this) }

val sources by parser.positionalList("SOURCE", 1..Int.MAX_VALUE,
                                     help = "...") { File(this) }
```


## Modifying Delegates

The delegates returned by any of these methods also have a few methods for setting
optional attributes:

### Adding a Default Value

Certain types of delegates (notably `storing`, `mapping`, and `positional`)
have no default value, and hence will be required options unless a default
value is provided. This is done with the `default` method:

```kotlin
val name by parser.storing("-N", "--name", help = "...").default("John Doe")
```

Note that it *is* possible to use `null` for the default, though this may
require specifying the type parameter for `default` explicitly:

```kotlin
val name by parser.storing("-N", "--name", help = "...").default<String?>(null)
```

The type of the resulting property be nullable (a `String?` in this case).


### Adding a Validator

Sometimes it's easier to validate an option at the end of parsing, in which
case the `addValidator` method can be used.

```kotlin
val percentages by parser.adding("--percentages", help = "...") { toInt() }
        .addValidator {
              if (value.sum() != 100)
                  throw InvalidArgumentException(
                          "Percentages must add up to 100%")
        }
```

## Error Handling

If the parser determines that execution should not continue it will throw a
`SystemExitException` which has a status code appropriate for passing to
`exitProcess` as well as a message for the user.

These exceptions can be caused by user error, or even if the user requests help
(eg: via the `--help` option).

It is recommended that transform functions (given to `storing`,
`positionalList`, etc.) and post-parsing validation, including that performed
via, `addValidator` also throw a `SystemExitException` on failure.

As a convenience, these exceptions can be handled by using the `mainBody`
function:

```kotlin
class ParsedArgs(parser: ArgParser) {
    val name by positional("The user's name").default("world")
}

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::ParsedArgs).run {
        println("Hello, {name}!")
    }
}
```

## Parsing

Parsing of command-line arguments is performed sequentially. So long as
option-processing is enabled, each not-yet-processed command-line argument that
starts with a hyphen (`-`) is treated as an option.

### Short Options

Short options start with a single hyphen. If the option takes an argument, the
argument can either be appended:

```bash
# "-o" with argument "ARGUMENT"
my_program -oARGUMENT
```

or can be the following command-line argument:

```bash
# "-o" with argument "ARGUMENT"
my_program -o ARGUMENT
```

Zero argument short options can also be appended to each other without
intermediate hyphens:

```bash
# "-x", "-y" and "-z" options
my_program -xyz
```

An option that accepts arguments is also allowed at the end of such a chain:

```bash
# "-x", "-y" and "-z" options, with argument for "-z"
my_program -xyzARGUMENT
```

### Long Options

Long options start with a double hyphen (`--`). An argument to a long option
can
either be delimited with an equal sign (`=`):

```bash
# "--foo" with argument "ARGUMENT"
my_program --foo=ARGUMENT
```

or can be the following command-line argument:

```bash
# "--foo" with argument "ARGUMENT"
my_program --foo ARGUMENT
```

### Multi-argument Options

Multi-argument options are supported, though currently not by any of the
convenience methods. Option-arguments after the first must be separate
command-line arguments, for both an long and short forms of an option.

### Positional Arguments

In GNU mode (the default), options can be interspersed with positional
arguments, but in POSIX mode the first positional argument that is encountered
disables option processing for the remaining arguments. In either mode, if the
argument "--" is encountered while option processing is enabled, then option
processing is disabled for the rest of the command-line. Once the options and
option-arguments have been eliminated, what remains are considered to be
positional arguments.

Each positional argument delegate can specify a minimum and maximum number of
arguments it is willing to collect.

The positional arguments are distributed to the delegates by allocating each
positional delegate at least as many arguments as it requires. If more than the
minimum number of positional arguments have been supplied then additional
arguments will be allocated to the first delegate up to its maximum, then the
second, and so on, until all arguments have been allocated to a delegate.

This makes it easy to create a program that behaves like `grep`:

```kotlin
class Args(parser: ArgParser) {
    // accept 1 regex followed by n filenames
    val regex by parser.positional("REGEX",
            help = "regular expression to search for")
    val files by parser.positionalList("FILE",
            help = "file to search in")
}
```

And equally easy to create a program that behaves like `cp`:

```kotlin
class Args(parser: ArgParser) {
    // accept n source files followed by 1 destination
    val sources by parser.positionalList("SOURCE",
            help = "source file")
    val destination by parser.positional("DEST",
            help = "destination file")
}
```


## Forcing Parsing

Parsing normally does not begin until a delegate's value is accessed. Sometimes
this is not desirable, so it is possible to enforce the parsing of arguments
into a class of values. This ensures that all arguments that are required are
provided, and all arguments provided are consumed.

Forcing can be done in a separate step using the `force` method:

```kotlin
val parser = ArgParser(args)
val parsedArgs = ParsedArgs(parser)
parser.force()
// now you can use parsedArgs
```

Alternatively, forcing can be done inline via the `parseInto` method:

```kotlin
val parsedArgs = ArgParser(args).parseInto(::ParsedArgs)
// now you can use parsedArgs
```

In both cases exceptions will be thrown where parsing or validation errors are found.    

## Help Formatting

By default, `ArgParser` will add a `--help` option (short name `-h`) for
displaying usage information. If this option is present a `ShowHelpException` will be thrown.
If the default exception handling is being used (see [Error Handling](#error-handling)) the
program will halt and print a help message like the one below, based on the `ArgParser`
configuration:

    usage: program_name [-h] [-n] [-I INCLUDE]... -o OUTPUT
                        [-v]... SOURCE... DEST


    This is the prologue. Lorem ipsum dolor sit amet, consectetur
    adipiscing elit. Aliquam malesuada maximus eros. Fusce
    luctus risus eget quam consectetur, eu auctor est
    ullamcorper. Maecenas eget suscipit dui, sed sodales erat.
    Phasellus.


    required arguments:
      -o OUTPUT,          directory in which all output should
      --output OUTPUT     be generated


    optional arguments:
      -h, --help          show this help message and exit

      -n, --dry-run       don't do anything

      -I INCLUDE,         search in this directory for header
      --include INCLUDE   files

      -v, --verbose       increase verbosity


    positional arguments:
      SOURCE              source file

      DEST                destination file


    This is the epilogue. Lorem ipsum dolor sit amet,
    consectetur adipiscing elit. Donec vel tortor nunc. Sed eu
    massa sed turpis auctor faucibus. Donec vel pellentesque
    tortor. Ut ultrices tempus lectus fermentum vestibulum.
    Phasellus.

The creation of the `--help` option can be disabled by passing `null` as the
`helpFormatter` when constructing the `ArgParser`, or configured by manually
constructing a `HelpFormatter` instance. In the above example a
`DefaultHelpFormatter` was created with the prologue and epilogue.


## Caveats

- This library should be considered to be *very beta*. While there are no plans
  to make any breaking changes to the API, it's possible that there may be some
  until it is mature.

- Upon reading the value any of the delegated properties created by an
  `ArgParser`, the arguments used to construct that `ArgParser` will be
  parsed. This means it's important that you don't attempt to create delegates
  on an `ArgParser` after any of its existing delegated properties have been
  read. Attempting to do so will cause an `IllegalStateException`. It would be
  nice if Kotlin had facilities for doing some of the work of `ArgParser` at
  compile time rather than run time, but so far the run time errors seem to be
  reasonably easy to avoid.


## Configuring Your Build

<!-- TODO move detailed instructions elsewhere, just have brief instructions here -->

Kotlin-argparser binaries are hosted on Maven Central and also Bintray's
JCenter.

In Gradle, add something like this in your `build.gradle`:

```groovy
// you probably already have this part
buildscript {
    repositories {
        mavenCentral() // or jcenter()
    }
}

dependencies {
    compile "com.xenomachina:kotlin-argparser:$kotlin_argparser_version"
}
```

In Maven add something like this to your `pom.xml`:

```xml
<dependency>
    <groupId>com.xenomachina</groupId>
    <artifactId>kotlin-argparser</artifactId>
    <version>VERSION</version>
</dependency>
```

Information on setting up other build systems, as well as the current version
number, can be found on
[MVN Repository's page for Kotlin-argparser](https://mvnrepository.com/artifact/com.xenomachina/kotlin-argparser/latest).

## Thanks

Thanks to the creators of Python's
[`argparse`](https://docs.python.org/3/library/argparse.html) module, which
provided the initial inspiration for this library.

Thanks also to the team behind [Kotlin](https://kotlinlang.org/).

Finally, thanks to all of the people who have contributed
[code](https://github.com/xenomachina/kotlin-argparser/graphs/contributors)
and/or
[issues](https://github.com/xenomachina/kotlin-argparser/issues).
