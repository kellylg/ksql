/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.serde.delimited;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.kafka.connect.data.Schema.OPTIONAL_INT32_SCHEMA;
import static org.apache.kafka.connect.data.SchemaBuilder.struct;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

import io.confluent.ksql.schema.ksql.PersistenceSchema;
import io.confluent.ksql.util.DecimalUtil;
import io.confluent.ksql.util.KsqlException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.apache.commons.csv.CSVFormat;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.connect.data.ConnectSchema;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KsqlDelimitedDeserializerTest {

  private static final PersistenceSchema ORDER_SCHEMA = persistenceSchema(
      SchemaBuilder.struct()
          .field("ORDERTIME", Schema.OPTIONAL_INT64_SCHEMA)
          .field("ORDERID", Schema.OPTIONAL_INT64_SCHEMA)
          .field("ITEMID", Schema.OPTIONAL_STRING_SCHEMA)
          .field("ORDERUNITS", Schema.OPTIONAL_FLOAT64_SCHEMA)
          .field("COST", DecimalUtil.builder(4, 2).build())
          .build()
  );

  private KsqlDelimitedDeserializer deserializer;

  @Before
  public void setUp() {
    deserializer = new KsqlDelimitedDeserializer(ORDER_SCHEMA, CSVFormat.DEFAULT);
  }

  @Test
  public void shouldDeserializeDelimitedCorrectly() {
    // Given:
    final byte[] bytes = "1511897796092,1,item_1,10.0,10.10\r\n".getBytes(StandardCharsets.UTF_8);

    // When:
    final Struct struct = deserializer.deserialize("", bytes);

    // Then:
    assertThat(struct.schema(), is(ORDER_SCHEMA.serializedSchema()));
    assertThat(struct.get("ORDERTIME"), is(1511897796092L));
    assertThat(struct.get("ORDERID"), is(1L));
    assertThat(struct.get("ITEMID"), is("item_1"));
    assertThat(struct.get("ORDERUNITS"), is(10.0));
    assertThat(struct.get("COST"), is(new BigDecimal("10.10")));
  }

  @Test
  public void shouldDeserializeJsonCorrectlyWithEmptyFields() {
    // Given:
    final byte[] bytes = "1511897796092,1,item_1,,\r\n".getBytes(StandardCharsets.UTF_8);

    // When:
    final Struct struct = deserializer.deserialize("", bytes);

    // Then:
    assertThat(struct.schema(), is(ORDER_SCHEMA.serializedSchema()));
    assertThat(struct.get("ORDERTIME"), is(1511897796092L));
    assertThat(struct.get("ORDERID"), is(1L));
    assertThat(struct.get("ITEMID"), is("item_1"));
    assertThat(struct.get("ORDERUNITS"), is(nullValue()));
    assertThat(struct.get("COST"), is(nullValue()));
  }

  @Test
  public void shouldThrowIfRowHasTooFewColumns() {
    // Given:
    final byte[] bytes = "1511897796092,1,item_1,\r\n".getBytes(StandardCharsets.UTF_8);

    // When:
    final Exception e = assertThrows(
        SerializationException.class,
        () -> deserializer.deserialize("", bytes)
    );

    // Then:
    assertThat(e.getCause(), (hasMessage(is("Unexpected field count, csvFields:4 schemaFields:5"))));
  }

  @Test
  public void shouldThrowIfRowHasTooMayColumns() {
    // Given:
    final byte[] bytes = "1511897796092,1,item_1,10.0,10.10,extra\r\n".getBytes(StandardCharsets.UTF_8);

    // When:
    final Exception e = assertThrows(
        SerializationException.class,
        () -> deserializer.deserialize("", bytes)
    );

    // Then:
    assertThat(e.getCause(), (hasMessage(is("Unexpected field count, csvFields:6 schemaFields:5"))));
  }

  @Test
  public void shouldThrowIfTopLevelNotStruct() {
    // Given:
    final PersistenceSchema schema = PersistenceSchema.from(
        (ConnectSchema) SchemaBuilder.struct()
            .field("f0", Schema.OPTIONAL_INT64_SCHEMA)
            .build(),
        true
    );

    // When:
    final Exception e = assertThrows(
        IllegalArgumentException.class,
        () -> new KsqlDelimitedDeserializer(schema, CSVFormat.DEFAULT.withDelimiter(','))
    );

    // Then:
    assertThat(e.getMessage(), containsString(
        "DELIMITED expects all top level schemas to be STRUCTs"));
  }

  @Test
  public void shouldDeserializedTopLevelPrimitiveTypeIfSchemaHasOnlySingleField() {
    // Given:
    final PersistenceSchema schema = persistenceSchema(
        SchemaBuilder.struct()
            .field("id", Schema.OPTIONAL_INT32_SCHEMA)
            .build()
    );

    final KsqlDelimitedDeserializer deserializer =
        createDeserializer(schema);

    final byte[] bytes = "10".getBytes(StandardCharsets.UTF_8);

    // When:
    final Struct result = deserializer.deserialize("", bytes);

    // Then:
    assertThat(result.get("id"), CoreMatchers.is(10));
  }

  @Test
  public void shouldDeserializeDecimal() {
    // Given:
    final PersistenceSchema schema = persistenceSchema(
        SchemaBuilder.struct()
            .field("cost", DecimalUtil.builder(4, 2))
            .build()
    );
    final KsqlDelimitedDeserializer deserializer =
        createDeserializer(schema);

    final byte[] bytes = "01.12".getBytes(StandardCharsets.UTF_8);

    // When:
    final Struct result = deserializer.deserialize("", bytes);

    // Then:
    assertThat(result.get("cost"), is(new BigDecimal("01.12")));
  }

  @Test
  public void shouldDeserializeDecimalWithoutLeadingZeros() {
    // Given:
    final PersistenceSchema schema = persistenceSchema(
        SchemaBuilder.struct()
            .field("cost", DecimalUtil.builder(4, 2))
            .build()
    );
    final KsqlDelimitedDeserializer deserializer =
        createDeserializer(schema);

    final byte[] bytes = "1.12".getBytes(StandardCharsets.UTF_8);

    // When:
    final Struct result = deserializer.deserialize("", bytes);

    // Then:
    assertThat(result.get("cost"), is(new BigDecimal("01.12")));
  }

  @Test
  public void shouldDeserializeDelimitedCorrectlyWithTabDelimiter() {
    shouldDeserializeDelimitedCorrectlyWithNonDefaultDelimiter('\t');
  }

  @Test
  public void shouldDeserializeDelimitedCorrectlyWithBarDelimiter() {
    shouldDeserializeDelimitedCorrectlyWithNonDefaultDelimiter('|');
  }

  private void shouldDeserializeDelimitedCorrectlyWithNonDefaultDelimiter(final char delimiter) {
    // Given:
    final byte[] bytes = "1511897796092\t1\titem_1\t10.0\t10.10\r\n".getBytes(StandardCharsets.UTF_8);

    final KsqlDelimitedDeserializer deserializer =
        new KsqlDelimitedDeserializer(ORDER_SCHEMA, CSVFormat.DEFAULT.withDelimiter('\t'));

    // When:
    final Struct struct = deserializer.deserialize("", bytes);

    // Then:
    assertThat(struct.schema(), is(ORDER_SCHEMA.serializedSchema()));
    assertThat(struct.get("ORDERTIME"), is(1511897796092L));
    assertThat(struct.get("ORDERID"), is(1L));
    assertThat(struct.get("ITEMID"), is("item_1"));
    assertThat(struct.get("ORDERUNITS"), is(10.0));
    assertThat(struct.get("COST"), is(new BigDecimal("10.10")));
  }

  @Test
  public void shouldThrowOnDeserializedTopLevelPrimitiveWhenSchemaHasMoreThanOneField() {
    // Given:
    final PersistenceSchema schema = persistenceSchema(
        struct()
            .field("id", OPTIONAL_INT32_SCHEMA)
            .field("id2", OPTIONAL_INT32_SCHEMA)
            .build()
    );

    final KsqlDelimitedDeserializer deserializer =
        createDeserializer(schema);

    final byte[] bytes = "10".getBytes(UTF_8);

    // When:
    final Exception e = assertThrows(
        SerializationException.class,
        () -> deserializer.deserialize("", bytes)
    );

    // Then:
    assertThat(e.getCause(),
        (instanceOf(KsqlException.class)));
    assertThat(e.getCause(),
        (hasMessage(CoreMatchers.is("Unexpected field count, csvFields:1 schemaFields:2"))));
  }

  @Test
  public void shouldThrowOnArrayTypes() {
    // Given:
    final PersistenceSchema schema = persistenceSchema(
        SchemaBuilder.struct()
            .field("ids", SchemaBuilder
                .array(Schema.OPTIONAL_INT32_SCHEMA)
                .optional()
                .build())
            .build()
    );

    // When:
    final Exception e = assertThrows(
        UnsupportedOperationException.class,
        () -> createDeserializer(schema)
    );

    // Then:
    assertThat(e.getMessage(), containsString("DELIMITED does not support type: ARRAY, field: ids"));
  }

  @Test
  public void shouldThrowOnMapTypes() {
    // Given:
    final PersistenceSchema schema = persistenceSchema(
        SchemaBuilder.struct()
            .field("ids", SchemaBuilder
                .map(Schema.OPTIONAL_STRING_SCHEMA, Schema.OPTIONAL_INT64_SCHEMA)
                .optional()
                .build())
            .build()
    );

    // When:
    final Exception e = assertThrows(
        UnsupportedOperationException.class,
        () -> createDeserializer(schema)
    );

    // Then:
    assertThat(e.getMessage(), containsString("DELIMITED does not support type: MAP, field: ids"));
  }

  @Test
  public void shouldThrowOnStructTypes() {
    // Given:
    final PersistenceSchema schema = persistenceSchema(
        SchemaBuilder.struct()
            .field("ids", SchemaBuilder
                .struct()
                .field("f0", Schema.OPTIONAL_INT32_SCHEMA)
                .optional()
                .build())
            .build()
    );

    // When:
    final Exception e = assertThrows(
        UnsupportedOperationException.class,
        () -> createDeserializer(schema)
    );

    // Then:
    assertThat(e.getMessage(), containsString("DELIMITED does not support type: STRUCT, field: ids"));
  }


  private static PersistenceSchema persistenceSchema(final Schema connectSchema) {
    return PersistenceSchema.from((ConnectSchema) connectSchema, false);
  }

  private static KsqlDelimitedDeserializer createDeserializer(final PersistenceSchema schema) {
    return new KsqlDelimitedDeserializer(schema, CSVFormat.DEFAULT.withDelimiter(','));
  }


}
