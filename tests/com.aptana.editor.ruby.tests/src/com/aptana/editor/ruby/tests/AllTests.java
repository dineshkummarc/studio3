package com.aptana.editor.ruby.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.aptana.editor.ruby.RubySourcePartitionScannerTest;
import com.aptana.editor.ruby.RubyTokenScannerTest;

public class AllTests
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite("Test for com.aptana.editor.ruby.tests");
		// $JUnit-BEGIN$
		suite.addTestSuite(RubySourcePartitionScannerTest.class);
		suite.addTestSuite(RubyTokenScannerTest.class);
		// $JUnit-END$
		return suite;
	}
}
