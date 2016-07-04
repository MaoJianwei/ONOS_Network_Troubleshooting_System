/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.fnl.impl;

/**
 * Used as the parameter of invoked methods, treated as a extra return value the methods.
 *
 * At present, used in Loop Checking's matchAndAddFlowEntry()
 *
 * @param <M> the class of expected return value.
 */
public class TsReturn<M> {
    private M ret;

    /**
     * Mainly used in the invoked methods, to set value.
     * @param value The value to set.
     */
    public void setValue(M value) {
        ret = value;
    }

    /**
     * Mainly used in the invoker, after the invoked methods return, to get value.
     * @return The value
     */
    public M getValue() {
        return ret;
    }

    /**
     * Check whether the value is null or not.
     * Generally, if setValue() has not been invoked, the ret is null.
     * @return {@code true} if ret is null;
     *         {@code false} otherwise.
     */
    public boolean isNull() {
        return ret == null;
    }
}
