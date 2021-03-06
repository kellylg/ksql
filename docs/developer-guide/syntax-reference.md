---
layout: page
title: ksqlDB Syntax Reference
tagline:  Syntax for ksqlDB queries and statements
description: Syntax Reference for statements and queries in ksqlDB
keywords: ksqldb, syntax, api
---

ksqlDB SQL has similar semantics to ANSI SQL:

-   Terminate SQL statements with a semicolon `;`.
-   Escape single-quote characters (`'`) inside string literals by using
    two successive single quotes (`''`). For example, to escape `'T'`,
    write `''T''`.

Terminology
-----------

When using ksqlDB, the following terminology is used.

### Stream

A stream is an unbounded sequence of structured data ("facts"). For
example, we could have a stream of financial transactions such as "Alice
sent $100 to Bob, then Charlie sent $50 to Bob". Facts in a stream are
immutable, which means new facts can be inserted to a stream, but
existing facts can never be updated or deleted. Streams can be created
from an {{ site.aktm }} topic or derived from an existing stream. A
stream's underlying data is durably stored (persisted) within a Kafka
topic on the Kafka brokers.

### Table

A table is a view of a stream, or another table, and represents a
collection of evolving facts. For example, we could have a table that
contains the latest financial information such as "Bob's current account
balance is $150". It is the equivalent of a traditional database table
but enriched by streaming semantics such as windowing. Facts in a table
are mutable, which means new facts can be inserted to the table, and
existing facts can be updated or deleted. Tables can be created from a
Kafka topic or derived from existing streams and tables. In both cases,
a table's underlying data is durably stored (persisted) within a Kafka
topic on the Kafka brokers.

### STRUCT

You can read nested data, in Avro, Protobuf, JSON, and JSON_SR
formats, by using the `STRUCT` type in CREATE STREAM and CREATE TABLE
statements. You can use the `STRUCT` type in these SQL statements:

-   CREATE STREAM/TABLE (from a topic)
-   CREATE STREAM/TABLE AS SELECT (from existing streams/tables)
-   SELECT (non-persistent query)

Use the following syntax to declare nested data:

```sql
STRUCT<FieldName FieldType, ...>
```

!!! note
    ksqlDB doesn't support reading nested data from CSV-formatted data.

The `STRUCT` type requires you to specify a list of fields. For each
field, you specify the field name and field type. The field type can be
any of the supported ksqlDB types, including the complex types `MAP`,
`ARRAY`, and `STRUCT`.

!!! note
		`Properties` is not a valid field name.

Here's an example CREATE STREAM statement that uses a `STRUCT` to
encapsulate a street address and a postal code:

```sql
CREATE STREAM orders (
  ID BIGINT KEY,
  address STRUCT<street VARCHAR, zip INTEGER>) WITH (...);
```

Access the fields in a `STRUCT` by using the dereference operator
(`->`):

```sql
SELECT address->city, address->zip FROM orders;
```

For more info, see [Operators](ksqldb-reference/operators.md).

You can create a `STRUCT` in a query by specifying the names of the columns
and expressions that construct the values, separated by commas. The following
example SELECT statement creates a schema that has a `STRUCT`.

```sql
SELECT STRUCT(name := col0, ageInDogYears := col1*7) AS dogs FROM animals
```

If `col0` is a string and `col1` is an integer, the resulting schema is:

```sql
col0 STRUCT<name VARCHAR, ageInDogYears INTEGER>
```

### ksqlDB Time Units

The following list shows valid time units for the SIZE, ADVANCE BY,
SESSION, and WITHIN clauses.

-   DAY, DAYS
-   HOUR, HOURS
-   MINUTE, MINUTES
-   SECOND, SECONDS
-   MILLISECOND, MILLISECONDS

For more information, see
[Windows in ksqlDB Queries](../concepts/time-and-windows-in-ksqldb-queries.md#windows-in-sql-queries).

### ksqlDB Timestamp Formats

Time-based operations, like windowing, process records according to the
timestamp in `ROWTIME`. By default, the implicit `ROWTIME` pseudo column is the
timestamp of a message in a Kafka topic. Timestamps have an accuracy of
one millisecond.

Use the TIMESTAMP property to override `ROWTIME` with the contents of
the specified column. Define the format of a record's timestamp by
using the TIMESTAMP_FORMAT property.

If you use the TIMESTAMP property but don't set TIMESTAMP_FORMAT, ksqlDB
assumes that the timestamp field is a `bigint`. If you set
TIMESTAMP_FORMAT, the TIMESTAMP field must be of type `varchar` and
have a format that the `DateTimeFormatter` Java class can parse.

If your timestamp format has embedded single quotes, you can escape them
by using two successive single quotes, `''`. For example, to escape
`'T'`, write `''T''`. The following examples show how to escape the `'`
character in SQL statements.

```sql
-- Example timestamp format: yyyy-MM-dd'T'HH:mm:ssX
CREATE STREAM TEST (ROWKEY INT KEY, ID bigint, event_timestamp VARCHAR)
  WITH (kafka_topic='test_topic',
        value_format='JSON',
        timestamp='event_timestamp',
        timestamp_format='yyyy-MM-dd''T''HH:mm:ssX');

-- Example timestamp format: yyyy.MM.dd G 'at' HH:mm:ss z
CREATE STREAM TEST (ROWKEY INT KEY, ID bigint, event_timestamp VARCHAR)
  WITH (kafka_topic='test_topic',
        value_format='JSON',
        timestamp='event_timestamp',
        timestamp_format='yyyy.MM.dd G ''at'' HH:mm:ss z');

-- Example timestamp format: hh 'o'clock' a, zzzz
CREATE STREAM TEST (ROWKEY INT KEY, ID bigint, event_timestamp VARCHAR)
  WITH (kafka_topic='test_topic',
        value_format='JSON',
        timestamp='event_timestamp',
        timestamp_format='hh ''o''clock'' a, zzzz');
```

For more information on timestamp formats, see
[DateTimeFormatter](https://cnfl.io/java-dtf).

ksqlDB CLI Commands
-----------------

The ksqlDB CLI commands can be run after
[starting the ksqlDB CLI](../operate-and-deploy/installation/installing.md#start-the-ksqldb-cli).
You can view the ksqlDB CLI help by running
`<path-to-confluent>/bin/ksql --help`.

!!! tip
      You can search and browse your command history in the ksqlDB CLI
      with `Ctrl-R`. After pressing `Ctrl-R`, start typing the command or any
      part of the command to show an auto-complete of past commands.

```
NAME
        ksql - KSQL CLI

SYNOPSIS
        ksql [ --config-file <configFile> ] [ {-h | --help} ]
                [ --output <outputFormat> ]
                [ --query-row-limit <streamedQueryRowLimit> ]
                [ --query-timeout <streamedQueryTimeoutMs> ] [--] <server>

OPTIONS
        --config-file <configFile>
            A file specifying configs for Ksql and its underlying Kafka Streams
            instance(s). Refer to KSQL documentation for a list of available
            configs.

        -h, --help
            Display help information

        --output <outputFormat>
            The output format to use (either 'JSON' or 'TABULAR'; can be changed
            during REPL as well; defaults to TABULAR)

        --query-row-limit <streamedQueryRowLimit>
            An optional maximum number of rows to read from streamed queries

            This options value must fall in the following range: value >= 1


        --query-timeout <streamedQueryTimeoutMs>
            An optional time limit (in milliseconds) for streamed queries

            This options value must fall in the following range: value >= 1


        --
            This option can be used to separate command-line options from the
            list of arguments (useful when arguments might be mistaken for
            command-line options)

        <server>
            The address of the Ksql server to connect to (ex:
            http://confluent.io:9098)

            This option may occur a maximum of 1 times
```

ksqlDB data types
---------------

ksqlDB supports the following data types.

### Primitive Types

ksqlDB supports the following primitive data types:

-   `BOOLEAN`
-   `INTEGER` or [`INT`]
-   `BIGINT`
-   `DOUBLE`
-   `VARCHAR` (or `STRING`)

### Array

`ARRAY<ElementType>`

!!! note
		The `DELIMITED` format doesn't support arrays.

ksqlDB supports fields that are arrays of another type. All the elements
in the array must be of the same type. The element type can be any valid
SQL type.

The elements of an array are zero-indexed and can be accessed by using
the `[]` operator passing in the index. For example, `SOME_ARRAY[0]`
retrieves the first element from the array. For more information, see
[Operators](ksqldb-reference/operators.md).

You can define arrays within a `CREATE TABLE` or `CREATE STREAM`
statement by using the syntax `ARRAY<ElementType>`. For example,
`ARRAY<INT>` defines an array of integers.

Also, you can output an array from a query by using a SELECT statement.
The following example creates an array from a stream named `s1`. 

```sql
SELECT ARRAY[1, 2] FROM s1 EMIT CHANGES;
```

Starting in version 0.8.0, the built-in AS_ARRAY function syntax for
creating arrays doesn't work. Replace AS_ARRAY with the ARRAY constructor
syntax. For example, replace this legacy query:

```sql
CREATE STREAM OUTPUT AS SELECT cube_explode(as_array(col1, col2)) VAL1, ABS(col3) VAL2 FROM TEST;
```

With this query:

```sql
CREATE STREAM OUTPUT AS SELECT cube_explode(array[col1, col2]) VAL1, ABS(col3) VAL2 FROM TEST;
```

### Map

`MAP<KeyType, ValueType>`

!!! note
		The `DELIMITED` format doesn't support maps.

ksqlDB supports fields that are maps. A map has a key and value type. All
of the keys must be of the same type, and all of the values must be also
be of the same type. Currently only `STRING` keys are supported. The
value type can be any valid SQL type.

Access the values of a map by using the `[]` operator and passing in the
key. For example, `SOME_MAP['cost']` retrieves the value for the entry
with key `cost`, or `null` For more information, see
[Operators](ksqldb-reference/operators.md).

You can define maps within a `CREATE TABLE` or `CREATE STREAM` statement
by using the syntax `MAP<KeyType, ValueType>`. For example,
`MAP<STRING, INT>` defines a map with string keys and integer values.

Also, you can output a map from a query by using a SELECT statement.
The following example creates a map from a stream named `s1`.

```sql
SELECT MAP(k1:=v1, k2:=v1*2) FROM s1 EMIT CHANGES;
```

### Struct

`STRUCT<FieldName FieldType, ...>`

!!! note
		The `DELIMITED` format doesn't support structs.

ksqlDB supports fields that are structs. A struct represents strongly
typed structured data. A struct is an ordered collection of named fields
that have a specific type. The field types can be any valid SQL type.

Access the fields of a struct by using the `->` operator. For example,
`SOME_STRUCT->ID` retrieves the value of the struct's `ID` field. For
more information, see [Operators](ksqldb-reference/operators.md).

You can define a structs within a `CREATE TABLE` or `CREATE STREAM`
statement by using the syntax `STRUCT<FieldName FieldType, ...>`. For
example, `STRUCT<ID BIGINT, NAME STRING, AGE INT>` defines a struct with
three fields, with the supplied name and type.

Also, you can output a struct from a query by using a SELECT statement.
The following example creates a struct from a stream named `s1`.

```sql
SELECT STRUCT(f1 := v1, f2 := v2) FROM s1 EMIT CHANGES;
```

### Decimal

`DECIMAL(Precision, Scale)`

ksqlDB supports fields that are numeric data types with fixed precision and scale:

- **Precision** is the maximum total number of decimal digits to be stored,
  including values to the left and right of the decimal point. The precision
  must be greater than 1. There is no default precision.
- **Scale** is the number of decimal digits to the right of the decimal points.
  This number must be greater than 0 and less than or equal to the value for
  `Precision`.

Mathematical operations between `DOUBLE` and `DECIMAL` cause the decimal to be
converted to a double value automatically. Converting from the decimal data type
to any floating point type (`DOUBLE`) may cause loss of precision.

### Constants

- **String constants** are enclosed in single quotation marks and may include any unicode
character (e.g. `'hello'`, `'1.2'`).
- **Integer constants** are represented by numbers that are not enclosed in quotation marks
and do not contain decimal points (e.g. `1`, `2`).
- **Decimal constants** are represented by a string of numbers that are no enclosed in quotation
marks and contain a decimal point (e.g. `1.2`, `87.`, `.94`). The type of the decimal constant
will be `DECIMAL(p, s)` where `p` is the total number of numeric characters in the string and
`s` is the total number of numeric characters that appear to the right of the decimal point.
- **Double constants** are numeric strings represented in scientific notation (e.g. `1E0`, `.42E-3`).
- **Boolean constants** are the unquoted strings that are exactly (case-insensitive) `TRUE`
or `FALSE`.

SQL statements
--------------

- SQL statements must be terminated with a semicolon (`;`).
- Statements can be spread over multiple lines.
- The hyphen character, `-`, isn't supported in names for streams,
  tables, topics, and columns.
- Don't use quotes around stream names or table names when you CREATE them.
- Use backticks around column and source names with characters that are
  unparseable by ksqlDB or when you want to control case.

Quoted identifiers for source and column names
----------------------------------------------

Quoted identifiers in column names and source names are supported. If you have
names that ksqlDB can't parse, or if you need to control the case of your
column names, enclose them in backtick characters, like this:
`` `identifier` ``.

For example, a record with the following unparseable column names is still
usable. 

```
{"@id": 42, "col.val": value}
```

Use backtick characters to reference the columns:

```sql
-- Enclose unparseable column names with backticks:
CREATE STREAM s1 (K STRING KEY, `@id` integer, `col.val` string) …
```

Also, you can use backtick characters for the names of sources, like streams
and tables. For example, you can create a stream name that has an embedded
hyphen:

```sql
CREATE STREAM `foo-bar` (id VARCHAR) WITH (kafka_topic='foo', value_format='JSON', partitions=1);
```

You can use the hyphenated stream name in SQL statements by enclosing it with
backticks:

```sql
INSERT INTO `foo-bar` (id) VALUES ('123');
CREATE STREAM `foo-too` AS SELECT * FROM `foo-bar`;

 Message
------------------------------------------------------------------------------------
 Stream foo-too created and running. Created by query with query ID: CSAS_foo-too_5
------------------------------------------------------------------------------------
```

!!! note
    By default, ksqlDB converts source and column names automatically to all
    capital letters. Use quoted identifiers to override this behavior and
    fully control your source and column names.

Key Requirements
----------------

### Message Keys

The `CREATE STREAM` and `CREATE TABLE` statements, which read data from
a Kafka topic into a stream or table, allow you to specify a
`KEY` or `PRIMARY KEY` column, respectively, to represent the data in the Kafka message key.

Example:

```sql
CREATE TABLE users (userId INT PRIMARY KEY, registertime BIGINT, gender VARCHAR, regionid VARCHAR)
  WITH (KAFKA_TOPIC='users', VALUE_FORMAT='JSON');
```

While tables require a `PRIMARY KEY`, the `KEY` column of a stream is optional.

Joins involving tables can be joined to the table on the `PRIMARY KEY` column. Joins involving
streams have no such limitation. Stream joins on any expression other than the stream's `KEY`
column require an internal repartition, but joins on the stream's `KEY` column do not.

!!! important
    {{ site.ak }} guarantees the relative order of any two messages from
    one source partition only if they are also both in the same partition
    *after* the repartition. Otherwise, {{ site.ak }} is likely to interleave
    messages. The use case will determine if these ordering guarantees are
    acceptable.

### What To Do If Your Key Is Not Set or Is In A Different Format

### Streams

For streams, just leave out the `KEY` column from the column list.
ksqlDB takes care of repartitioning the stream for you, using the
value(s) from the `GROUP BY` columns for aggregates and the join
criteria for joins.

### Tables

For tables, you can still use ksqlDB if the message key isn't set or if
it isn't in the required format, as long as the key can be rebuilt from
the value data, and *one* of the following statements is true:

-   The message key is a [unary
    function](https://en.wikipedia.org/wiki/Unary_function) of the value
    in the desired key column.
-   It's acceptable for the messages in the topic to be re-ordered before
    being inserted into the table.

First create a stream which you'll use to have ksqlDB write the message
key, and then declare the table on the output topic of this stream.

Example:

-   Goal: You want to create a table from a topic, which is keyed by
    userid of type INT.
-   Problem: The required key is present as a column (aptly named
    `userid`) in the message value, as is a string containing an integer,
    but the actual message key in {{ site.ak }} is not set or has some
    other value or format.

```sql
-- Create a stream on the original topic
CREATE STREAM users_with_wrong_key (userid INT, username VARCHAR, email VARCHAR)
  WITH (KAFKA_TOPIC='users', VALUE_FORMAT='JSON');
  
-- Derive a new stream with the required key changes.
-- 1) The CAST statement converts userId to the required SQL type.
-- 2) The PARTITION BY clause re-partitions the stream based on the new, converted key.
-- 3) The SELECT clause selects the required value columns, all in this case.
-- The resulting schema is: KSQL_COL_0 INT KEY, USERNAME STRING, EMAIL STRING.
-- Note: the system generated KSQL_COL_0 column name can be replaced via an alias in the projection,
-- but this is not necessary in this case, because this stream is not a source for other queries.
CREATE STREAM users_with_proper_key
  WITH(KAFKA_TOPIC='users-with-proper-key') AS
  SELECT *
  FROM users_with_wrong_key
  PARTITION BY CAST(userid AS BIGINT)
  EMIT CHANGES;

-- Now you can create the table on the properly keyed stream.
CREATE TABLE users_table (userId INT PRIMARY KEY, username VARCHAR, email VARCHAR)
  WITH (KAFKA_TOPIC='users-with-proper-key',
        VALUE_FORMAT='JSON');

-- Or, if you prefer, you can keep userId in the value of the repartitioned data
-- by using the AS_VALUE function.
-- The resulting schema is: userId INT KEY, USERNAME STRING, EMAIL STRING, VUSERID INT
CREATE STREAM users_with_proper_key_and_user_id
  WITH(KAFKA_TOPIC='users_with_proper_key_and_user_id') AS
  SELECT userId, username, email, AS_VALUE(userId) as vUserId
  FROM users_with_wrong_key
  PARTITION BY userid
  EMIT CHANGES;

-- Now you can create the table on the properly keyed stream.
CREATE TABLE users_table_2 (userId INT PRIMARY KEY, username VARCHAR, email VARCHAR, vUserId INT)
  WITH (KAFKA_TOPIC='users_with_proper_key_and_user_id',
        VALUE_FORMAT='JSON');
```

For more information, see
[Partition Data to Enable Joins](joins/partition-data.md).
