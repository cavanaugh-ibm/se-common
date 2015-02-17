package com.cloudant.se.db.writer;

import java.io.IOException;
import java.util.Map;

import com.cloudant.client.api.Database;
import com.cloudant.se.Constants.WriteCode;
import com.cloudant.se.db.exception.StructureException;
import com.fasterxml.jackson.core.JsonProcessingException;

public class CloudantWriterTestImpl extends CloudantWriter {

	public CloudantWriterTestImpl(Database database) {
		super(database);
	}

	@Override
	public WriteCode call() throws Exception {
		return WriteCode.EXCEPTION;
	}

	@Override
	protected Map<String, Object> handleConflict(Map<String, Object> failed) throws StructureException, JsonProcessingException, IOException {
		//
		// In this base version, all we want is the latest revision number
		Map<String, Object> fromC = get((String) failed.get("_id"));
		failed.put("_rev", fromC.get("_rev"));

		return failed;
	}
}
