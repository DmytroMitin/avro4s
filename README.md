# avro4s

[![Build Status](https://travis-ci.org/sksamuel/avro4s.png)](https://travis-ci.org/sksamuel/avro4s)
[<img src="https://img.shields.io/maven-central/v/com.sksamuel.avro4s/avro4s-core_2.11.svg?label=latest%20release%20for%202.11"/>](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22avro4s-core_2.11%22)
[<img src="https://img.shields.io/maven-central/v/com.sksamuel.avro4s/avro4s-core_2.12.svg?label=latest%20release%20for%202.12"/>](http://search.maven.org/#search%7Cga%7C1%7Cavro4s-core_2.12)
[<img src="https://img.shields.io/maven-central/v/com.sksamuel.avro4s/avro4s-core_2.13.0-M5.svg?label=latest%20release%20for%202.13"/>](http://search.maven.org/#search%7Cga%7C1%7Cavro4s-core_2.13)
[<img src="https://img.shields.io/nexus/s/https/oss.sonatype.org/com.sksamuel.avro4s/avro4s-core_2.12.svg?label=latest%20snapshot&style=plastic"/>](https://oss.sonatype.org/content/repositories/snapshots/com/sksamuel/avro4s/)

Avro4s is a schema/class generation and serializing/deserializing library for [Avro](http://avro.apache.org/) written in Scala. The objective is to allow seamless use with Scala without the need to to write boilerplate conversions yourself, and without the runtime overhead of reflection. Hence, this is a macro based library and generates code for use with Avro at _compile time_.

The features of the library are:
* Schema generation from classes at compile time
* Boilerplate free serialization of Scala types into Avro types
* Boilerplate free deserialization of Avro types to Scala types

Note: This document refers to the 3.0 release train.

## Schemas

Unlike Json, Avro is a schema based format. You'll find yourself wanting to generate schemas frequently, and writing these by hand or through the Java based `SchemaBuilder` classes can be tedious for complex domain models. Avro4s allows us to generate schemas directly from case classes at compile time via macros. This gives you both the convenience of generated code, without the annoyance of having to run a code generation step, as well as avoiding the peformance penalty of runtime reflection based code.

Let's define some classes.

```scala
case class Ingredient(name: String, sugar: Double, fat: Double)
case class Pizza(name: String, ingredients: Seq[Ingredient], vegetarian: Boolean, vegan: Boolean, calories: Int)
```

To generate an Avro Schema, we need to use the `AvroSchema` object passing in the target type as a type parameter.
This will return an `org.apache.avro.Schema` instance.

```scala
import com.sksamuel
val schema = AvroSchema[Pizza]
```

Where the generated schema is as follows:

```json
{
   "type":"record",
   "name":"Pizza",
   "namespace":"com.sksamuel",
   "fields":[
      {
         "name":"name",
         "type":"string"
      },
      {
         "name":"ingredients",
         "type":{
            "type":"array",
            "items":{
               "type":"record",
               "name":"Ingredient",
               "fields":[
                  {
                     "name":"name",
                     "type":"string"
                  },
                  {
                     "name":"sugar",
                     "type":"double"
                  },
                  {
                     "name":"fat",
                     "type":"double"
                  }
               ]
            }
         }
      },
      {
         "name":"vegetarian",
         "type":"boolean"
      },
      {
         "name":"vegan",
         "type":"boolean"
      },
      {
         "name":"calories",
         "type":"int"
      }
   ]
}
```
You can see that the schema generator handles nested case classes, sequences, primitives, etc. For a full list of supported object types, see the table later.

### Overriding class name and namespace

Avro schemas for complex types (RECORDS) contain a name and a namespace. By default, these are the name of the class
and the enclosing package name, but it is possible to customize these using the annotations `AvroName` and `AvroNamespace`.

For example, the following class:

```scala
package com.sksamuel
case class Foo(a: String)
```

Would normally have a schema like this:

```json
{
  "type":"record",
  "name":"Foo",
  "namespace":"com.sksamuel",
  "fields":[
    {
      "name":"a",
      "type":"string"
    }
  ]
}
```

However we can override the name and/or the namespace like this:

```scala
package com.sksamuel

@AvroName("Wibble")
@AvroNamespace("com.other")
case class Foo(a: String)
```

And then the generated schema looks like this:

```json
{
  "type":"record",
  "name":"Wibble",
  "namespace":"com.other",
  "fields":[
    {
      "name":"a",
      "type":"string"
    }
  ]
}
```

Note: It is possible, but not necessary, to use both AvroName and AvroNamespace. You can just use either of them if you wish.

### Overriding a field name

The `AvroName` annotation can also be used to override field names. This is useful when the record instances you are generating or reading need to have field names different from the scala case classes. For example if you are reading data generated by another system, or another language.

Given the following class.

```scala
package com.sksamuel
case class Foo(a: String, @AvroName("z") b : String)
```

Then the generated schema would look like this:

```json
{
  "type":"record",
  "name":"Foo",
  "namespace":"com.sksamuel",
  "fields":[
    {
      "name":"a",
      "type":"string"
    },
    {
      "name":"z",
      "type":"string"
    }    
  ]
}
```

Notice that the second field is `z` and not `b`.

Note: @AvroName does not add an alternative name for the field, but an override. If you wish to have alternatives then you want to use @AvroAlias.

### Adding properties and docs to a Schema

Avro allows a doc field, and arbitrary key/values to be added to generated schemas. Avro4s supports this through the use of `AvroDoc` and `AvroProp` annotations.

These properties works on either complex or simple types - in other words, on both fields and classes. For example:

```scala
package com.sksamuel
@AvroDoc("hello, is it me you're looking for?")
case class Foo(@AvroDoc("I am a string") str: String, @AvroDoc("I am a long") long: Long, int: Int)
```

Would result in the following schema:

```json
{  
  "type": "record",
  "name": "Foo",
  "namespace": "com.sksamuel",
  "doc":"hello, is it me you're looking for?",
  "fields": [  
    {  
      "name": "str",
      "type": "string",
      "doc" : "I am a string"
    },
    {  
      "name": "long",
      "type": "long",
      "doc" : "I am a long"
    },
    {  
      "name": "int",
      "type": "int"
    }
  ]
}
```

An example of properties:

```scala
package com.sksamuel
@AvroProp("jack", "bruce")
case class Annotated(@AvroProp("richard", "ashcroft") str: String, @AvroProp("kate", "bush") long: Long, int: Int)
```

Would generate this schema:

```json
{
  "type": "record",
  "name": "Annotated",
  "namespace": "com.sksamuel",
  "fields": [
    {
      "name": "str",
      "type": "string",
      "richard": "ashcroft"
    },
    {
      "name": "long",
      "type": "long",
      "kate": "bush"
    },
    {
      "name": "int",
      "type": "int"
    }
  ],
  "jack": "bruce"
}
```

### Overriding a Schema

Behind the scenes, `AvroSchema` uses an implicit `SchemaFor`. This is the core typeclass which generates an Avro schema for a given Java or Scala type. There are `SchemaFor` instances for all the common JDK and SDK types, as well as macros that generate instances for case classes.

In order to override how a schema is generated for a particular type you need to bring into scope an implicit `SchemaFor` for the type you want to override. As an example, lets say you wanted all integers to be encoded as `Schema.Type.STRING` rather than the standard `Schema.Type.INT`.

To do this, we just introduce a new instance of `SchemaFor` and put it in scope when we generate the schema.

```scala
implicit object IntOverride extends SchemaFor[Int] {
  override def schema(implicit naming: NamingStrategy = DefaultNamingStrategy): Schema = SchemaBuilder.builder.stringType
}

case class Foo(a: String)
val schema = AvroSchema[Foo]
```

Note: If you create an override like this, be aware that schemas in Avro are mutable, so don't share the values that the typeclasses return.

### Recursive Schemas

Avro4s supports recursive schemas, but you will have to manually force the `SchemaFor` instance, instead of letting it be generated.

``` scala
case class Recursive(payload: Int, next: Option[Recursive])
implicit val schemaFor = SchemaFor[Recursive]
val schema = AvroSchema[Recursive]
```

### Transient Fields

Avro4s does not support the @transient anotation to mark a field as ignored, but instead supports its own @AvroTransient annotation to do the same job. Any field marked with this will be excluded from the generated schema.

```scala
package com.sksamuel
case class Foo(a: String, @AvroTransient b: String)
```

Would result in the following schema:

```json
{  
  "type": "record",
  "name": "Foo",
  "namespace": "com.sksamuel",
  "fields": [  
    {  
      "name": "a",
      "type": "string"
    }
  ]
}
```

### Naming Strategy

If you are dealing with Avro data generated in other languages then it's quite likely the field names will reflect the style of that language. For example, Java may prefer `camelCaseFieldNames` but other languages may use `snake_case_field_names` or `PascalStyleFieldNames`. By default the name of the field in the case class is what will be used, and you've seen earlier that you can override a specific field with @AvroName, but doing this for every single field would be insane.

So, avro4s provides `NamingStrategy` for this. You simply bring into scope an instance of NamingStrategy that will convert the scala field names into a target type field names.

For example, lets take a scala case and generate a schema using snake case.

```scala
package com.sksamuel
case class Foo(userName: String, emailAddress: String)
implicit val snake: NamingStrategy = SnakeCase
val schema = AvroSchema[Foo]
```

Would generate the following schema:

```json
{
  "type": "record",
  "name": "Foo",
  "namespace": "com.sksamuel",
  "fields": [
    {
      "name": "user_name",
      "type": "string"
    },
    {
      "name": "email_address",
      "type": "string"
    }
  ]
}
```

### Avro Fixed

Avro supports the idea of fixed length byte arrays. To use these we can either override the schema generated for a type to return `Schema.Type.Fixed`. This will work for types like String or UUID. You can also annotate a field with @AvroFixed(size).
For example:

```scala
package com.sksamuel
case class Foo(@AvroFixed(7) mystring: String)
val schema = AvroSchema[Foo]
```

Will generate the following schema:

```json
{
  "type": "record",
  "name": "Foo",
  "namespace": "com.sksamuel",
  "fields": [
    {
      "name": "mystring",
      "type": {
        "type": "fixed",
        "name": "mystring",
        "size": 7
      }
    }
  ]
}
```

If you have a value type that you always want to be represented as fixed, then rather than annotate every single location it is used, you can annotate the value type itself.

```scala
package com.sksamuel

@AvroFixed(4)
case class FixedA(bytes: Array[Byte]) extends AnyVal

case class Foo(a: FixedA)
val schema = AvroSchema[Foo]
```

And this would generate:

```json
{
  "type": "record",
  "name": "Foo",
  "namespace": "com.sksamuel",
  "fields": [
    {
      "name": "a",
      "type": {
        "type": "fixed",
        "name": "FixedA",
        "size": 4
      }
    }
  ]
}
```

Finally, these annotated value types can be used as top level schemas too:

```scala
package com.sksamuel

@AvroFixed(6)
case class FixedA(bytes: Array[Byte]) extends AnyVal
val schema = AvroSchema[FixedA]
```

```json
{
  "type": "fixed",
  "name": "FixedA",
  "namespace": "com.sksamuel",
  "size": 6
}
```

## Input / Output

### Serializing

Avro4s allows us to easily serialize case classes using an instance of `AvroOutputStream` which we write to, and close, just like you would any regular output stream.
An `AvroOutputStream` can be created from a `File`, `Path`, or by wrapping another `OutputStream`.
When we create one, we specify the type of objects that we will be serializing and provide a writer schema.
For example, to serialize instances of our Pizza class:

```scala
import java.io.File
import com.sksamuel.avro4s.AvroOutputStream

val pepperoni = Pizza("pepperoni", Seq(Ingredient("pepperoni", 12, 4.4), Ingredient("onions", 1, 0.4)), false, false, 598)
val hawaiian = Pizza("hawaiian", Seq(Ingredient("ham", 1.5, 5.6), Ingredient("pineapple", 5.2, 0.2)), false, false, 391)

val schema = AvroSchema[Pizza]

val os = AvroOutputStream.data[Pizza].to(new File("pizzas.avro")).build(schema)
os.write(Seq(pepperoni, hawaiian))
os.flush()
os.close()
```

### Deserializing

We can easily deserialize a file back into case classes.
Given the `pizzas.avro` file we generated in the previous section on serialization, we will read this back in using the `AvroInputStream` class.
We first create an instance of the input stream specifying the types we will read back, the source file, and then build it using a reader schema.

Once the input stream is created, we can invoke `iterator` which will return a lazy iterator that reads on demand the data in the file.

In this example, we'll load all data at once from the iterator via `toSet`.

```scala
import com.sksamuel.avro4s.AvroInputStream

val schema = AvroSchema[Pizza]

val is = AvroInputStream.data[Pizza].from(new File("pizzas.avro")).build(schema)
val pizzas = is.iterator.toSet
is.close()

println(pizzas.mkString("\n"))
```

Will print out:

```scala
Pizza(pepperoni,List(Ingredient(pepperoni,12.2,4.4), Ingredient(onions,1.2,0.4)),false,false,500)
Pizza(hawaiian,List(Ingredient(ham,1.5,5.6), Ingredient(pineapple,5.2,0.2)),false,false,500)
```

### Binary and JSON Formats

You can serialize as [binary](https://avro.apache.org/docs/1.8.2/spec.html#binary_encoding) or [json](https://avro.apache.org/docs/1.8.2/spec.html#json_encoding)
by specifying the format when creating the input or output stream. In the earlier example we use `data` which is considered the "default" for Avro.

To use json or binary, you can do the following:

```scala
AvroOutputStream.binary.to(...).build(...)
AvroOutputStream.json.to(...).build(...)

AvroInputStream.binary.from(...).build(...)
AvroInputStream.json.from(...).build(...)
```

Note: Binary serialization does not include the schema in the output.

## Avro Records

In Avro there are two container interfaces designed for complex types - `GenericRecord`, which is the most commonly used, along with the lesser used `SpecificRecord`.
These record types are used with a schema of type `Schema.Type.RECORD`.

To interface with the Avro Java API or with third party frameworks like Kafka it is sometimes desirable to convert between your case classes and these records,
rather than using the input/output streams that avro4s provides.

To perform conversions, use the `RecordFormat` typeclass which converts to/from case classes and Avro records.

Note: In Avro, `GenericRecord` and `SpecificRecord` don't have a common _Record_ interface (just a `Container` interface which simply provides for a schema without any methods for accessing values), so
avro4s has defined a `Record` trait, which is the union of the `GenericRecord` and `SpecificRecord` interfaces. This allows avro4s to generate records which implement both interfaces at the same time.

To convert from a class into a record:

```scala
case class Composer(name: String, birthplace: String, compositions: Seq[String])
val ennio = Composer("ennio morricone", "rome", Seq("legend of 1900", "ecstasy of gold"))
val format = RecordFormat[Composer]
// record is a type that implements both GenericRecord and Specific Record
val record = format.to(ennio)
```

And to go from a record back into a type:

```scala
// given some record from earlier
val record = ...
val format = RecordFormat[Composer]
val ennio = format.from(record)
```

## Type Mappings

Avro4s defines two typeclasses, `Encoder` and `Decoder` which do the work
of mapping between scala values and Avro compatible values. Avro has no understanding of Scala types, or anything outside of it's built in set of supported types, so all values must be converted to something that is compatible with Avro. There are built in encoders and decoders for all the common JDK and Scala SDK types, including macro generated instances for case classes.

For example a `java.sql.Timestamp` is usually encoded as a Long, and a `java.util.UUID` is encoded as a String.

Decoders do the same work, but in reverse. They take an Avro value, such as null and return a scala value, such as `Option`.

Some values can be mapped in multiple ways depending on how the schema was generated. For example a String, which is usually encoded as 
`org.apache.avro.util.Utf8` could also be encoded as an array of bytes if the generated schema for that field was `Schema.Type.BYTES`. Therefore some encoders will take into account the schema passed to them when choosing the avro compatible type. In the schemas section you saw how you could influence which schema is generated for types.

### Built in Type Mappings

``` scala
import scala.collection.{Array, List, Seq, Iterable, Set, Map, Option, Either}
import shapeless.{:+:, CNil}
```

The following table shows how types used in your code will be mapped / encoded in the generated Avro schemas and files.
If a type can be mapped in multiple ways, it is listed more than once.

| Scala Type                   	| Schema Type   	| Logical Type     	| Encoded Type |
|------------------------------	|---------------	|------------------	| ------------ |
| String                       	| STRING        	|                  	| Utf8                      |
| String                       	| FIXED        	|                  	| GenericFixed         |
| String                       	| BYTES        	|                  	| ByteBuffer         |
| Boolean                      	| BOOLEAN       	|                  	| java.lang.Boolean |
| Long                         	| LONG          	|                  	| java.lang.Long |
| Int                          	| INT           	|                  	| java.lang.Integer |
| Short                        	| INT           	|                  	| java.lang.Integer |
| Byte                         	| INT           	|                  	| java.lang.Integer |
| Double                       	| DOUBLE        	|                  	| java.lang.Double |
| Float                        	| FLOAT         	|                  	| java.lang.Float |
| UUID                         	| STRING        	| UUID             	| Utf8 |
| LocalDate                    	| INT           	| Date             	| java.lang.Int |
| LocalTime                    	| INT           	| Time-Millis      	| java.lang.Int |
| LocalDateTime                	| LONG          	| Timestamp-Millis 	| java.lang.Long |
| java.sql.Date                	| INT           	| Date             	| java.lang.Int |
| Instant                      	| LONG          	| Timestamp-Millis 	| java.lang.Long |
| Timestamp                    	| LONG          	| Timestamp-Millis 	| java.lang.Long |
| BigDecimal                   	| BYTES         	| Decimal<8,2>     	| ByteBuffer |
| BigDecimal                   	| FIXED         	| Decimal<8,2>     	| GenericFixed |
| BigDecimal                   	| STRING         	| Decimal<8,2>     	| String |
| Option[T]                    	| UNION<null,T> 	|                  	| null, T |
| Array[Byte]                  	| BYTES         	|                  	| ByteBuffer |
| Array[Byte]                  	| FIXED         	|                  	| GenericFixed |
| ByteBuffer                   	| BYTES         	|                  	| ByteBuffer |
| Seq[Byte]                    	| BYTES         	|                  	| ByteBuffer |
| List[Byte]                   	| BYTES         	|                  	| ByteBuffer |
| Vector[Byte]                 	| BYTES         	|                  	| ByteBuffer |
| Array[T]                     	| ARRAY<T>      	|                  	| Array[T] |
| Vector[T]                    	| ARRAY<T>      	|                  	| Array[T] |
| Seq[T]                       	| ARRAY<T>      	|                  	| Array[T] |
| List[T]                      	| ARRAY<T>      	|                  	| Array[T] |
| Set[T]                       	| ARRAY<T>      	|                  	| Array[T] |
| sealed trait of case classes 	| UNION<A,B,..>  	|                  	| A, B, ... |
| sealed trait of case objects 	| ENUM<A,B,..>  	|                  	| GenericEnumSymbol |
| Map[String, V]              	| MAP<V>        	|                  	| java.util.Map[String, V] |
| Either[A,B]                  	| UNION<A,B>    	|                  	| A, B |
| A :+: B :+: C :+: CNil       	| UNION<A,B,C>  	|                  	| A, B, ... |
| case class T                 	| RECORD        	|                  	| GenericRecord with SpecificRecord |
| Scala enumeration            	| ENUM          	|                  	| GenericEnumSymbol |
| Java enumeration             	| ENUM          	|                  	| GenericEnumSymbol |
| Scala tuples                  | RECORD            |                   | GenericRecord with SpecificRecord |

### Custom Type Mappings

It is very easy to add custom type mappings. To do this, we bring into scope a custom implicit of `Encoder[T]` and/or `Decoder[T]`.

For example, to create a custom type mapping for a type Foo which writes out the contents in upper case, but always reads
the contents in lower case, we can do the following:

```scala
case class Foo(a: String, b: String)

implicit object FooEncoder extends Encoder[Foo] {
  override def encode(foo: Foo, schema: Schema, naming: NamingStrategy) = {
    val record = new GenericData.Record(schema)
    record.put("a", foo.a.toUpperCase)
    record.put("b", foo.b.toUpperCase)
    record
  }
}

implicit object FooDecoder extends Decoder[Foo] {
  override def decode(value: Any, schema: Schema, naming: NamingStrategy) = {
    val record = value.asInstanceOf[GenericRecord]
    Foo(record.get("a").toString.toLowerCase, record.get("b").toString.toLowerCase)
  }
}
```

Another example is changing the way we serialize `LocalDateTime` to store these dates as ISO strings. In this case, we are
writing out a String rather than the default Long so we must also change the schema type. Therefore, we must add an implicit `SchemaFor` as well as the encoders
and decoders.

```scala
implicit object LocalDateTimeSchemaFor extends SchemaFor[LocalDateTime] {
  override val schema(implicit naming: NamingStrategy = DefaultNamingStrategy) = 
    Schema.create(Schema.Type.STRING)
}

implicit object DateTimeEncoder extends Encoder[LocalDateTime] {
  override def apply(value: LocalDateTime, schema: Schema, naming: NamingStrategy) = 
    ISODateTimeFormat.dateTime().print(value)
}

implicit object DateTimeDecoder extends Decoder[LocalDateTime] {
  override def apply(value: Any, field: Field)(implicit naming: NamingStrategy = DefaultNamingStrategy) = 
    ISODateTimeFormat.dateTime().parseDateTime(value.toString)
}
```

These typeclasses must be implicit and in scope when you use `AvroSchema` or `RecordFormat`.

### Coproducts

Avro supports generalised unions, eithers of more than two values.
To represent these in scala, we use `shapeless.:+:`, such that `A :+: B :+: C :+: CNil` represents cases where a type is `A` OR `B` OR `C`.
See shapeless' [documentation on coproducts](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#coproducts-and-discriminated-unions) for more on how to use coproducts.

### Sealed hierarchies

Scala sealed traits/classes are supported both when it comes to schema generation and conversions to/from `GenericRecord`.
Generally sealed hierarchies are encoded as unions - in the same way like Coproducts.
Under the hood, shapeless `Generic` is used to derive Coproduct representation for sealed hierarchy.

When all descendants of sealed trait/class are singleton objects, optimized, enum-based encoding is used instead.


## Decimal scale, precision and rounding mode

In order to customize the scale and precision used by `BigDecimal` schema generators, bring an implicit `ScalePrecision` instance into scope.before using `AvroSchema`.

```scala
import com.sksamuel.avro4s.ScalePrecision

case class MyDecimal(d: BigDecimal)

implicit val sp = ScalePrecision(4, 20)
val schema = AvroSchema[MyDecimal]
```

```json
{
  "type":"record",
  "name":"MyDecimal",
  "namespace":"com.foo",
  "fields":[{
    "name":"d",
    "type":{
      "type":"bytes",
      "logicalType":"decimal",
      "scale":"4",
      "precision":"20"
    }
  }]
}
```

When encoding values, it may be necessary to round values if they need to be converted to the scale used by the schema. By default this is `RoundingMode.UNNECESSARY` which will throw an exception if rounding is required.
In order to change this, add an implicit `RoundingMode` value before the `Encoder` is generated.

```scala
case class MyDecimal(d: BigDecimal)

implicit val sp = ScalePrecision(4, 20)
val schema = AvroSchema[MyDecimal]

implicit val roundingMode = RoundingMode.HALF_UP
val encoder = Encoder[MyDecimal]
``` 

## Type Parameters

When serializing a class with one or more type parameters, the avro name used in a schema is the name of the raw type, plus the actual type parameters. In other words, it would be of the form `rawtype__typeparam1_typeparam2_..._typeparamN`. So for example, the schema for a type `Event[Foo]` would have the avro name `event__foo`.

You can disable this by annotating the class with `@AvroErasedName` which uses the JVM erased name - in other words, it drops type parameter information. So the aforementioned `Event[Foo]` would be simply `event`.

## Selective Customisation

You can selectively customise the way Avro4s generates certain parts of your hierarchy, thanks to implicit precedence. Suppose you have the following classes:

```scala
case class Product(name: String, price: Price, litres: BigDecimal)
case class Price(currency: String, amount: BigDecimal)
```

And you want to selectively use different scale/precision for the `price` and `litres` quantities. You can do this by forcing the implicits in the corresponding companion objects.

``` scala
object Price {
  implicit val sp = ScalePrecisionRoundingMode(10, 2)
  implicit val schema = SchemaFor[Price]
}

object Product {
  implicit val sp = ScalePrecisionRoundingMode(8, 4)
  implicit val schema = SchemaFor[Product]
}
```

This will result in a schema where both `BigDecimal` quantities have their own separate scale and precision.

## Cats Support

If you use cats in your domain objects, then Avro4s provides a cats module with schemas, encoders and decoders for some cats types.
Just import `import com.sksamuel.avro4s.cats._` before calling into the macros.

```scala
case class Foo(list: NonEmptyList[String], vector: NonEmptyVector[Boolean])
val schema = AvroSchema[Foo]
```

## Refined Support

If you use [refined](https://github.com/fthomas/refined) in your domain objects, then Avro4s provides a refined module with schemas, encoders and decoders for refined types.
Just import `import com.sksamuel.avro4s.refined._` before calling into the macros.

```scala
case class Foo(nonEmptyStr: String Refined NonEmpty)
val schema = AvroSchema[Foo]
```


## Using avro4s in your project

#### Gradle

`compile 'com.sksamuel.avro4s:avro4s-core_2.12:xxx'`

#### SBT

`libraryDependencies += "com.sksamuel.avro4s" %% "avro4s-core" % "xxx"`

#### Maven

```xml
<dependency>
    <groupId>com.sksamuel.avro4s</groupId>
    <artifactId>avro4s-core_2.12</artifactId>
    <version>xxx</version>
</dependency>
```

Check the latest released version on [Maven Central](http://search.maven.org/#search|ga|1|g%3A%22com.sksamuel.avro4s%22)

## Contributions
Contributions to avro4s are always welcome. Good ways to contribute include:

* Raising bugs and feature requests
* Fixing bugs and enhancing the DSL
* Improving the performance of avro4s
* Adding to the documentation
