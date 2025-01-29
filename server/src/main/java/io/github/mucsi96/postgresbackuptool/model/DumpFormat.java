package io.github.mucsi96.postgresbackuptool.model;

public enum DumpFormat {
    PLAIN("plain"),
    CUSTOM("custom"),
    DIRECTORY("directory"),
    TAR("tar");

    private final String value;

    DumpFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
