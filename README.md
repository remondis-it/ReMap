[![Maven Central](https://img.shields.io/maven-central/v/com.remondis/remap.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.remondis%22%20AND%20a:%22remap%22)
[![JCenter](https://api.bintray.com/packages/schuettec/maven/com.remondis%3Aremap/images/download.svg) ](https://bintray.com/schuettec/maven/com.remondis%3Aremap/_latestVersion)
[![Build Status](https://travis-ci.org/remondis-it/remap.svg?branch=develop)](https://travis-ci.org/remondis-it/remap)

# ReMap - A declarative object mapper

# Table of Contents
1. [Long story short](#long-story-short)
1. [Hot Tip](#hot-tip)
2. [About ReMap](#about-remap)
3. [Mapping operations](#mapping-operations)
4. [Validation](#validation)
5. [Features](#features)
6. [Limitations](#limitations)
7. [How to use](#how-to-use)
   1. [Implicit Mappings](#implicit-mappings)
   2. [Mapping fields of the same type](#mapping-fields-of-the-same-type)
   3. [Mapping fields with another mapper](#mapping-fields-with-another-mapper)
   4. [Mapping fields with different names with another mapper](#mapping-fields-with-different-names-with-another-mapper)
   5. [Mapping fields with a custom mapping function](#mapping-fields-with-a-custom-mapping-function)
   6. [Transforming collections with a custom mapping function](#transforming-collections-with-a-custom-mapping-function)
   7. [Type mappings](#type-mappings)
   8. [Mapping with property paths](#mapping-with-property-paths)
   9. [Mapping maps](#mapping-maps)
   10. [Tests](#tests)
8. [Spring integration](#spring-integration)
   1. [Spring Boot Issue](#spring-boot-issue)
9. [Migration guide](#migration-guide)
10. [How to contribute](#how-to-contribute)

## Long story short

ReMap is a library that simplifies conversion of objects field by field and greatly reduces the efforts for unit testing mapper classes. You can get this library via Maven Central using the following coordinates

ReMap is available through [![Maven Central](https://img.shields.io/maven-central/v/com.remondis/remap.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.remondis%22%20AND%20a:%22remap%22) and [JCenter](https://bintray.com/schuettec/maven/com.remondis%3Aremap/_latestVersion)



The following code snippet shows how to map a source type to a destination type:

```java
Mapping.from(Customer.class)
    .to(Person.class)
    .omitInSource(Customer::getAddress)
    .omitInDestination(Person::getBodyHeight)
    .reassign(Customer::getTitle)
        .to(Person::getSalutation)
    .replace(Customer::getGender, Person::getGender)
        .withSkipWhenNull(Gender::valueOf)
    .mapper();
```

The resulting mapper does the following:

* maps `Customer` objects to `Person` objects
* `Customer` has the field `address`, `Person` does not, hence it is omitted
* `Person` has the field `bodyHeight` and `Customer` does not, hence it is omitted
* the field `Customer.title` is mapped to the field `Person.salutation`
* the field `Customer.gender` of type `String` is mapped to the Field `Customer.gender` of enum type `Gender`
  using `Gender.valueOf()` as a transformation function

You can find this demo and the involved classes [here](src/test/java/com/remondis/remap/demo/DemoTest.java)

### Hot Tip

You can call `Mapper.toString()` to get an overview of what the mapper does. Since the first version of ReMap the mapping configuration can be visualized this way but only a few people now that.

The mapper from the example above would produce the following `toString()` output:

```
Mapping from com.remondis.remap.demo.Customer
	  to com.remondis.remap.demo.Person
 with transformation:
- Omitting Property 'bodyHeight' in com.remondis.remap.demo.Person
- Map Property 'name' in com.remondis.remap.demo.Customer
   to Property 'name' in com.remondis.remap.demo.Person
- Omitting Property 'address' in com.remondis.remap.demo.Customer
- Replacing but skipping when null Property 'gender' in com.remondis.remap.demo.Customer
           with Property 'gender' in com.remondis.remap.demo.Person using transformation
- Map Property 'forname' in com.remondis.remap.demo.Customer
   to Property 'forname' in com.remondis.remap.demo.Person
- Reassigning Property 'title' in com.remondis.remap.demo.Customer
           to Property 'salutation' in com.remondis.remap.demo.Person
All properties are mapped!
```

This may give you a better overview when trying to understand especially older mapping configurations.

## About ReMap

ReMap is a library that simplifies conversion of objects field by field. It was developed to make conversion of database entities to DTOs (data transfer objects) easier. The use of ReMap makes converter classes and unit tests for converters obsolete: ReMap only needs a specification of what fields are to be mapped, but the amount of code that actually performs the assignments and transformations is minimized. Therefore the code that must be unit-tested is also minimized.

ReMap maps a objects of a source to a destination type. As per default ReMap tries to map all fields from the source to the destination object if the fields have equal name and equal types or equal name and a mapper was registered to perform the type mapping. __Only differences between the source type and the target type must be specified when creating a mapper.__

## Mapping operations

The following operations can be declared on a mapper:
* `omitInSource`: omits a field in the source type and skips the mapping.
* `omitInDestination`: omits a field in the destination type and skips the mapping.
* `omitOtherSourceProperties`: omits all source fields that are not configured differently.
* `omitOtherDestinationProperties`: omits all source fields that are not configured differently.
* `omitOthers`: omits all source and destination fields that are not configured differently.
* `reassign`: maps a source field to the destination field
	* if the property types are equal, references are copied
	* if the property types differ, a mapper must be registered that supports the type mapping
* `replace`: converts a source field to the destination field while changing the field name and the type. To transform the source object into the destination type a transformation function is to be specified.
* `replaceCollection`: converts a source collection to the destination collection by applying a transform function elementwise.
* `set`: Sets a value provided either by a function or by a value supplier in the destination.
* `useMapper`:
    * registers a specific mapper instance that is used to convert referenced Java Bean types.
    * registers a type mapping that is used to convert non-Java Bean types.

## Validation

ReMap validates the mapping configuration of a mapper **at instantiation time** and denies the following states:
* A source field was not mapped to a destination field
* A destination field is not covered by a source field
* Multiple mappings where defined for a destination field
* `omit` is specified for a source field that already has a mapping configuration

These validation rules make sure that all fields are covered by the mapping configuration when a mapper instance is created.

## Unit Testing

Since ReMap relies on getter and setter references like `Address::getId` to specify a mapping,
the compiler does not allow mappings between fields that are incompatible. When a mapper is
instantiated, ReMap performs the above-mentioned validations.

Thus, the only things you need to test in a unit test are:
* that `Mapping.mapper()` does not throw a `MappingException` telling you that one of the validations failed
* any transformation functions specified for `replace` operations

Optionally, you may want to assert that your specification matches certain expectations to prevent regressions
to creep into your codebase (see [Tests](#tests)).

## Features

ReMap supports
* out-of-the-box mapping of primitives, built-in types, custom Java Beans and enumeration values
* type inheritance
* fields holding non-Java Bean types can be mapped using a specific type mapping.
* mapping object references to fields
* restrictive visibilities
* mapping from interface to Java Bean type
* mapping of nested collections (Attention: maps are not collections!)
* mapping of maps using `replace` and a transformation function that maps key and values
* unit testing of mapping specifications
* mapping without invasively changing code of involved objects
* overwrite fields in an instance by specifying the target instance for the mapping

## Limitations

* objects that should be mapped must meet the Java Bean convention
  * fields can have any visibility
  * fields have properly named public get/set methods
    * getter methods are mandatory for source and destination properties
    * setter methods are only mandatory for destination properties
  * fields of primitive type boolean comply with is-method convention as getter.
  * the declaring type has a public default constructor (this is only necessary for the destination object)
  * keywords like `transient` do not have an effect on the mapping
* non-static inner classes are not supported (they do not have a parameterless default constructor!)
* circular references are currently not supported
* mapping equal types does not copy object instances!
* multi-classloader environments are currently not supported. All types must be loaded by the same classloader.
* Generics cannot be used without limitations: It is possible to build a mapper for generic types, but due to the class literals used when declaring the mapping, the generic type informations gets lost.

## How to use

The short mapping shown under [Long story short](#long-story-short) uses all common operations. When a mapping becomes a little more complex the following code snippets may help.

### Implicit Mappings

ReMap performs implicit mappings if
* property name and type of source and destination fields are equal
* property name of source and destination field are equal and mappers were registered that can perform the type conversion (explained later).

Assuming `A` and `B` have the exact same number of properties with the same names and type, the following mapping configuration is enough to map all fields automatically.

__This also works for collections!__

```java
Mapper<A, B> mapper = Mapping
    .from(A.class)
    .to(B.class)
    .mapper();
```

### Mapping fields of the same type

Sometimes fields have the same type but different names. In this case use a `reassign` operation.

__This also works for collections!__

```java
public class A {
String name;
// default constructor, getter/setter, ...
}

public class B {
String lastName;
// default constructor, getter/setter, ...
}

Mapper<A, B> mapper = Mapping
    .from(A.class)
    .to(B.class)
    .reassign(A::getName)
    .to(AResource::getLastName)
    .mapper();
```

### Mapping fields with another mapper

ReMap supports the reuse of mappers. If you want to map fields for whose type conversions a mapper was already defined, you can register the mapper in the mapping configuration and use it.

__This also works for collections!__

```java
public class A {
SomeBean someBean;
// default constructor, getter/setter, ...
}

public class B {
AnotherBean someBean;
// default constructor, getter/setter, ...
}

Mapper<SomeBean, AnotherBean> someBeanToAnotherBean = Mapping
    .from(SomeBean.class)
    .to(AnotherBean.class)
    // ...
    .mapper();

Mapper<A, B> mapper = Mapping
    .from(A.class)
    .to(B.class)
    .useMapper(someBeanToAnotherBean)
    .mapper();
```

As you can see, the mapping of the fields with name `someBean` are performed automatically because the mapper supporting the required type conversion was registered.

__If the field names differ you can simply use a `reassign` operation to specify the new field name__

### Mapping fields with different names with another mapper

Assuming the field names differ but you want to map fields for whose type conversions a mapper was already defined, you can register the mapper in the mapping configuration and use it in conjunction with a `reassign` operation.

__This also works for collections!__

```java
public class A {
SomeBean someBean;
// default constructor, getter/setter, ...
}

public class B {
AnotherBean anotherBean;
// default constructor, getter/setter, ...
}

Mapper<SomeBean, AnotherBean> someBeanToAnotherBean = Mapping
    .from(SomeBean.class)
    .to(AnotherBean.class)
    // ...
    .mapper();

Mapper<A, B> mapper = Mapping
    .from(A.class)
    .to(B.class)
    .useMapper(someBeanToAnotherBean)
    .reassign(A::getSomeBean)
    .to(B::getAnotherBean)
    .mapper();
```

As you can see, you only have to configure the different field names. The type conversion is performed automatically because the required mapper was registered.


### Mapping fields with a custom mapping function

Sometimes you want to perform the mapping of fields with a custom function. ReMap supports custom mapping functions with the `replace` operation.
This operation takes two fields and performs the mapping by applying the specified function on the source field value. The result is then used as the destination field value.

The `replace` operation allows two configurations:
* `with(Function)`: applies the function, even if the source field is `null`. __As a consequence the function must implement null-checks to be null-safe.__
* `withSkipWhenNull(Function)`: only applies the function if the source field is not `null`. If the source field is `null` this field will not be mapped.

```java
public class A {
String name;
// default constructor, getter/setter, ...
}

public class B {
int nameLength;
// default constructor, getter/setter, ...
}

Mapper<A, B> mapper = Mapping
    .from(A.class)
    .to(B.class)
    .replace(A::getName, B::getNameLength)
    .withSkipWhenNull(String::length)
    .to(AResource::getLastName)
    .mapper();
```

The above example maps the `name` property in `A` to the `nameLength` property in `B`.



### Transforming collections with a custom mapping function

When performing a `replace` operation on collections in earlier versions of ReMap you had to manually iterate over the collection to apply the conversion. Since ReMap version `1.0.0` you can use the operation `replaceCollection` to apply the transformation function automatically on the collection items.

The following code snippet shows how to use `replaceCollection`:

```java
Mapper<Source, Destination> mapper = Mapping.from(Source.class)
      .to(Destination.class)
      .replaceCollection(Source::getIds, Destination::getIds)
      .with(id -> Id.builder()
        .id(id)
        .build())
      .mapper();
```

The following code asserts the above mapping:

```
AssertMapping.of(mapper)
      .expectReplaceCollection(Source::getIds, Destination::getIds)
      .andTest(id -> Id.builder()
        .id(id)
        .build())
      .ensure();
```

You can find this demo and the involved classes [here](src/test/java/com/remondis/remap/flatCollectionMapping/DemoTest.java)


### Type mappings

When mapping types that are not Java Beans you can either use the `replace` operation or you can use a type mapping. Type mappings are like the functions used for `replace` but they can be globally registered on the mapper. Type mappings act like registered mappers (see `useMapper()`): They are applied when a type conversion is required for an implicit mapping or a `reassign` mapping.

Type mappings may reduce the number of `replace` operations in your mapping configuration if the type mapping occurs very often, because the type conversion can be registered globally.

The following example shows the use of type mappings:

```
public class Person {
  private CharSequence forname;
  private CharSequence lastName;
  private List<CharSequence> addresses;
  // Getters/Setters...
}

public class Customer {
  private String forname;
  private String lastName;
  private List<String> addresses;
  // Getters/Setters...
}

Mapping.from(Person.class)
        .to(Customer.class)
        .useMapper(
             TypeMapping.from(CharSequence.class)
                        .to(String.class)
                        .applying(String::valueOf))
        .mapper();
```

In this example the mapping from `CharSequence` to `String` is defined globally for the mapping. In implicit mappings (field names are equal) or reassing operations the global mapping function defined by the type mapping is used to perform the conversion.


### Mapping with property paths

ReMap can be used to flatten object hierarchies. You can perform a mapping by specifying a get-call chain on the source type. If the get-call chain evaluates to a non-null value, the result is mapped to the destination field.

A property path is a special get-call chain: The actual get-method calls are performed by the framework. Each get-call is checked for a non-null return value. If a get returns a `null` value, the whole property path evaluates to no value and the mapping is skipped.

```java
public class Contract {
  private Company company; // Optional, may be null
  // Getters, Setters, No-Args Default Constructor...
}

public class Company {
  private List<Address> addresses; // Optional, may be null or empty
  // Getters, Setters, No-Args Default Constructor...
}

public class Address {
  String city; // Desired value to get.
  // Getters, Setters, No-Args Default Constructor...
}

public class City {
String city;
// default constructor, getter/setter, ...
}

Mapper<Contract, City> mapper = Mapping
    .from(Contract.class)
    .to(City.class)
    .replace(Contract::getCompany, City::getCity)
    .withPropertyPath(c -> c.getCompany()
        .getAddresses()
        .get(0)
        .getCity())
    .mapper();
```

The above example maps the `city` from a complex object hierarchy to the destination field `City.city`.

Keep in mind that if `Contract::getCompany` returns `null` or any other get-call in `c.getCompany().getAddresses().get(0).getCity()`, the mapping is skipped.

It is also possible to apply a transformation function to the result of a property path evaluation. The following mapping reduces the city from the above example to the string length by applying a transformation function:

```java
Mapper<Contract, City> mapper = Mapping
    .from(Contract.class)
    .to(City.class)
    .replace(Contract::getCompany, City::getCityLength)
    .withPropertyPathAnd(c -> c.getCompany()
        .getAddresses()
        .get(0)
        .getCity())
    .apply(String::length)
    .mapper();
```

The above example assumes, that the field `City.cityLength` of type `int` exists.

### Mapping other values to a field

Sometimes you want to specify another source of a value that is to be mapped to a destination field, rather than using the source field of the source mapping type. In this case you can use the `set` operation:

```java
public class A {
String name;
// default constructor, getter/setter, ...
}

public class B {
String name;
int age;
// default constructor, getter/setter, ...
}

Mapper<A, B> mapper = Mapping
    .from(A.class)
    .to(B.class)
    .set(B::getAge)
    .with(24)
    .mapper();
```

Assume you don't have a source field to map to `B.age`. In this case the `set` operation specifies the static value `24` that is used during the mapping. It is possible to provide a `java.util.function.Supplier` that produces a value, a static value or a `java.util.function.Function`.

#### Use a function with set operation

When using the `set` operation, you can specify a function that produces the value to be set on the destination field.
The function gets the reference to the source object and returns a value used for the destination field. __This mapping is useful if you need full access to the source object for your custom mapping function.__

__Do not mix up the `set`- and `replace`-operations:__ Sometimes you can write a `replace`- as a `set`-operation. As a result you often have to perform a get-call yourself instead of having ReMap handling the get-call and the optional null-checks. __Don't use set, if you can use `replace`.__

### Mapping maps

As mentioned above ReMap does not directly support the mapping of `java.util.Map`. The following example maps a map in `A` to a map of different key-value-types in `AResource`. The field `bmap` in `A` is a map that may look like this `Map<Integer, B>` while the target field `bmap` in `AResource` is a map of type `Map<String, BResource>`. For this mapping we need a function that transforms the map into another map of the specified type and a mapper to map `B` to `BResource`.

Use the following code snippet to map maps using the `replace` operation:

```java
Mapper<B, BResource> bMapper = Mapping.from(B.class)
    .to(BResource.class)
    .mapper();

 Mapper<A, AResource> mapper = Mapping.from(A.class)
    .to(AResource.class)
    // specify a replace operation involving the source and the destination field holding the map
    .replace(A::getBmap, AResource::getBmap)
        // specify a transformation function (Map<Integer, B>) -> Map<String, BResource>
        .with(iToBMap -> {
          return iToBMap.entrySet()
            .stream()
            .map(e -> {
              // Perform the type conversion while iterating over the entry set
              return new AbstractMap.SimpleEntry<String, BResource>(String.valueOf(e.getKey()),
                  bMapper.map(e.getValue()));
            })
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        })
    .useMapper(bMapper)
    .mapper();
```

### Tests

ReMap provides an easy way to assert the mapping specification for a mapper instance. These assertions should be used in unit tests to provide regression tests for your mapping configuration. The following example shows how to assert a mapping specification:

Given the following mapping...

```java
Mapper<B, BResource> bMapper = Mapping.from(B.class)
    .to(BResource.class)
    .mapper();

Mapper<A, AResource> mapper = Mapping.from(A.class)
    .to(AResource.class)
    .reassign(A::getString)
        .to(AResource::getAnotherString)
    .replace(A::getInteger, AResource::getIntegerAsString)
        .with(String::valueOf)
    .omitInSource(A::getOmitted)
    .omitInDestination(AResource::getOmitted)
    .useMapper(bMapper)
    .mapper();
```

...the following assertion can be made to ensure regression validity for the mapping specification:

```java
AssertMapping.of(mapper)
    .expectReassign(A::getString)
        .to(AResource::getAnotherString)
    .expectReplace(A::getInteger, AResource::getIntegerAsString)
        .andTest(String::valueOf)
    .expectOmitInSource(A::getOmitted)
    .expectOmitInDestination(AResource::getOmitted)
    .ensure();
```

The asserts check that the expected mappings are also configured on the specified mapper. If there are differences, the `ensure()` method will throw an assertion error.

Note: The `replace` operation supports two null-strategies and the mapper needs to specify the same strategy as the asserts! The transformation function in this example is checked against a `null` when `ensure()` is invoked. If the `replace` operation was added using `withSkipWhenNull()` the specified transformation function is not checked against `null`.

## Spring Integration

ReMap can be nicely integrated in Spring Applications so that mapper instances can be injected using `@Autowired`. Spring also checks the generic type of the mapper to autowire the correct mapping requested.

The following bean configuration creates mappers to convert a `Person` into `Human` and vice versa:

```java
@Configuration
static class TestConfiguration {
  @Bean
  BidirectionalMapper<Person, Human> bidiPersonHumanMapper(Mapper<Person, Human> personHumanMapper,
                                                           Mapper<Human, Person> humanPersonMapper) {
    return BidirectionalMapper.of(personHumanMapper, humanPersonMapper);
  }

  @Bean
  Mapper<Person, Human> personHumanMapper(){
    return Mapping.from(Person.class)
        .to(Human.class)
        .mapper();
  }
  @Bean
  Mapper<Human, Person> humanPersonMapper(){
    return Mapping.from(Human.class)
        .to(Person.class)
        .mapper();
  }
}
```

Use the following code snippet in components to inject the mapper instances:

```Java
  @Autowired
  Mapper<Person, Human> mapper1;

  @Autowired
  Mapper<Human, Person> mapper2;

`````

### Spring Boot Issue

When using the Spring Boot Framework in version <= 1.5.13.RELEASE in combination with ReMap 4.0.0 a known issue can occur:

If you get the following exception...
```
        Caused by:
        java.lang.IllegalArgumentException
            at org.objectweb.asm.ClassVisitor.<init>(Unknown Source)
            at net.sf.cglib.core.DebuggingClassWriter.<init>(DebuggingClassWriter.java:49)
            at net.sf.cglib.core.DefaultGeneratorStrategy.getClassVisitor(DefaultGeneratorStrategy.java:30)
            at net.sf.cglib.core.DefaultGeneratorStrategy.generate(DefaultGeneratorStrategy.java:24)
            at net.sf.cglib.core.AbstractClassGenerator.generate(AbstractClassGenerator.java:329)
            at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData$3.apply(AbstractClassGenerator.java:93)
            at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData$3.apply(AbstractClassGenerator.java:91)
            at net.sf.cglib.core.internal.LoadingCache$2.call(LoadingCache.java:54)
            at java.util.concurrent.FutureTask.run(FutureTask.java:266)
            at net.sf.cglib.core.internal.LoadingCache.createEntry(LoadingCache.java:61)
            at net.sf.cglib.core.internal.LoadingCache.get(LoadingCache.java:34)
            at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData.get(AbstractClassGenerator.java:116)
            at net.sf.cglib.core.AbstractClassGenerator.create(AbstractClassGenerator.java:291)
            at net.sf.cglib.core.KeyFactory$Generator.create(KeyFactory.java:221)
            at net.sf.cglib.core.KeyFactory.create(KeyFactory.java:174)
            at net.sf.cglib.core.KeyFactory.create(KeyFactory.java:153)
            at net.sf.cglib.proxy.Enhancer.<clinit>(Enhancer.java:73)
            ... 1 more
```
...you can workaround this by adding the dependency `compile "com.jayway.jsonpath:json-path:2.4.0"` to your project. This error is caused by a library that ships classes of a dependency in an incompatible version.

#### Why does this happen?

The library `net.minidev:accessors-smart:1.1` which is used as a transitive dependency requires `org.ow2.asm:asm:5.0.3`. ReMap declares `org.ow2.asm` in version 6.0 as dependecy so this newer version is chosen by dependency management of Maven or Gradle.

The problem is that this library does not only declare this dependency but ships it's own copy of the package `org.objectweb.asm` in this older version. Even if your dependecy management seems to choose `org.ow2.asm:asm:6.0`, the classes of `5.0.3` stay in the classpath.


```
+--- org.springframework.boot:spring-boot-starter-test:1.5.13.RELEASE
|    +--- com.jayway.jsonpath:json-path:2.2.
|    |    +--- net.minidev:json-smart:2.2.
|    |    |    \--- net.minidev:accessors-smart:1.
|    |    |         \--- org.ow2.asm:asm:5.0.3 -> 6.0 // Looks like 6.0 is chosen, but the classes of 5.0.3 stay in the classpath.
```

This bug was fixed in `net.minidev:accessors-smart:1.2` but is still present in Spring Boot Framework in version <= 1.5.13.RELEASE. Since `com.jayway.jsonpath:json-path:2.4.0` depends on this newer version it seems to be the safest way to overwrite the version of this library.

This workaround was tested and should work for most cases. Please file an issue if you are experiencing problems.

# Migration guide

## Migration from 3.x.x to 4.x.x

Since the source property rules relaxed a little bit, no setter methods are required for the source type of the mapping. This was done to support mapping from read-only properties. This has one drawback: In earlier version read-only properties were not recognized as properties used for the mapping. This changed now and mappings that used an earlier version of ReMap in combination with read-only properties now need an `omitInSource` specification.

## Migration from 2.x.x to 3.x.x

There were API changes that break backward compatibility:

* The method `com.remondis.remap.ReplaceCollectionAssertBuilder.andTestButSkipWhenNull(Transform<RD, RS> transformation)` changed  to `com.remondis.remap.ReplaceCollectionAssertBuilder.andSkipWhenNull()`. Specifying transform function is not longer required. Please check your mapping assertions to match the new API.

## Migration from 1.x.x to 2.x.x

There were API changes that break backward compatibility:

* The method `com.remondis.remap.ReplaceAssertBuilder.andTestButSkipWhenNull(Transform<RD, RS> transformation)` changed  to `com.remondis.remap.ReplaceAssertBuilder.andSkipWhenNull()`. Specifying transform function is not longer required. Please check your mapping assertions to match the new API.
* The generic type in `com.remondis.remap.Transform<D, S>` changed to `com.remondis.remap.Transform<S, D>`
* According to the above change please check your mapping configuration and assertions for the `replace` operation to match the new API. The affected classes are:
    * `com.remondis.remap.ReplaceBuilder`
    * `com.remondis.remap.ReplaceAssertBuilder`
    * `com.remondis.remap.ReplaceCollectionBuilder`
    * `com.remondis.remap.ReplaceCollectionAssertBuilder`
    * `com.remondis.remap.ReplaceTransformation`
    * `com.remondis.remap.Transformation`

# How to contribute
Please refer to the project's [contribution guide](CONTRIBUTE.md)