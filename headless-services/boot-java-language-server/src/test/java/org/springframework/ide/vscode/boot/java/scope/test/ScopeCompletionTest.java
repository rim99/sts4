/*******************************************************************************
 * Copyright (c) 2017 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.scope.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ide.vscode.commons.java.IJavaProject;
import org.springframework.ide.vscode.commons.util.text.LanguageId;
import org.springframework.ide.vscode.languageserver.testharness.Editor;
import org.springframework.ide.vscode.project.harness.BootLanguageServerHarness;
import org.springframework.ide.vscode.project.harness.ProjectsHarness;

/**
 * @author Martin Lippert
 */
public class ScopeCompletionTest {

	private BootLanguageServerHarness harness;
	private Editor editor;

	@Before
	public void setup() throws Exception {
		IJavaProject testProject = ProjectsHarness.INSTANCE.mavenProject("test-annotations");
		harness = BootLanguageServerHarness.builder().mockDefaults().build();
		harness.useProject(testProject);
		harness.intialize(null);
	}

//	private IJavaProject getTestProject() {
//		return testProject;
//	}

	@Test
	public void testEmptyBracketsCompletion() throws Exception {
		prepareCase("@Scope(\"onClass\")", "@Scope(<*>)");
		assertAnnotationCompletions(
				"@Scope(\"prototype\"<*>)",
				"@Scope(\"singleton\"<*>)",
				"@Scope(\"request\"<*>)",
				"@Scope(\"session\"<*>)",
				"@Scope(\"globalSession\"<*>)",
				"@Scope(\"application\"<*>)",
				"@Scope(\"websocket\"<*>)");
	}

	@Test
	public void testEmptyStringLiteralCompletion() throws Exception {
		prepareCase("@Scope(\"onClass\")", "@Scope(\"<*>\")");
		assertAnnotationCompletions(
				"@Scope(\"prototype\"<*>)",
				"@Scope(\"singleton\"<*>)",
				"@Scope(\"request\"<*>)",
				"@Scope(\"session\"<*>)",
				"@Scope(\"globalSession\"<*>)",
				"@Scope(\"application\"<*>)",
				"@Scope(\"websocket\"<*>)");
	}

	@Test
	public void testEmptyValueCompletion() throws Exception {
		prepareCase("@Scope(\"onClass\")", "@Scope(value=<*>)");
		assertAnnotationCompletions(
				"@Scope(value=\"prototype\"<*>)",
				"@Scope(value=\"singleton\"<*>)",
				"@Scope(value=\"request\"<*>)",
				"@Scope(value=\"session\"<*>)",
				"@Scope(value=\"globalSession\"<*>)",
				"@Scope(value=\"application\"<*>)",
				"@Scope(value=\"websocket\"<*>)");
	}

	@Test
	public void testEmptyValueStringLiteralCompletion() throws Exception {
		prepareCase("@Scope(\"onClass\")", "@Scope(value=\"<*>\")");
		assertAnnotationCompletions(
				"@Scope(value=\"prototype\"<*>)",
				"@Scope(value=\"singleton\"<*>)",
				"@Scope(value=\"request\"<*>)",
				"@Scope(value=\"session\"<*>)",
				"@Scope(value=\"globalSession\"<*>)",
				"@Scope(value=\"application\"<*>)",
				"@Scope(value=\"websocket\"<*>)");
	}

	@Test
	public void testPrefixWithClosingQuotesCompletion() throws Exception {
		prepareCase("@Scope(\"onClass\")", "@Scope(\"pro<*>\")");
		assertAnnotationCompletions(
				"@Scope(\"prototype\"<*>)");
	}

	@Test
	public void testPrefixWithoutClosingQuotesCompletion() throws Exception {
		prepareCase("@Scope(\"onClass\")", "@Scope(\"pro<*>)");
		assertAnnotationCompletions();
	}

	@Test
	public void testValuePrefixWithClosingQuotesCompletion() throws Exception {
		prepareCase("@Scope(\"onClass\")", "@Scope(value=\"pro<*>\")");
		assertAnnotationCompletions(
				"@Scope(value=\"prototype\"<*>)");
	}

	@Test
	public void testValuePrefixWithoutClosingQuotesCompletion() throws Exception {
		prepareCase("@Scope(\"onClass\")", "@Scope(value=\"pro<*>)");
		assertAnnotationCompletions();
	}

	@Test
	public void testPrefixReplaceRestCompletion() throws Exception {
		prepareCase("@Scope(\"onClass\")", "@Scope(\"pro<*>something\")");
		assertAnnotationCompletions(
				"@Scope(\"prototype\"<*>)");
	}

	@Test
	public void testDifferentMemberNameCompletion() throws Exception {
		prepareCase("@Scope(\"onClass\")", "@Scope(proxyName=\"<*>\")");
		assertAnnotationCompletions();
	}

	private void prepareCase(String selectedAnnotation, String annotationStatementBeforeTest) throws Exception {
		InputStream resource = this.getClass().getResourceAsStream("/test-projects/test-annotations/src/main/java/org/test/TestScopeCompletion.java");
		String content = IOUtils.toString(resource);

		content = content.replace(selectedAnnotation, annotationStatementBeforeTest);
		editor = new Editor(harness, content, LanguageId.JAVA);
	}

	private void assertAnnotationCompletions(String... completedAnnotations) throws Exception {
		List<CompletionItem> completions = editor.getCompletions();
		int i = 0;
		for (String expectedCompleted : completedAnnotations) {
			Editor clonedEditor = editor.clone();
			clonedEditor.apply(completions.get(i++));
			assertTrue(clonedEditor.getText().contains(expectedCompleted));
		}

		assertEquals(i, completions.size());
	}


}
