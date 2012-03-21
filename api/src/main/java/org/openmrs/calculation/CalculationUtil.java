/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.calculation;

import java.lang.reflect.Method;

import org.apache.commons.lang.ArrayUtils;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.provider.CalculationProvider;

/**
 * Contains utility methods for the module
 */
public class CalculationUtil {
	
	//private static final Log log = LogFactory.getLog(CalculationUtil.class);
	
	private static final Class<?>[] PRIMITIVE_TYPES = { Boolean.class, Character.class, Byte.class, Short.class,
	        Integer.class, Float.class, Double.class, Long.class };
	
	/**
	 * Checks if the specified type is a wrapper class for a primitive type
	 * 
	 * @param clazz the class to check
	 * @return true if the class is a wrapper class for a primitive type otherwise false
	 * @should return true for primitive type wrappers classes
	 */
	public static boolean isPrimitiveWrapperType(Class<?> clazz) {
		return ArrayUtils.contains(PRIMITIVE_TYPES, clazz);
	}
	
	/**
	 * Checks if the specified class name is for a wrapper class for a primitive type
	 * 
	 * @param className the class name to check
	 * @return true if the class name is for a wrapper class for a primitive type otherwise false
	 * @should return true for primitive type wrapper class names
	 */
	public static boolean isPrimitiveWrapperClassName(String className) {
		if (className != null) {
			for (Class<?> type : PRIMITIVE_TYPES) {
				if (className.equals(type.getName()))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Utility method that constructs a Calculation instance from a provider Name, calculation name,
	 * and configuration string
	 * 
	 * @return the Calculation represented by the passed parameters
	 * @throws InvalidCalculationException if there is no valid Calculation matching the parameters
	 */
	public static Calculation getCalculation(String providerClassName, String calculationName, String configuration)
	    throws InvalidCalculationException {
		CalculationProvider calculationProvider = null;
		try {
			Class<?> providerClass = Context.loadClass(providerClassName);
			calculationProvider = (CalculationProvider) providerClass.newInstance();
		}
		catch (Exception e) {
			String msg = "Unable to instantiate CalculationProvider:" + providerClassName;
			throw new InvalidCalculationException(msg, e);
		}
		return calculationProvider.getCalculation(calculationName, configuration);
	}
	
	/**
	 * Utility method that constructs a Calculation instance from a CalculationRegistration instance
	 * 
	 * @param calculationRegistration
	 * @return the Calculation represented by the passed CalculationRegistration
	 * @throws InvalidCalculationException if the CalculationRegistration is invalid
	 */
	public static Calculation getCalculationForCalculationRegistration(CalculationRegistration r)
	    throws InvalidCalculationException {
		Calculation c = null;
		if (r != null) {
			return getCalculation(r.getProviderClassName(), r.getCalculationName(), r.getConfiguration());
		}
		return c;
	}
	
	/**
	 * Utility method that casts the specified value to the specified Type and handles conversions
	 * to primitive wrapper classes in a better/more lenient way than java's type casting. Note that
	 * the method will throw a {@link ConversionException} at runtime if it fails to convert the
	 * specified value
	 * 
	 * @see ConversionException
	 * @param valueToCast the value to be cast
	 * @param clazz the class to cast to
	 * @return a value of the specified type
	 * @should fail if the value to convert is not of a compatible type
	 * @should convert the value to the specified type if it is compatible
	 * @should return null if the passed in value is null
	 * @should convert a valid string value to Boolean
	 * @should convert a character value to Character
	 * @should convert a valid string value to Short
	 * @should convert a valid string value to Integer
	 * @should convert a valid string value to Long
	 * @should convert a valid string value to Float
	 * @should convert a valid string value to Double
	 * @should convert a valid string value to Byte
	 * @should convert a single character value to Short
	 * @should convert a valid single character value to Integer
	 * @should convert a valid single character value to Long
	 * @should convert a result with an number value in the valid range to byte
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object value, Class<T> clazz) {
		if (value == null)
			return null;
		
		if (clazz == null)
			throw new IllegalArgumentException("The class to cast to cannot be null");
		
		T castValue = null;
		// We should be able to convert any value to a String		
		if (String.class.isAssignableFrom(clazz)) {
			castValue = (T) value.toString();
		} else {
			//we should be able to convert objects that are of primitive types like String "2" to integer 2, 
			//java types casting doesn't allow this so we need to convert the value first to
			//a string
			if (CalculationUtil.isPrimitiveWrapperType(clazz)) {
				try {
					String stringValue = value.toString();
					if (Character.class.equals(clazz) && stringValue.length() == 1) {
						value = stringValue.charAt(0);
					} else {
						Method method = clazz.getMethod("valueOf", new Class<?>[] { String.class });
						value = method.invoke(null, stringValue);
					}
				}
				catch (Exception e) {
					throw new ConversionException(value, clazz);
				}
			}
			
			try {
				castValue = clazz.cast(value);
			}
			catch (ClassCastException e) {
				throw new ConversionException(value, clazz);
			}
		}
		
		return castValue;
	}
}