package io.github.mucsi96.postgresbackuptool.model;

public enum BackupType {
    PLAIN("PLAIN"),
    ARCHIVE("ARCHIVE");

    private final String value;

    BackupType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
