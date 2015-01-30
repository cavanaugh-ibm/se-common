package com.cloudant.se;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.cloudant.se.db.writer.CloudantWriterTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		CloudantWriterTest.class
})
public class SeCommonTestSuite {
}
