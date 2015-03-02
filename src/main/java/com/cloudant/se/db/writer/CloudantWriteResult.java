package com.cloudant.se.db.writer;

import com.cloudant.se.Constants.WriteCode;

public class CloudantWriteResult {
    private final String    id;

    private final String    rev;

    private final Throwable t;

    private final WriteCode writeCode;

    public CloudantWriteResult(WriteCode writeCode, String id, String rev, Throwable t) {
        this.writeCode = writeCode;
        this.id = id;
        this.rev = rev;
        this.t = t;
    }

    public String getId() {
        return id;
    }

    public String getRev() {
        return rev;
    }

    public Throwable getT() {
        return t;
    }

    public WriteCode getWriteCode() {
        return writeCode;
    }

    public static CloudantWriteResult insertResult(String id, String rev) {
        return new CloudantWriteResult(WriteCode.INSERT, id, rev, null);
    }

    public static CloudantWriteResult updateResult(String id, String rev) {
        return new CloudantWriteResult(WriteCode.UPDATE, id, rev, null);
    }

    public static CloudantWriteResult errorResult(WriteCode code, Throwable t) {
        return new CloudantWriteResult(code, null, null, t);
    }
}
