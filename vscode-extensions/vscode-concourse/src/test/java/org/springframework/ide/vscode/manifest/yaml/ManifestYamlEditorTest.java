/*******************************************************************************
 * Copyright (c) 2016 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.manifest.yaml;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ide.vscode.concourse.ConcourseLanguageServer;
import org.springframework.ide.vscode.languageserver.testharness.Editor;
import org.springframework.ide.vscode.languageserver.testharness.LanguageServerHarness;

public class ManifestYamlEditorTest {

	LanguageServerHarness harness;

	@Before public void setup() throws Exception {
		harness = new LanguageServerHarness(ConcourseLanguageServer::new);
		harness.intialize(null);
	}

	@Test public void testReconcileCatchesParseError() throws Exception {

		Editor editor = harness.newEditor(
				"somemap: val\n"+
				"- sequence"
		);
		editor.assertProblems(
				"-|expected <block end>"
		);
	}

	@Test public void reconcileRunsOnDocumentOpenAndChange() throws Exception {
		LanguageServerHarness harness = new LanguageServerHarness(ConcourseLanguageServer::new);
		harness.intialize(null);

		Editor editor = harness.newEditor(
				"somemap: val\n"+
				"- sequence"
		);

		editor.assertProblems(
				"-|expected <block end>"
		);

		editor.setText(
				"- sequence\n" +
				"zomemap: val"
		);

		editor.assertProblems(
				"z|expected <block end>"
		);
	}

	@Test
	public void reconcileMisSpelledPropertyNames() throws Exception {
		Editor editor;

		editor = harness.newEditor(
				"memory: 1G\n" +
				"aplications:\n" +
				"  - buildpack: zbuildpack\n" +
				"    domain: zdomain\n" +
				"    name: foo"
		);
		editor.assertProblems("aplications|Unknown property");

		//mispelled or not allowed at toplevel
		editor = harness.newEditor(
				"name: foo\n" +
				"buildpeck: yahah\n" +
				"memory: 1G\n" +
				"memori: 1G\n"
		);
		editor.assertProblems(
				"name|Unknown property",
				"buildpeck|Unknown property",
				"memori|Unknown property"
		);

		//mispelled or not allowed as nested
		editor = harness.newEditor(
				"applications:\n" +
				"- name: fine\n" +
				"  buildpeck: yahah\n" +
				"  memory: 1G\n" +
				"  memori: 1G\n" +
				"  applications: bad\n"
		);
		editor.assertProblems(
				"buildpeck|Unknown property",
				"memori|Unknown property",
				"applications|Unknown property"
		);
	}

	@Test
	public void reconcileStructuralProblems() throws Exception {
		Editor editor;

		//forgot the 'applications:' heading
		editor = harness.newEditor(
				"- name: foo"
		);
		editor.assertProblems(
				"- name: foo|Expecting a 'Map' but found a 'Sequence'"
		);

		//forgot to make the '-' after applications
		editor = harness.newEditor(
				"applications:\n" +
				"  name: foo"
		);
		editor.assertProblems(
				"name: foo|Expecting a 'Sequence' but found a 'Map'"
		);

		//Using a 'composite' element where a scalar type is expected
		editor = harness.newEditor(
				"memory:\n"+
				"- bad sequence\n" +
				"buildpack:\n" +
				"  bad: map\n"
		);
		editor.assertProblems(
				"- bad sequence|Expecting a 'Memory' but found a 'Sequence'",
				"bad: map|Expecting a 'Buildpack' but found a 'Map'"
		);
	}

	@Test
	public void reconcileSimpleTypes() throws Exception {
		Editor editor;

		//check for 'format' errors:
		editor = harness.newEditor(
				"applications:\n" +
				"- name: foo\n" +
				"  instances: not a number\n" +
				"  no-route: notBool\n"+
				"  memory: 1024\n" +
				"  disk_quota: 2048\n" +
				"  health-check-type: unhealthy"
		);
		editor.assertProblems(
				"not a number|Positive Integer",
				"notBool|boolean",
				"1024|Memory",
				"2048|Memory",
				"unhealthy|Health Check Type"
		);

		//check for 'range' errors:
		editor = harness.newEditor(
				"applications:\n" +
				"- name: foo\n" +
				"  instances: -3\n" +
				"  memory: -1024M\n" +
				"  disk_quota: -2048M\n"
		);
		editor.assertProblems(
				"-3|Positive Integer",
				"-1024M|Memory",
				"-2048M|Memory"
		);

		//check that correct values are indeed accepted
		editor = harness.newEditor(
				"applications:\n" +
				"- name: foo\n" +
				"  instances: 2\n" +
				"  no-route: true\n"+
				"  memory: 1024M\n" +
				"  disk_quota: 2048MB\n"
		);
		editor.assertProblems(/*none*/);

		//check that correct values are indeed accepted
		editor = harness.newEditor(
				"applications:\n" +
				"- name: foo\n" +
				"  instances: 2\n" +
				"  no-route: false\n" +
				"  memory: 1024m\n" +
				"  disk_quota: 2048mb\n"
		);
		editor.assertProblems(/*none*/);

		editor = harness.newEditor(
				"applications:\n" +
				"- name: foo\n" +
				"  instances: 2\n" +
				"  memory: 1G\n" +
				"  disk_quota: 2g\n"
		);
		editor.assertProblems(/*none*/);
	}

	@Test
	public void noListIndent() throws Exception {
		Editor editor;
		editor = harness.newEditor("appl<*>");
		editor.assertCompletions(
				"applications:\n"+
				"- <*>"
		);
	}

	@Test
	public void toplevelCompletions() throws Exception {
		Editor editor;
		editor = harness.newEditor("<*>");
		editor.assertCompletions(
				"applications:\n"+
				"- <*>",
				// ---------------
				"buildpack: <*>",
				// ---------------
				"command: <*>",
				// ---------------
				"disk_quota: <*>",
				// ---------------
				"domain: <*>",
				// ---------------
				"domains:\n"+
				"- <*>",
				// ---------------
				"env:\n"+
				"  <*>",
				// ---------------
				"health-check-type: <*>",
				// ---------------
//				"host: <*>",
				// ---------------
//				"hosts: \n"+
//				"  - <*>",
				// ---------------
				"inherit: <*>",
				// ---------------
				"instances: <*>",
				// ---------------
				"memory: <*>",
				// ---------------
//				"name: <*>",
				// ---------------
				"no-hostname: <*>",
				// ---------------
				"no-route: <*>",
				// ---------------
				"path: <*>",
				// ---------------
				"random-route: <*>",
				// ---------------
				"services:\n"+
				"- <*>",
				// ---------------
				"stack: <*>",
				// ---------------
				"timeout: <*>"
		);

		editor = harness.newEditor("ranro<*>");
		editor.assertCompletions(
				"random-route: <*>"
		);
	}

	@Test
	public void nestedCompletions() throws Exception {
		Editor editor;
		editor = harness.newEditor(
				"applications:\n" +
				"- <*>"
		);
		editor.assertCompletions(
				// ---------------
				"applications:\n" +
				"- buildpack: <*>",
				// ---------------
				"applications:\n" +
				"- command: <*>",
				// ---------------
				"applications:\n" +
				"- disk_quota: <*>",
				// ---------------
				"applications:\n" +
				"- domain: <*>",
				// ---------------
				"applications:\n" +
				"- domains:\n"+
				"  - <*>",
				// ---------------
				"applications:\n" +
				"- env:\n"+
				"    <*>",
				// ---------------
				"applications:\n" +
				"- health-check-type: <*>",
				// ---------------
				"applications:\n" +
				"- host: <*>",
				// ---------------
				"applications:\n" +
				"- hosts:\n"+
				"  - <*>",
				// ---------------
				"applications:\n" +
				"- instances: <*>",
				// ---------------
				"applications:\n" +
				"- memory: <*>",
				// ---------------
				"applications:\n" +
				"- name: <*>",
				// ---------------
				"applications:\n" +
				"- no-hostname: <*>",
				// ---------------
				"applications:\n" +
				"- no-route: <*>",
				// ---------------
				"applications:\n" +
				"- path: <*>",
				// ---------------
				"applications:\n" +
				"- random-route: <*>",
				// ---------------
				"applications:\n" +
				"- services:\n"+
				"  - <*>",
				// ---------------
				"applications:\n" +
				"- stack: <*>",
				// ---------------
				"applications:\n" +
				"- timeout: <*>"
		);
	}

	@Test
	public void completionDetailsAndDocs() throws Exception {
		Editor editor = harness.newEditor(
				"applications:\n" +
				"- build<*>"
		);
		editor.assertCompletionDetails("buildpack", "Buildpack", "If your application requires a custom buildpack");
	}

	@Test
	public void valueCompletions() throws Exception {
		assertCompletions("disk_quota: <*>",
				"disk_quota: 1024M<*>",
				"disk_quota: 256M<*>",
				"disk_quota: 512M<*>"
		);
		assertCompletions("memory: <*>",
				"memory: 1024M<*>",
				"memory: 256M<*>",
				"memory: 512M<*>"
		);
		assertCompletions("no-hostname: <*>",
				"no-hostname: false<*>",
				"no-hostname: true<*>"
		);
		assertCompletions("no-route: <*>",
				"no-route: false<*>",
				"no-route: true<*>"
		);
		assertCompletions("random-route: <*>",
				"random-route: false<*>",
				"random-route: true<*>"
		);
		
		assertCompletions("health-check-type: <*>",
				"health-check-type: none<*>",
				"health-check-type: port<*>"
		);
	}

	@Test
	public void hoverInfos() throws Exception {
		Editor editor = harness.newEditor(
			"memory: 1G\n" +
			"#comment\n" +
			"inherit: base-manifest.yml\n"+
			"applications:\n" +
			"- buildpack: zbuildpack\n" +
			"  domain: zdomain\n" +
			"  name: foo\n" + 
			"  command: java main.java\n" +
			"  disk_quota: 1024M\n" +
			"  domains:\n" +
			"  - pivotal.io\n" +
			"  - otherdomain.org\n" +
			"  env:\n" +
			"    RAILS_ENV: production\n" +
			"    RACK_ENV: production\n" +
			"  host: apppage\n" +
			"  hosts:\n" +
			"  - apppage2\n" +
			"  - appage3\n" + 
			"  instances: 2\n" +
			"  no-hostname: true\n" + 
			"  no-route: true\n" + 
			"  path: somepath/app.jar\n" +
			"  random-route: true\n" +
			"  services:\n" +
			"  - instance_ABC\n" +
			"  - instance_XYZ\n" +
			"  stack: cflinuxfs2\n" +
			"  timeout: 80\n" +
			"  health-check-type: none\n"
		);
		
		editor.assertIsHoverRegion("memory");
		editor.assertIsHoverRegion("inherit");
		editor.assertIsHoverRegion("applications");
		editor.assertIsHoverRegion("buildpack");
		editor.assertIsHoverRegion("domain");
		editor.assertIsHoverRegion("name");
		editor.assertIsHoverRegion("command");
		editor.assertIsHoverRegion("disk_quota");
		editor.assertIsHoverRegion("domains");
		editor.assertIsHoverRegion("env");
		editor.assertIsHoverRegion("host");
		editor.assertIsHoverRegion("hosts");
		editor.assertIsHoverRegion("instances");
		editor.assertIsHoverRegion("no-hostname");
		editor.assertIsHoverRegion("no-route");
		editor.assertIsHoverRegion("path");
		editor.assertIsHoverRegion("random-route");
		editor.assertIsHoverRegion("services");
		editor.assertIsHoverRegion("stack");
		editor.assertIsHoverRegion("timeout");
		editor.assertIsHoverRegion("health-check-type");

		editor.assertHoverContains("memory", "Use the `memory` attribute to specify the memory limit");
		editor.assertHoverContains("1G", "Use the `memory` attribute to specify the memory limit");
		editor.assertHoverContains("inherit", "For example, every child of a parent manifest called `base-manifest.yml` begins like this");
		editor.assertHoverContains("buildpack", "use the `buildpack` attribute to specify its URL or name");
	    editor.assertHoverContains("name", "The `name` attribute is the only required attribute for an application in a manifest file");
	    editor.assertHoverContains("command", "On the command line, use the `-c` option to specify the custom start command as the following example shows");
	    editor.assertHoverContains("disk_quota", "Use the `disk_quota` attribute to allocate the disk space for your app instance");
	    editor.assertHoverContains("domain", "You can use the `domain` attribute when you want your application to be served");
	    editor.assertHoverContains("domains", "Use the `domains` attribute to provide multiple domains");
	    editor.assertHoverContains("env", "The `env` block consists of a heading, then one or more environment variable/value pairs");
	    editor.assertHoverContains("host", "Use the `host` attribute to provide a hostname, or subdomain, in the form of a string");
	    editor.assertHoverContains("hosts", "Use the `hosts` attribute to provide multiple hostnames, or subdomains");
	    editor.assertHoverContains("instances", "Use the `instances` attribute to specify the number of app instances that you want to start upon push");
	    editor.assertHoverContains("no-hostname", "By default, if you do not provide a hostname, the URL for the app takes the form of `APP-NAME.DOMAIN`");
	    editor.assertHoverContains("no-route", "You can use the `no-route` attribute with a value of `true` to prevent a route from being created for your application");
	    editor.assertHoverContains("path", "You can use the `path` attribute to tell Cloud Foundry where to find your application");
	    editor.assertHoverContains("random-route", "Use the `random-route` attribute to create a URL that includes the app name and random words");
	    editor.assertHoverContains("services", "The `services` block consists of a heading, then one or more service instance names");
	    editor.assertHoverContains("stack", "Use the `stack` attribute to specify which stack to deploy your application to.");
	    editor.assertHoverContains("timeout", "The `timeout` attribute defines the number of seconds Cloud Foundry allocates for starting your application");
	    editor.assertHoverContains("health-check-type", "Use the `health-check-type` attribute to");
	}
	
	@Test
	public void noHoverInfos() throws Exception {
		Editor editor = harness.newEditor(
		    "#comment\n" +
			"applications:\n" +
			"- buildpack: zbuildpack\n" +
			"  name: foo\n" + 
			"  domains:\n" +
			"  - pivotal.io\n" +
			"  - otherdomain.org\n"

		);
		editor.assertNoHover("comment");
		
		// May fail in the future if hover support is added, but if hover support is added in the future,
		// it is expected that these should start to fail, as right now they have no hover
		editor.assertNoHover("pivotal.io");
		editor.assertNoHover("otherdomain.org");
	}

	//////////////////////////////////////////////////////////////////////////////

	private void assertCompletions(String textBefore, String... textAfter) throws Exception {
		Editor editor = harness.newEditor(textBefore);
		editor.assertCompletions(textAfter);
	}
}