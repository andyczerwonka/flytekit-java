/*
 * Copyright 2021 Flyte Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.flyte.flytekit;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.google.auto.value.AutoValue;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.flyte.api.v1.Literal;
import org.flyte.api.v1.LiteralType;
import org.flyte.api.v1.Scalar;
import org.flyte.api.v1.SchemaType;
import org.flyte.api.v1.Struct;
import org.flyte.api.v1.Variable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SdkTypesTest {

  public static final String PARAM_NAME = "it";
  public static final Instant INSTANT = ZonedDateTime.now(UTC).toInstant();

  @Test
  void testNullsType() {
    SdkType<Void> sdkType = SdkTypes.nulls();

    assertNull(sdkType.fromLiteralMap(emptyMap()));
    assertEquals(emptyMap(), sdkType.toLiteralMap(null));
    assertEquals(emptyMap(), sdkType.getVariableMap());
  }

  @ParameterizedTest(name = "literal type of {0}")
  @MethodSource("testOfPrimitiveProvider")
  <T> void testOfPrimitive(
      Class<T> clazz,
      T value,
      Map<String, Literal> literalMap,
      Map<String, Variable> expectedVariableMap) {
    SdkType<T> sdkType = SdkTypes.ofPrimitive(PARAM_NAME, clazz);

    assertEquals(value, sdkType.fromLiteralMap(literalMap));
    assertEquals(literalMap, sdkType.toLiteralMap(value));
    assertEquals(expectedVariableMap, sdkType.getVariableMap());
  }

  public static Stream<Arguments> testOfPrimitiveProvider() {
    return Stream.of(
        arguments(
            Long.class, 42L, literalMap(Literals.ofInteger(42)), varMap(LiteralTypes.INTEGER)),
        arguments(Double.class, 4.0, literalMap(Literals.ofFloat(4.0)), varMap(LiteralTypes.FLOAT)),
        arguments(
            String.class, "foo", literalMap(Literals.ofString("foo")), varMap(LiteralTypes.STRING)),
        arguments(
            Boolean.class,
            true,
            literalMap(Literals.ofBoolean(true)),
            varMap(LiteralTypes.BOOLEAN)),
        arguments(
            Instant.class,
            INSTANT,
            literalMap(Literals.ofDatetime(INSTANT)),
            varMap(LiteralTypes.DATETIME)),
        arguments(
            Duration.class,
            Duration.ofHours(1),
            literalMap(Literals.ofDuration(Duration.ofHours(1))),
            varMap(LiteralTypes.DURATION)));
  }

  @ParameterizedTest(name = "literal type of {0}")
  @MethodSource("testOfCollectionProvider")
  <T> void testOfCollection(
      Class<T> clazz,
      List<T> values,
      Map<String, Literal> literalMap,
      Map<String, Variable> expectedVariableMap) {
    SdkType<List<T>> sdkType = SdkTypes.ofCollection(PARAM_NAME, clazz);

    assertEquals(values, sdkType.fromLiteralMap(literalMap));
    assertEquals(literalMap, sdkType.toLiteralMap(values));
    assertEquals(expectedVariableMap, sdkType.getVariableMap());
  }

  public static Stream<Arguments> testOfCollectionProvider() {
    return Stream.of(
        arguments(
            Long.class,
            Arrays.asList(1L, 2L, 3L),
            literalMap(
                Arrays.asList(
                    Literals.ofInteger(1L), Literals.ofInteger(2L), Literals.ofInteger(3L))),
            varMapForCollection(LiteralTypes.INTEGER)),
        arguments(
            Double.class,
            Arrays.asList(1.5, 2.5, 3.5),
            literalMap(
                Arrays.asList(Literals.ofFloat(1.5), Literals.ofFloat(2.5), Literals.ofFloat(3.5))),
            varMapForCollection(LiteralTypes.FLOAT)));
  }

  @ParameterizedTest(name = "literal type of {0}")
  @MethodSource("testOfMapProvider")
  <T> void testOfMap(
      Class<T> clazz,
      Map<String, T> values,
      Map<String, Literal> literalMap,
      Map<String, Variable> expectedVariableMap) {
    SdkType<Map<String, T>> sdkType = SdkTypes.ofMap(PARAM_NAME, clazz);

    assertEquals(values, sdkType.fromLiteralMap(literalMap));
    assertEquals(literalMap, sdkType.toLiteralMap(values));
    assertEquals(expectedVariableMap, sdkType.getVariableMap());
  }

  public static Stream<Arguments> testOfMapProvider() {
    return Stream.of(
        arguments(
            Instant.class,
            singletonMap("foo", INSTANT),
            literalMap(singletonMap("foo", Literals.ofDatetime(INSTANT))),
            varMapForMap(LiteralTypes.DATETIME)),
        arguments(
            Duration.class,
            singletonMap("foo", Duration.ofDays(1)),
            literalMap(singletonMap("foo", Literals.ofDuration(Duration.ofDays(1)))),
            varMapForMap(LiteralTypes.DURATION)));
  }

  @Test()
  void testOfStruct() {
    CustomStructSdkType sdkType = new CustomStructSdkType();
    CustomStruct customStruct =
        CustomStruct.create(123, 4.56, "789", true, INSTANT, Duration.ofMinutes(3));

    Map<String, Struct.Value> strucLiteralMapFields = new HashMap<>();
    strucLiteralMapFields.put("i", Struct.Value.ofNumberValue(123));
    strucLiteralMapFields.put("f", Struct.Value.ofNumberValue(4.56));
    strucLiteralMapFields.put("s", Struct.Value.ofStringValue("789"));
    strucLiteralMapFields.put("b", Struct.Value.ofBoolValue(true));
    strucLiteralMapFields.put("dt", Struct.Value.ofStringValue(INSTANT.toString()));
    strucLiteralMapFields.put("d", Struct.Value.ofStringValue(Duration.ofMinutes(3).toString()));
    Map<String, Literal> structLiteralMap =
        singletonMap(
            PARAM_NAME, Literal.ofScalar(Scalar.ofGeneric(Struct.of(strucLiteralMapFields))));

    List<SchemaType.Column> columns = new ArrayList<>();
    columns.add(column("i", SchemaType.ColumnType.INTEGER));
    columns.add(column("f", SchemaType.ColumnType.FLOAT));
    columns.add(column("s", SchemaType.ColumnType.STRING));
    columns.add(column("b", SchemaType.ColumnType.BOOLEAN));
    columns.add(column("dt", SchemaType.ColumnType.DATETIME));
    columns.add(column("d", SchemaType.ColumnType.DURATION));
    Map<String, Variable> structVariableMap =
        singletonMap(
            PARAM_NAME,
            variable(LiteralType.ofSchemaType(SchemaType.builder().columns(columns).build())));

    SdkType<CustomStruct> structType = SdkTypes.ofStruct(PARAM_NAME, sdkType);

    assertEquals(customStruct, structType.fromLiteralMap(structLiteralMap));
    assertEquals(structLiteralMap, structType.toLiteralMap(customStruct));
    assertEquals(structVariableMap, structType.getVariableMap());
  }

  @Test()
  void testOfStructCollection() {
    CustomStructSdkType sdkType = new CustomStructSdkType();
    CustomStruct customStruct =
        CustomStruct.create(123, 4.56, "789", true, INSTANT, Duration.ofMinutes(3));

    Map<String, Struct.Value> strucLiteralMapFields = new HashMap<>();
    strucLiteralMapFields.put("i", Struct.Value.ofNumberValue(123));
    strucLiteralMapFields.put("f", Struct.Value.ofNumberValue(4.56));
    strucLiteralMapFields.put("s", Struct.Value.ofStringValue("789"));
    strucLiteralMapFields.put("b", Struct.Value.ofBoolValue(true));
    strucLiteralMapFields.put("dt", Struct.Value.ofStringValue(INSTANT.toString()));
    strucLiteralMapFields.put("d", Struct.Value.ofStringValue(Duration.ofMinutes(3).toString()));
    Map<String, Literal> structLiteralMap =
        singletonMap(
            PARAM_NAME,
            Literal.ofCollection(
                singletonList(
                    Literal.ofScalar(Scalar.ofGeneric(Struct.of(strucLiteralMapFields))))));

    List<SchemaType.Column> columns = new ArrayList<>();
    columns.add(column("i", SchemaType.ColumnType.INTEGER));
    columns.add(column("f", SchemaType.ColumnType.FLOAT));
    columns.add(column("s", SchemaType.ColumnType.STRING));
    columns.add(column("b", SchemaType.ColumnType.BOOLEAN));
    columns.add(column("dt", SchemaType.ColumnType.DATETIME));
    columns.add(column("d", SchemaType.ColumnType.DURATION));
    Map<String, Variable> structVariableMap =
        singletonMap(
            PARAM_NAME,
            variable(
                LiteralType.ofCollectionType(
                    LiteralType.ofSchemaType(SchemaType.builder().columns(columns).build()))));

    SdkType<List<CustomStruct>> structType = SdkTypes.ofCollection(PARAM_NAME, sdkType);

    assertEquals(singletonList(customStruct), structType.fromLiteralMap(structLiteralMap));
    assertEquals(structLiteralMap, structType.toLiteralMap(singletonList(customStruct)));
    assertEquals(structVariableMap, structType.getVariableMap());
  }

  @Test()
  void testOfStructMap() {
    CustomStructSdkType sdkType = new CustomStructSdkType();
    CustomStruct customStruct =
        CustomStruct.create(123, 4.56, "789", true, INSTANT, Duration.ofMinutes(3));

    Map<String, Struct.Value> strucLiteralMapFields = new HashMap<>();
    strucLiteralMapFields.put("i", Struct.Value.ofNumberValue(123));
    strucLiteralMapFields.put("f", Struct.Value.ofNumberValue(4.56));
    strucLiteralMapFields.put("s", Struct.Value.ofStringValue("789"));
    strucLiteralMapFields.put("b", Struct.Value.ofBoolValue(true));
    strucLiteralMapFields.put("dt", Struct.Value.ofStringValue(INSTANT.toString()));
    strucLiteralMapFields.put("d", Struct.Value.ofStringValue(Duration.ofMinutes(3).toString()));
    Map<String, Literal> structLiteralMap =
        singletonMap(
            PARAM_NAME,
            Literal.ofMap(
                singletonMap(
                    "foo", Literal.ofScalar(Scalar.ofGeneric(Struct.of(strucLiteralMapFields))))));

    List<SchemaType.Column> columns = new ArrayList<>();
    columns.add(column("i", SchemaType.ColumnType.INTEGER));
    columns.add(column("f", SchemaType.ColumnType.FLOAT));
    columns.add(column("s", SchemaType.ColumnType.STRING));
    columns.add(column("b", SchemaType.ColumnType.BOOLEAN));
    columns.add(column("dt", SchemaType.ColumnType.DATETIME));
    columns.add(column("d", SchemaType.ColumnType.DURATION));
    Map<String, Variable> structVariableMap =
        singletonMap(
            PARAM_NAME,
            variable(
                LiteralType.ofMapValueType(
                    LiteralType.ofSchemaType(SchemaType.builder().columns(columns).build()))));

    SdkType<Map<String, CustomStruct>> structType = SdkTypes.ofMap(PARAM_NAME, sdkType);

    assertEquals(singletonMap("foo", customStruct), structType.fromLiteralMap(structLiteralMap));
    assertEquals(structLiteralMap, structType.toLiteralMap(singletonMap("foo", customStruct)));
    assertEquals(structVariableMap, structType.getVariableMap());
  }

  private static SchemaType.Column column(String name, SchemaType.ColumnType columnType) {
    return SchemaType.Column.builder().name(name).type(columnType).build();
  }

  @Test
  void testOfPrimitiveWithUnsupportedClass() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> SdkTypes.ofPrimitive("in", UnSupportedLiteralClass.class));

    assertEquals(
        "Type [class org.flyte.flytekit.SdkTypesTest$UnSupportedLiteralClass] is not a supported literal type",
        ex.getMessage());
  }

  @Test
  void testOfCollectionLiteralWithUnsupportedClass() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> SdkTypes.ofCollection("in", UnSupportedLiteralClass.class));

    assertEquals(
        "Type [class org.flyte.flytekit.SdkTypesTest$UnSupportedLiteralClass] is not a supported literal type",
        ex.getMessage());
  }

  @Test
  void testOfMapLiteralWithUnsupportedClass() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> SdkTypes.ofMap("in", UnSupportedLiteralClass.class));

    assertEquals(
        "Type [class org.flyte.flytekit.SdkTypesTest$UnSupportedLiteralClass] is not a supported literal type",
        ex.getMessage());
  }

  private static Map<String, Literal> literalMap(Literal value) {
    return singletonMap(PARAM_NAME, value);
  }

  private static Map<String, Literal> literalMap(List<Literal> values) {
    return singletonMap(PARAM_NAME, Literal.ofCollection(values));
  }

  private static Map<String, Literal> literalMap(Map<String, Literal> values) {
    return singletonMap(PARAM_NAME, Literal.ofMap(values));
  }

  private static Map<String, Variable> varMap(LiteralType literalType) {
    return singletonMap(PARAM_NAME, variable(literalType));
  }

  private static Map<String, Variable> varMapForCollection(LiteralType literalType) {
    return singletonMap(PARAM_NAME, variable(LiteralType.ofCollectionType(literalType)));
  }

  private static Map<String, Variable> varMapForMap(LiteralType literalType) {
    return singletonMap(PARAM_NAME, variable(LiteralType.ofMapValueType(literalType)));
  }

  private static Variable variable(LiteralType literalType) {
    return Variable.builder().literalType(literalType).build();
  }

  private static class UnSupportedLiteralClass {}

  @AutoValue
  abstract static class CustomStruct {
    abstract long i();

    abstract double f();

    abstract String s();

    abstract boolean b();

    abstract Instant dt();

    abstract Duration d();

    public static CustomStruct create(
        long i, double f, String s, boolean b, Instant dt, Duration d) {
      return new AutoValue_SdkTypesTest_CustomStruct(i, f, s, b, dt, d);
    }
  }

  // Creating a custom SdkType for CustomStruct to avoid cyclic dependency with JacksonSdkType
  private static class CustomStructSdkType extends SdkType<CustomStruct> {

    @Override
    public Map<String, Literal> toLiteralMap(CustomStruct value) {
      Map<String, Literal> literals = new LinkedHashMap<>(); // keep insertion order for debugging
      literals.put("i", Literals.ofInteger(value.i()));
      literals.put("f", Literals.ofFloat(value.f()));
      literals.put("s", Literals.ofString(value.s()));
      literals.put("b", Literals.ofBoolean(value.b()));
      literals.put("dt", Literals.ofDatetime(value.dt()));
      literals.put("d", Literals.ofDuration(value.d()));
      return literals;
    }

    @Override
    public CustomStruct fromLiteralMap(Map<String, Literal> value) {
      return CustomStruct.create(
          value.get("i").scalar().primitive().integerValue(),
          value.get("f").scalar().primitive().floatValue(),
          value.get("s").scalar().primitive().stringValue(),
          value.get("b").scalar().primitive().booleanValue(),
          value.get("dt").scalar().primitive().datetime(),
          value.get("d").scalar().primitive().duration());
    }

    @Override
    public Map<String, Variable> getVariableMap() {
      Map<String, Variable> variables = new LinkedHashMap<>(); // keep insertion order for debugging
      variables.put("i", variable(LiteralTypes.INTEGER));
      variables.put("f", variable(LiteralTypes.FLOAT));
      variables.put("s", variable(LiteralTypes.STRING));
      variables.put("b", variable(LiteralTypes.BOOLEAN));
      variables.put("dt", variable(LiteralTypes.DATETIME));
      variables.put("d", variable(LiteralTypes.DURATION));
      return variables;
    }
  }
}
