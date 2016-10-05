/*
 * This file is part of the Eclipse Virgo project.
 *
 * Copyright (c) 2016 ISPIN AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Daniel Marthaler - initial contribution
 */
package org.eclipse.virgo.bundlor.gradle

import org.eclipse.virgo.bundlor.gradle.internal.Configuration
import org.eclipse.virgo.bundlor.gradle.internal.GradleBundlorExecutor
import org.eclipse.virgo.bundlor.gradle.internal.support.StandardConfigurationValidator
import org.eclipse.virgo.bundlor.gradle.internal.support.StandardManifestTemplateFactory
import org.eclipse.virgo.bundlor.gradle.internal.support.StandardOsgiProfileFactory
import org.eclipse.virgo.bundlor.gradle.internal.support.StandardPropertiesSourceFactory
import org.eclipse.virgo.bundlor.support.classpath.StandardClassPathFactory
import org.eclipse.virgo.bundlor.support.manifestwriter.StandardManifestWriterFactory
import org.eclipse.virgo.bundlor.util.StringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class BundlorTask extends DefaultTask {

	@Input @Optional String bundlorVersion
	@Input @Optional Boolean failOnWarnings = true
	@InputFile @Optional File propertiesPath
	@Input @Optional String bundleManifestVersion = '2'
	@Input @Optional String bundleName = project.description
	@Input @Optional String bundleVersion = project.version
	@Input @Optional String bundleVendor = 'Eclipse Virgo Project'
	@Input @Optional String bundleSymbolicName = project.name
	@Input @Optional String manifestTemplatePath
	@Input @Optional String manifestTemplate
	@Input @Optional def importTemplate = []
	@Input @Optional def exportTemplate = []
	@Input @Optional def excludedImports = []
	@Input @Optional def excludedExports = []
	@Input File inputPath = project.sourceSets.main.output.classesDir
	@OutputDirectory File outputDir = new File("${project.buildDir}/bundlor")
	@OutputFile def manifest = new File("${outputDir}/META-INF/MANIFEST.MF")
	
	BundlorTask () {
		group = 'Build'
		description = 'Generates an OSGi-compatible MANIFEST.MF file.'
	}

	@TaskAction
	def executeBundlor() {
		createBundleNameIfRequired()
		createInlineTemplateIfRequired()
		
		def warnings = createBundlorExecutor().execute()
		
		if(warnings.size() > 0) {
			logger.warn('Bundlor Warnings:')
			warnings.each { warning ->
				logger.warn('	' + warning)
			}
			if(failOnWarnings) {
				String message = 'Bundlor returned warnings. '
				if(StringUtils.hasText(manifestTemplatePath)) {
					message += String.format("Bundlor returned warnings.  Please fix manifest template at '%s' and "
						+ "try again.", manifestTemplatePath);
				} else {
					message += 'Please fix inline manifest template and try again.'
				}				
				throw new GradleException(message)
			}
		}
	}
	
	void createBundleNameIfRequired() {
		if(!StringUtils.hasText(bundleName)) {
			bundleName = 'Generated by Bundlor'
		}
	}

	void createInlineTemplateIfRequired() {
		if(!StringUtils.hasText(manifestTemplate) && !StringUtils.hasText(manifestTemplatePath)){
			manifestTemplate = """\
				Bundle-Vendor: ${bundleVendor}
				Bundle-Version: ${bundleVersion}
				Bundle-Name: ${bundleName}
				Bundle-ManifestVersion: ${bundleManifestVersion}
				Bundle-SymbolicName: ${bundleSymbolicName}
				""".stripIndent()
		}
	}
	
	BundlorExecutor createBundlorExecutor() {
		new GradleBundlorExecutor(createConfiguration(), new StandardConfigurationValidator(),
				new StandardClassPathFactory(), new StandardManifestWriterFactory(), 
				new StandardManifestTemplateFactory(), new StandardPropertiesSourceFactory(), 
				new StandardOsgiProfileFactory())
	}
	
	Configuration createConfiguration() {
		new Configuration(inputPath: inputPath.toString(),
							outputPath: outputDir.toString(),
							manifestTemplatePath: (manifestTemplatePath != null ? project.file(manifestTemplatePath).path:''),
							manifestTemplate: manifestTemplate,
							bundleSymbolicName: bundleSymbolicName,
							defaultBundleSymbolicName: bundleSymbolicName,
							bundleVersion: bundleVersion,
							defaultBundleVersion: '1.0.0',
							propertiesPath: (propertiesPath != null ? propertiesPath.toString():''))
	}
}
