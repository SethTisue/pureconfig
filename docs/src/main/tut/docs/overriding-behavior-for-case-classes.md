---
layout: docs
title: Case Classes
---

## Case Classes

PureConfig has to assume some conventions and behaviors when deriving
`ConfigReader` instances for case classes:

- How do keys in config objects map to field names of the case class?
- Are unknown keys allowed in the config object?
- Should default values in case class fields be applied when its respective
  config key is missing?

By default, PureConfig:

- expects config keys to be written in kebab case (such as `my-field`) and the
associated field names are written in camel case (such as `myField`);
- allows unknown keys;
- uses the default values when a key is missing.

All of these assumptions can be overridden by putting an implicit
`ProductHint` in scope - an object that "hints" PureConfig on how to best derive
converters for products.

### Field mappings

In case the naming convention you use in your configuration files differs from
the default one, PureConfig allows you to define the mappings to use. A mapping
between different naming conventions is done using a `ConfigFieldMapping`
object, with which one can construct a `ProductHint`. The `ConfigFieldMapping`
trait has a single `apply` method that maps field names in Scala objects to
field names in the source configuration file.

For instance, here's a contrived
example where the configuration file has all keys in upper case and we're
loading it into a type whose fields are all in lower case. First, define a `ProductHint`
instance in implicit scope:

```tut:silent
import com.typesafe.config.ConfigFactory
import pureconfig._

case class SampleConf(foo: Int, bar: String)

implicit val productHint = ProductHint[SampleConf](new ConfigFieldMapping {
  def apply(fieldName: String) = fieldName.toUpperCase
})
```

Then load a config:
```tut:book
loadConfig[SampleConf](ConfigFactory.parseString("{ FOO: 2, BAR: two }"))
```

PureConfig provides a way to create a `ConfigFieldMapping` by defining the
naming conventions of the fields in the Scala object and in the configuration
file. Some of the most used naming conventions are supported directly in the
library:

* [`CamelCase`](https://en.wikipedia.org/wiki/Camel_case) (examples: `camelCase`, `useMorePureconfig`);
* [`SnakeCase`](https://en.wikipedia.org/wiki/Snake_case) (examples: `snake_case`, `use_more_pureconfig`);
* [`KebabCase`](http://wiki.c2.com/?KebabCase): (examples: `kebab-case`, `use-more-pureconfig`);
* [`PascalCase`](https://en.wikipedia.org/wiki/PascalCase): (examples: `PascalCase`, `UseMorePureconfig`).

You can use the `apply` method of `ConfigFieldMapping` that accepts the two
naming conventions (for the fields in the Scala object and for the fields in the
configuration file, respectively). A common use case is to have both your field
names and your configuration files in `camelCase`. In order to support it, you
can make sure the following implicit is in scope before loading or writing
configuration files:

```tut:silent
import pureconfig._

implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
```

### Default field values

If a case class has a default argument and the underlying configuration is
missing a value for that field, then by default PureConfig will happily
create an instance of the class, loading the other values from the
configuration.

For example, with this setup:

```tut:reset:silent
import com.typesafe.config.ConfigFactory
import pureconfig._
import scala.concurrent.duration._

case class Holiday(where: String = "last resort", howLong: Duration = 7 days)
```

We can load configurations using default values:

```tut:book
// Defaulting `where`
loadConfig[Holiday](ConfigFactory.parseString("{ how-long: 21 days }"))

// Defaulting `howLong`
loadConfig[Holiday](ConfigFactory.parseString("{ where: Zürich }"))

// Defaulting both arguments
loadConfig[Holiday](ConfigFactory.parseString("{}"))

// Specifying both arguments
loadConfig[Holiday](ConfigFactory.parseString("{ where: Texas, how-long: 3 hours }"))
```

A `ProductHint` can make the conversion fail if a key is missing from the
config regardless of whether a default value exists or not:

```tut:silent
implicit val hint = ProductHint[Holiday](useDefaultArgs = false)
```

```tut:book
loadConfig[Holiday](ConfigFactory.parseString("{ how-long: 21 days }"))
```

### Unknown keys

By default, PureConfig ignores keys in the config that do not map to any
case class field, leading to potential bugs due to misspellings:

```tut:reset:invisible
// reset tut's REPL session to remove the implicit ProductHint defined above.
import com.typesafe.config.ConfigFactory
import pureconfig._
import scala.concurrent.duration._

case class Holiday(where: String = "last resort", howLong: Duration = 7 days)
```

```tut:book
loadConfig[Holiday](ConfigFactory.parseString("{ wher: Texas, how-long: 21 days }"))
```

With a `ProductHint`, one can tell the converter to fail if an unknown key is
found:

```tut:book
implicit val hint = ProductHint[Holiday](allowUnknownKeys = false)

loadConfig[Holiday](ConfigFactory.parseString("{ wher: Texas, how-long: 21 days }"))
```

### Missing keys

The default behavior of `ConfigReader`s that are derived in PureConfig is to return a `KeyNotFound` failure when a
required key is missing unless its type is an `Option`, in which case it is read as a `None`.

Consider this configuration:

```tut:reset:silent
import com.typesafe.config.ConfigFactory
import pureconfig._

case class Foo(a: Int)
case class FooOpt(a: Option[Int])
```

Loading a `Foo` results in a `Left` because of missing keys, but loading a `FooOpt` produces a `Right`:

```tut:book
loadConfig[Foo](ConfigFactory.empty)
loadConfig[FooOpt](ConfigFactory.empty)
```

However, if you want to allow your custom `ConfigReader`s to handle missing keys, you can extend the `AllowMissingKey`
trait. For `ConfigReader`s extending `AllowMissingKey`, a missing key will issue a call to the `from` method of the
available `ConfigReader` for that type with a [cursor](config-cursors.html) to an undefined value.

Under this setup:

```tut:silent
implicit val maybeIntReader = new ConfigReader[Int] with AllowMissingKey {
  override def from(cur: ConfigCursor) =
    if (cur.isUndefined) Right(42) else ConfigReader[Int].from(cur)
}
```

You can load an empty configuration and get a `Right`:

```tut:book
loadConfig[Foo](ConfigFactory.empty)
```
