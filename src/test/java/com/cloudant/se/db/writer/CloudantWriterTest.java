package com.cloudant.se.db.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cloudant.se.Constants.WriteCode;
import com.cloudant.se.db.BaseTest;
import com.google.common.collect.Maps;

public class CloudantWriterTest extends BaseTest {
	private CloudantWriterTestImpl	writer	= null;

	@After
	public void after() {
		writer = null;
	}

	@Before
	public void before() {
		writer = new CloudantWriterTestImpl(database);
	}

	@Test
	public void testGetFromCloudant() {
		String id = "testGetFromCloudant-1";
		Map<String, Object> sent = getMap(id, false);
		testInsertExpected(sent, WriteCode.INSERT);
		try {
			testGet(sent, writer.get(id));
		} catch (IOException e) {
			fail("Should have been able to read this id");
		}
	}

	@Test
	public void testInsert() {
		testInsertExpected(getMap("testInsert-1", false), WriteCode.INSERT);
		testInsertExpected(getMap("testInsert-1", false), WriteCode.CONFLICT);
		testInsertExpected(getMap(1, false), WriteCode.EXCEPTION);
	}

	@Test
	public void testUpdate() {
		testUpdateExpected(getMap("testUpdate-1", true), WriteCode.UPDATE);
		testUpdateExpected(getMap("testUpdate-2", false), WriteCode.MISSING_REV);
	}

	@Test
	public void testUpsert() {
		testUpsertExpected(getMap("testUpsert-1", false), WriteCode.INSERT);
		testUpsertExpected(getMap("testUpsert-1", false), WriteCode.UPDATE);
	}

	private Map<String, Object> getMap(Object id, boolean withRev) {
		Map<String, Object> map = Maps.newHashMap();
		map.put("_id", id);
		map.put("x", "y");

		if (withRev) {
			map.put("_rev", "1-fjaksdlfjalksdjflkasjdl");
		}

		return map;
	}

	private void testGet(Map<String, Object> sent, Map<String, Object> received) {
		assertEquals(sent.get("_id"), received.get("_id"));
		assertEquals(sent.get("x"), received.get("x"));
		assertNotNull(received.get("_rev"));
	}

	private void testInsertExpected(Map<String, Object> map, WriteCode expected) {
		WriteCode wc = writer.insert(map);
		assertEquals(expected, wc);
	}

	private void testUpdateExpected(Map<String, Object> map, WriteCode expected) {
		WriteCode wc = writer.update(map);
		assertEquals(expected, wc);
	}

	private void testUpsertExpected(Map<String, Object> map, WriteCode expected) {
		WriteCode wc = writer.upsert(map.get("_id").toString(), map);
		assertEquals(expected, wc);
	}
}
