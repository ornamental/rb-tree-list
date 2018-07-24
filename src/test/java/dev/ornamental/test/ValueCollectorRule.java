package dev.ornamental.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This class represents a rule which may be used to report some data obtained during the test execution
 * in case the test fails. This is particularly useful for the randomized tests.<br>
 * The {@link #put(String, Object)} and {@link #remove(String)} methods are used inside a test method
 * to form a key-value mapping to be sent to {@link System#out} upon test failure. The mapping is cleared
 * after each test.
 */
public final class ValueCollectorRule implements TestRule {

	private class WrappedStatement extends Statement {

		private final Statement next;

		public WrappedStatement(Statement statement) {
			this.next = statement;
		}

		@Override
		public void evaluate() throws Throwable {
			values.clear();
			try {
				next.evaluate();
			} catch (Throwable t) {
				if (!values.isEmpty()) {
					System.out.println(title == null ? DEFAULT_TITLE : title);
					System.out.println(values.toString());
				}
				throw t;
			} finally {
				values.clear();
			}
		}
	}

	private static final String DEFAULT_TITLE = "The failed test data:";

	private final Map<String, Object> values = new HashMap<>();

	private final String title;

	public ValueCollectorRule() {
		this(null);
	}

	public ValueCollectorRule(String title) {
		this.title = title;
	}

	@Override
	public Statement apply(Statement statement, Description description) {
		return new WrappedStatement(statement);
	}

	public void put(String key, Object value) {
		values.put(key, value);
	}

	public void remove(String key) {
		values.remove(key);
	}
}
