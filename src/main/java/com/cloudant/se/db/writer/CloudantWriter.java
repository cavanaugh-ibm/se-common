package com.cloudant.se.db.writer;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.lightcouch.CouchDbException;

import com.cloudant.client.api.Database;
import com.cloudant.se.Constants.WriteCode;
import com.cloudant.se.db.exception.CloudantExceptionHandler;
import com.cloudant.se.db.exception.LoadException;
import com.cloudant.se.db.exception.StructureException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

/**
 * Callable which provides the base methods to insert/update/upsert a document into a cloudant database
 *
 * @author IBM
 */
public abstract class CloudantWriter implements Callable<WriteCode> {
    private static final String   MSG_EXC     = "[id=%s] - %s - %s - %s";
    private static final String   MSG_STATUS  = "[id=%s] - %s - %s";
    private static final int      RETRY_COUNT = 30;
    protected static final Logger log         = Logger.getLogger(CloudantWriter.class);

    protected Database            database    = null;
    protected Gson                gson        = new Gson();

    public CloudantWriter(Database database) {
        this.database = database;
    }

    private Map<String, Object> _get(String id) throws JsonProcessingException, IOException {
        log.debug(format(MSG_STATUS, id, "read", "call"));
        InputStream is = database.find(id);
        Map<String, Object> map = new ObjectMapper().reader(Map.class).readValue(is);
        log.debug(format(MSG_STATUS, id, "read", "success"));

        return map;
    }

    private WriteCode _insert(Map<String, Object> map) {
        Object id = map.get("_id");
        try {
            log.debug(format(MSG_STATUS, id, "save", "call"));

            if (id == null || StringUtils.isBlank(id.toString())) {
                // Remove the magic string for generated IDs
                map.remove("_id");
            }
            database.save(map);
            return WriteCode.INSERT;
        } catch (Throwable t) {
            WriteCode code = CloudantExceptionHandler.getWriteCode(t);
            switch (code) {
                case EXCEPTION:
                    log.warn(format(MSG_EXC, id, "insert", t.getClass().getSimpleName(), code), t);
                    break;
                default:
                    log.debug(format(MSG_EXC, id, "insert", t.getClass().getSimpleName(), code));
                    break;
            }
            return code;
        }
    }

    private WriteCode _update(Map<String, Object> map) throws CouchDbException, SecurityException {
        Object id = map.get("_id");
        try {
            log.debug(format(MSG_STATUS, id, "update", "call"));

            if (id == null || StringUtils.isBlank(id.toString())) {
                // Remove the magic string for generated IDs
                map.remove("_id");
            }

            database.update(map);
            return WriteCode.UPDATE;
        } catch (Throwable t) {
            WriteCode code = CloudantExceptionHandler.getWriteCode(t);
            switch (code) {
                case EXCEPTION:
                    log.warn(format(MSG_EXC, id, "update", t.getClass().getSimpleName(), code), t);
                    break;
                default:
                    log.debug(format(MSG_EXC, id, "update", t.getClass().getSimpleName(), code));
                    break;
            }
            return code;
        }
    }

    protected Map<String, Object> get(String id) throws JsonProcessingException, IOException {
        int i = 0;
        Throwable lastT = null;
        while (i < RETRY_COUNT) {
            i++;
            try {
                return _get(id);
            } catch (Throwable t) {
                lastT = t;
                if (CloudantExceptionHandler.isTimeoutException(t)) {
                    log.debug(format(MSG_EXC, id, "get", t.getClass().getSimpleName(), WriteCode.TIMEOUT));
                    continue;
                }
            }
        }

        throw new LoadException("Unable to read " + id, lastT);
    }

    protected abstract Map<String, Object> handleConflict(Map<String, Object> failed) throws StructureException, JsonProcessingException, IOException;

    protected WriteCode insert(Map<String, Object> map) {
        int i = 0;
        while (i < RETRY_COUNT) {
            i++;
            WriteCode ic = _insert(map);
            switch (ic) {
                case TIMEOUT:
                    break; // Loop
                case CONFLICT:
                case EXCEPTION:
                case INSERT:
                case MAX_ATTEMPTS:
                case SECURITY:
                case MISSING_REV:
                case UPDATE:
                    return ic; // Exit
            }
        }

        return WriteCode.MAX_ATTEMPTS; // If we hit here it means that we hit our max retry attempts, Exit
    }

    protected WriteCode update(Map<String, Object> map) {
        int i = 0;
        while (i < RETRY_COUNT) {
            i++;
            WriteCode ic = _update(map);
            switch (ic) {
                case TIMEOUT:
                case CONFLICT:
                    try {
                        map = handleConflict(map);
                    } catch (Exception e) {
                        return WriteCode.EXCEPTION; // Exit
                    }
                    break; // Loop
                case EXCEPTION:
                case INSERT:
                case MAX_ATTEMPTS:
                case SECURITY:
                case MISSING_REV:
                case UPDATE:
                    return ic; // Exit
            }
        }

        return WriteCode.MAX_ATTEMPTS; // If we hit here it means that we hit our max retry attempts, Exit
    }

    protected WriteCode upsert(String id, Map<String, Object> map) {
        Map<String, Object> toUpsert = map;
        try {
            WriteCode insertCode = insert(toUpsert);
            switch (insertCode) {
                case INSERT:
                    //
                    // Insert worked, nothing else to do in this scenario
                    log.debug(format(MSG_STATUS, id, "insert", "succeeded"));
                    return WriteCode.INSERT;
                case CONFLICT:
                    //
                    // Conflict, get the old version, merge in our changes (adding)
                    toUpsert = handleConflict(toUpsert);
                    WriteCode updateCode = update(toUpsert);
                    switch (updateCode) {
                        case UPDATE:
                            log.debug(format(MSG_STATUS, id, "update", "succeeded"));
                            break;
                        default:
                            log.debug(format(MSG_EXC, id, "update", "failed", updateCode));
                            break;
                    }

                    return updateCode;
                default:
                    log.debug(format(MSG_EXC, id, "insert", "failed", insertCode));
                    return insertCode;
            }
        } catch (Exception e) {
            log.warn(format(MSG_EXC, id, "upsert", "failed", gson.toJson(toUpsert)), e);
            return WriteCode.EXCEPTION;
        }
    }
}
