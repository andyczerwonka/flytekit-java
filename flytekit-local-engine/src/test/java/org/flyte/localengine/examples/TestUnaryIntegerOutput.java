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
package org.flyte.localengine.examples;

import com.google.auto.value.AutoValue;
import java.util.Map;
import org.flyte.api.v1.Literal;
import org.flyte.api.v1.LiteralType;
import org.flyte.api.v1.Primitive;
import org.flyte.api.v1.Scalar;
import org.flyte.api.v1.SimpleType;
import org.flyte.api.v1.Variable;
import org.flyte.flytekit.SdkBindingData;

@AutoValue
public abstract class TestUnaryIntegerOutput {
  public abstract SdkBindingData<Long> o();

  public static TestUnaryIntegerOutput create(SdkBindingData<Long> o) {
    return new AutoValue_TestUnaryIntegerOutput(o);
  }

  public static class SdkType extends org.flyte.flytekit.SdkType<TestUnaryIntegerOutput> {

    private static final String VAR = "o";
    private static final LiteralType LITERAL_TYPE = LiteralType.ofSimpleType(SimpleType.INTEGER);

    @Override
    public Map<String, Literal> toLiteralMap(TestUnaryIntegerOutput value) {
      return Map.of(
          VAR, Literal.ofScalar(Scalar.ofPrimitive(Primitive.ofIntegerValue(value.o().get()))));
    }

    @Override
    public TestUnaryIntegerOutput fromLiteralMap(Map<String, Literal> value) {
      return create(SdkBindingData.ofInteger(value.get(VAR).scalar().primitive().integerValue()));
    }

    @Override
    public TestUnaryIntegerOutput promiseFor(String nodeId) {
      return create(SdkBindingData.ofOutputReference(nodeId, VAR, LITERAL_TYPE));
    }

    @Override
    public Map<String, Variable> getVariableMap() {
      return Map.of(VAR, Variable.builder().literalType(LITERAL_TYPE).build());
    }

    @Override
    public Map<String, SdkBindingData<?>> toSdkBindingMap(TestUnaryIntegerOutput value) {
      return Map.of(VAR, value.o());
    }
  }
}
