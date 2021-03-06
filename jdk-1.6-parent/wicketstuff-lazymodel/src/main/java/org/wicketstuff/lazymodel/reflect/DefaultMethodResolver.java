/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wicketstuff.lazymodel.reflect;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.wicket.WicketRuntimeException;

/**
 * The default resolver of {@link Method}s.
 * 
 * @author svenmeier
 */
public class DefaultMethodResolver implements IMethodResolver {

	/**
	 * Inverse operation of {@link #getId(Method)}.
	 */
	@Override
	public Method getMethod(Class<?> owner, Serializable id) {
		for (Method method : owner.getMethods()) {
			if (id.equals(getId(method))) {
				method.setAccessible(true);
				return method;
			}
		}
		throw new IllegalArgumentException(String.format(
				"unknown method %s#%s", owner.getName(), id));
	}

	/**
	 * Generates an identifier for the given method consisting of the method
	 * name and the first character of each parameter type.
	 * <p>
	 * <em>Java Bean</em> getters are abbreviated to the name of the property.
	 */
	@Override
	public Serializable getId(Method method) {
		Class<?>[] parameters = method.getParameterTypes();
		String name = method.getName();

		StringBuilder id = new StringBuilder();

		if (parameters.length == 0) {
			// possible getter
			Class<?> returnType = method.getReturnType();

			if (name.startsWith("get") && name.length() > 3
					&& Character.isUpperCase(name.charAt(3))
					&& returnType != Void.TYPE) {

				id.append(Character.toLowerCase(name.charAt(3)));
				id.append(name.substring(4));

				return id.toString();
			}

			if (name.startsWith("is")
					&& name.length() > 2
					&& Character.isUpperCase(name.charAt(2))
					&& (returnType == Boolean.TYPE || returnType == Boolean.class)) {

				id.append(Character.toLowerCase(name.charAt(2)));
				id.append(name.substring(3));

				return id.toString();
			}
		}

		id.append(method.getName());
		id.append("(");
		for (int p = 0; p < parameters.length; p++) {
			if (p > 0) {
				id.append(",");
			}
			if (parameters[p].isArray()) {
				id.append("[");
			}
			id.append(parameters[p].getSimpleName().charAt(0));
		}
		id.append(")");

		return id.toString();
	}

	/**
	 * Resolves the setter by <em>Java Beans</em> convention.
	 * <p>
	 * Setters are allowed to have arbitrary parameters, given that they match
	 * the arguments of the getter plus one additional argument matching the
	 * getter's return type.
	 */
	@Override
	public Method getSetter(Method getter) {
		String name = getter.getName();
		Class<?>[] getterParameters = getter.getParameterTypes();

		if (name.equals("get")
				&& Map.class.isAssignableFrom(getter.getDeclaringClass())) {
			name = "put";
		} else if (name.startsWith("get")) {
			name = "set" + name.substring(3);
		} else if (name.startsWith("is")) {
			name = "set" + name.substring(2);
		} else {
			throw new WicketRuntimeException(String.format(
					"no setter for %s#%s", getter.getClass(), name));
		}

		Class<?>[] setterParameters = new Class[getterParameters.length + 1];
		System.arraycopy(getterParameters, 0, setterParameters, 0,
				getterParameters.length);
		setterParameters[getterParameters.length] = getter.getReturnType();

		try {
			return getter.getDeclaringClass().getMethod(name, setterParameters);
		} catch (Exception e) {
			throw new WicketRuntimeException(String.format(
					"no setter for %s#%s", getter.getClass(), name));
		}
	}
}