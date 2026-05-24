/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.internal.util;

import java.lang.invoke.MethodHandles;

import jakarta.validation.spi.ValidationPackageOpener;

public class PackageOpenerHelper {

	private static final Module PROVIDER_MODULE = PackageOpenerHelper.class.getModule();
	private static final Module JAKARTA_VALIDATION_MODULE = ValidationPackageOpener.class.getModule();

	private final ValidationPackageOpener packageOpener;

	public PackageOpenerHelper(ValidationPackageOpener packageOpener) {
		this.packageOpener = packageOpener;
	}

	public void openModulePackagesIfNeeded(MethodHandles.Lookup callerLookup, Class<?> targetClass) {
		Module targetModule = targetClass.getModule();
		if ( !targetModule.isNamed() || !PROVIDER_MODULE.isNamed() ) {
			return;
		}

		String packageName = targetClass.getPackageName();
		if ( targetModule.isOpen( packageName, JAKARTA_VALIDATION_MODULE ) ) {
			packageOpener.openPackage( callerLookup, targetModule, packageName );
		}
	}
}
