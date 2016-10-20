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
package org.springframework.ide.vscode.properties.antlr.parser;

import java.util.ArrayList;
import java.util.BitSet;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.springframework.ide.vscode.properties.antlr.parser.JavaPropertiesParser.CommentLineContext;
import org.springframework.ide.vscode.properties.antlr.parser.JavaPropertiesParser.KeyContext;
import org.springframework.ide.vscode.properties.antlr.parser.JavaPropertiesParser.PropertyLineContext;
import org.springframework.ide.vscode.properties.antlr.parser.JavaPropertiesParser.SeparatorAndValueContext;
import org.springframework.ide.vscode.properties.parser.ParseResults;
import org.springframework.ide.vscode.properties.parser.Parser;
import org.springframework.ide.vscode.properties.parser.Problem;
import org.springframework.ide.vscode.properties.parser.ProblemCodes;
import org.springframework.ide.vscode.properties.parser.PropertiesAst;

import com.google.common.collect.ImmutableList;

/**
 * ANTLR based parser implementation
 * 
 * @author Alex Boyko
 *
 */
public class AntlrParser implements Parser {

	@Override
	public ParseResults parse(String text) {
		ArrayList<Problem> syntaxErrors = new ArrayList<>();
		ArrayList<Problem> problems = new ArrayList<>();
		ArrayList<PropertiesAst.Node> astNodes = new ArrayList<>();
		
		JavaPropertiesLexer lexer = new JavaPropertiesLexer(new ANTLRInputStream(text.toCharArray(), text.length()));
	    CommonTokenStream tokens = new CommonTokenStream(lexer);
	    JavaPropertiesParser parser = new JavaPropertiesParser(tokens);
	    
	    // Add listener to collect various parser errors
	    parser.addErrorListener(new ANTLRErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				syntaxErrors.add(createProblem(msg, ProblemCodes.PROPERTIES_SYNTAX_ERROR, (Token) offendingSymbol));
			}

			@Override
			public void reportAmbiguity(org.antlr.v4.runtime.Parser recognizer, DFA dfa, int startIndex, int stopIndex,
					boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
				problems.add(createProblem("Ambiguity detected!", ProblemCodes.PROPERTIES_AMBIGUITY_ERROR, recognizer.getCurrentToken()));
			}

			@Override
			public void reportAttemptingFullContext(org.antlr.v4.runtime.Parser recognizer, DFA dfa, int startIndex,
					int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
				problems.add(createProblem("Full-Context attempt detected!", ProblemCodes.PROPERTIES_FULL_CONTEXT_ERROR, recognizer.getCurrentToken()));
			}

			@Override
			public void reportContextSensitivity(org.antlr.v4.runtime.Parser recognizer, DFA dfa, int startIndex,
					int stopIndex, int prediction, ATNConfigSet configs) {
				problems.add(createProblem("Context sensitivity detected!", ProblemCodes.PROPERTIES_CONTEXT_SENSITIVITY_ERROR, recognizer.getCurrentToken()));
			}
	    	
	    });
	    
	    // Add listener to the parse tree to collect AST nodes
	    parser.addParseListener(new JavaPropertiesBaseListener() {
	    	
	    	private Key key = null;
	    	private Value value = null;
	    				
			@Override
			public void exitPropertyLine(PropertyLineContext ctx) {
				astNodes.add(new KeyValuePair(ctx, key, value));
				key = null;
				value = null;
			}

			@Override
			public void exitCommentLine(CommentLineContext ctx) {
				astNodes.add(new Comment(ctx));
			}

			@Override
			public void exitKey(KeyContext ctx) {
				key = new Key(ctx);
			}

			@Override
			public void exitSeparatorAndValue(SeparatorAndValueContext ctx) {
				value = new Value(ctx);
			}

		});
	    
	    parser.parse();
	    
	    // Collect and return parse results
	    return new ParseResults(new PropertiesAst(ImmutableList.copyOf(astNodes)), ImmutableList.copyOf(syntaxErrors), ImmutableList.copyOf(problems));
	}
	
	private static Problem createProblem(String message, String code, Token token) {
		return new Problem() {

			@Override
			public String getMessage() {
				return message;
			}

			@Override
			public String getCode() {
				return code;
			}

			@Override
			public int getOffset() {
				if (token.getStartIndex() >= token.getStopIndex()) {
					// No range? Make error span the whole line then
					return token.getStartIndex() - token.getCharPositionInLine();
				} else {
					return token.getStartIndex();
				}
			}

			@Override
			public int getLength() {
				if (token.getStartIndex() >= token.getStopIndex()) {
					// No range? Make error span the whole line then
					return token.getCharPositionInLine();
				} else {
					return token.getStopIndex() - token.getStartIndex();
				}
			}
			
		};
	}
	
	private static abstract class Node implements PropertiesAst.Node {
		
		abstract protected ParserRuleContext getContext();

		@Override
		public int getOffset() {
			return getContext().getStart().getStartIndex();
		}

		@Override
		public int getLength() {
			return getContext().getStop().getStartIndex() - getOffset() + 1;
		}
		
	}
	
	private static class Comment implements PropertiesAst.Comment {
		
		private CommentLineContext context;
		
		public Comment(CommentLineContext context) {
			this.context = context;
		}

		@Override
		public int getOffset() {
			return context.Comment().getSymbol().getStartIndex();
		}

		@Override
		public int getLength() {
			return context.Comment().getSymbol().getStopIndex() - getOffset() + 1;
		}
		
	}

	private static class KeyValuePair extends Node implements PropertiesAst.KeyValuePair {
		
		private PropertyLineContext context;
		private Key key;
		private Value value;
		
		public KeyValuePair(PropertyLineContext context, Key key, Value value) {
			this.context = context;
			this.key = key;
			this.value = value;
		}
		
		protected PropertyLineContext getContext() {
			return context;
		}

		@Override
		public Key getKey() {
			return key;
		}

		@Override
		public Value getValue() {
			return value;
		}
	}
	
	private static class Key extends Node implements PropertiesAst.Key {
		
		private KeyContext context;
		
		public Key(KeyContext context) {
			this.context = context;
		}
		
		protected KeyContext getContext() {
			return context;
		}
		
		@Override
		public String decode() {
			return context.getText().replace("\\:", ":").replace("\\=", "=");
		}
		
	}
	
	private static class Value extends Node implements PropertiesAst.Value {
		
		private SeparatorAndValueContext context;
		private String value;
		private String decoded;
		
		public Value(SeparatorAndValueContext context) {
			this.context = context;
			init();
		}
		
		private void init() {
			// Remove the separator, if it exists
			value = context.getText().replaceAll("^\\s*[:=]?\\s*", "");
			// Remove all escaped line breaks with trailing spaces
			decoded = value.replaceAll("\\\\(\r?\n|\r)[ \t\f]*", "");
		}

		@Override
		protected SeparatorAndValueContext getContext() {
			return context;
		}

		@Override
		public String decode() {
			return decoded;
		}

		@Override
		public int getOffset() {
			// Offset by 1 to skip the separator
			return context.getStart().getStartIndex() + (context.getText().length() - value.length());
		}

	}
	
}