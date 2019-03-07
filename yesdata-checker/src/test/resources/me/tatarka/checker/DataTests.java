package me.tatarka.checker;

import me.tatarka.yesdata.annotation.Data;
import java.util.Objects;

// BUG: Diagnostic contains: [Yesdata] Class does not implement equals, hashCode, and toString.
@Data class NoMethods {}

// BUG: Diagnostic contains: [Yesdata] Class does not implement equals.
@Data class NoEquals {
    @Override public int hashCode() {
        return 0;
    }

    @Override public String toString() {
        return "NoFields{}";
    }
}

// BUG: Diagnostic contains: [Yesdata] Class does not implement hashCode.
@Data class NoHashCode {
    @Override public boolean equals(Object obj) {
        return obj instanceof NoFields;
    }

    @Override public String toString() {
        return "NoFields{}";
    }
}

// BUG: Diagnostic contains: [Yesdata] Class does not implement toString.
@Data class NoToString {
    @Override public boolean equals(Object obj) {
        return obj instanceof NoFields;
    }

    @Override public int hashCode() {
        return 0;
    }
}

@Data final class NoFields {
    @Override public boolean equals(Object obj) {
        return obj instanceof NoFields;
    }

    @Override public int hashCode() {
        return 0;
    }

    @Override public String toString() {
        return "NoFields{}";
    }
}

@Data final class OneField {
    public final String field;

    public OneField(String field) {
        this.field = field;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MissingFieldInEquals oneField = (MissingFieldInEquals) o;
        return Objects.equals(field, oneField.field);
    }

    @Override public int hashCode() {
        return Objects.hash(field);
    }

    @Override public String toString() {
        return "OneField{" +
                "field='" + field + '\'' +
                '}';
    }
}

@Data final class MissingFieldInEquals {
    public final String field;

    public MissingFieldInEquals(String field) {
        this.field = field;
    }

    // BUG: Diagnostic contains: [Yesdata] Missing field 'field' in equals implementation.
    @Override public boolean equals(Object o) {
        return true;
    }

    @Override public int hashCode() {
        return Objects.hash(field);
    }

    @Override public String toString() {
        return "MissingFieldInEquals{" +
                "field='" + field + '\'' +
                '}';
    }
}
