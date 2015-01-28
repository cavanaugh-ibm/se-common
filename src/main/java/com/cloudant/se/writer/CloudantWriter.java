package com.cloudant.se.writer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.lightcouch.CouchDbException;
import org.lightcouch.DocumentConflictException;

import com.cloudant.client.api.Database;
import com.cloudant.se.Constants.WriteCode;
import com.cloudant.se.db.loader.exception.StructureException;
import com.google.gson.Gson;

public abstract class CloudantWriter implements Callable<WriteCode> {
	protected static final Logger	log			= Logger.getLogger(CloudantWriter.class);

	protected Gson					gson		= new Gson();
	protected Database				database	= null;

	public CloudantWriter(Database database) {
		this.database = database;
	}

	private boolean insert(Map<String, Object> map) {
		Object id = map.get("_id");
		try {
			log.debug("[id=" + id + "] - save - remote call");
			database.save(map);
			return true;
		} catch (DocumentConflictException e) {
			log.debug("[id=" + id + "] - insert - DocumentConflictException - returning false");
			return false;
		} catch (CouchDbException e) {
			if (e.getCause() != null) {
				log.debug("[id=" + id + "] - insert - CouchDbException - " + e.getCause().getMessage());
				if (StringUtils.contains(e.getCause().getMessage(), "Connection timed out: connect")) {
					log.debug("[id=" + id + "] - insert - CouchDbException - timeout - returning false");
					return false;
				}
			}

			if (e.getMessage().contains("\"error\":\"forbidden\",\"reason\":\"_writer access is required for this request\"")) {
				log.fatal("Cloudant account does not have writer permissions - exiting");
				System.exit(1);
			}

			throw e;
		}
	}

	private boolean update(Map<String, Object> map) {
		Object id = map.get("_id");
		try {
			log.debug("[id=" + id + "] - update - remote call");
			database.update(map);
			return true;
		} catch (DocumentConflictException e) {
			log.debug("[id=" + id + "] - update - DocumentConflictException - returning false");
			return false;
		} catch (CouchDbException e) {
			if (e.getCause() != null) {
				log.debug("[id=" + id + "] - update - CouchDbException - " + e.getCause().getMessage());
				if (StringUtils.contains(e.getCause().getMessage(), "Connection timed out: connect")) {
					log.debug("[id=" + id + "] - update - CouchDbException - timeout - returning false");
					return false;
				}
			}

			throw e;
		}
	}

	protected Map<String, Object> getFromCloudant(String id) throws JsonProcessingException, IOException {
		log.debug("[id=" + id + "] - read - call");
		InputStream is = database.find(id);
		Map<String, Object> map = new ObjectMapper().reader(Map.class).readValue(is);
		log.debug("[id=" + id + "] - read - success");

		return map;
	}

	protected abstract Map<String, Object> handleConflict(Map<String, Object> failed) throws StructureException, JsonProcessingException, IOException;

	protected WriteCode upsert(String id, Map<String, Object> map) {
		Map<String, Object> toUpsert = map;
		try {
			if (insert(toUpsert)) {
				//
				// Insert worked, nothing else to do in this scenario
				log.debug("[id=" + id + "] - insert - succeeded");
				return WriteCode.INSERT;
			} else {
				//
				// Conflict, get the old version, merge in our changes (adding)
				int i = 0;
				while (i < 30) {
					i++;
					toUpsert = handleConflict(toUpsert);

					if (update(toUpsert)) {
						log.debug("[id=" + id + "] - update - succeeded");
						return WriteCode.UPDATE;
					} else {
						continue;
					}
				}

				//
				// If we get to here it means we passed the max attempts - log that we did not write the message
				log.warn("[id=" + id + "] - Unable to upsert a document after " + 30 + " attempts - [" + gson.toJson(toUpsert) + "]");
				return WriteCode.MAX_ATTEMPTS;
			}
		} catch (Exception e) {
			log.warn("[id=" + id + "] - Unable to upsert a document due to exception - [" + gson.toJson(toUpsert) + "]", e);
			return WriteCode.EXCEPTION;
		}
	}
}
