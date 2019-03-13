package me.tatarka.checker;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Test;

public class YesdataCheckerTest {
    private static final String TEST = "Test.java";
    // language=java
    private static final String IMPORT_DATA = "import me.tatarka.yesdata.annotation.Data;";

    private final CompilationTestHelper compiler = CompilationTestHelper.newInstance(YesdataChecker.class, getClass());

    @Test
    // language=java
    public void fails_if_class_does_not_implement_equals_hashCode_toString() {
        compiler.addSourceLines(TEST, IMPORT_DATA,
                "// BUG: Diagnostic contains: [Yesdata] Class does not implement 'equals', 'hashCode', and 'toString'.",
                "@Data class Test {}").doTest();
    }

    @Test
    // language=java
    public void fails_if_class_does_not_implement_equals() {
        compiler.addSourceLines(TEST, IMPORT_DATA,
                "// BUG: Diagnostic contains: [Yesdata] Class does not implement 'equals'.",
                "@Data class Test {\n" +
                        "    @Override public int hashCode() {\n" +
                        "        return 0;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override public String toString() {\n" +
                        "        return \"NoFields{}\";\n" +
                        "    }\n" +
                        "}"
                ).doTest();
    }

    @Test
    // language=java
    public void passes_when_implementing_all_methods_with_no_fields() {
        compiler.addSourceLines(TEST, IMPORT_DATA,
                "@Data class Test {\n" +
                        "    @Override public boolean equals(Object other) {\n" +
                        "        return other instanceof Test;\n" +
                        "    }\n" +
                        "    \n" +
                        "    @Override public int hashCode() {\n" +
                        "        return 0;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override public String toString() {\n" +
                        "        return \"NoFields{}\";\n" +
                        "    }\n" +
                        "}"
        ).doTest();
    }

    @Test
    // language=java
    public void passes_when_implementing_all_methods_with_one_field() {
        compiler.addSourceLines(TEST, IMPORT_DATA,
                "import java.util.Objects;\n" +
                        "\n" +
                        "@Data class Test {\n" +
                        "    public final String field;\n" +
                        "\n" +
                        "    public Test(String field) {\n" +
                        "        this.field = field;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override public boolean equals(Object o) {\n" +
                        "        if (this == o) return true;\n" +
                        "        if (o == null || getClass() != o.getClass()) return false;\n" +
                        "        Test test = (Test) o;\n" +
                        "        return Objects.equals(field, test.field);\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override public int hashCode() {\n" +
                        "        return Objects.hash(field);\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override public String toString() {\n" +
                        "        return \"Test{\" +\n" +
                        "                \"field='\" + field + '\\'' +\n" +
                        "                '}';\n" +
                        "    }\n" +
                        "}\n").doTest();
    }

    @Test
    // language=java
    public void fails_when_field_is_not_used_in_a_method() {
        compiler.addSourceLines(TEST, IMPORT_DATA,
                "import java.util.Objects;\n" +
                        "\n" +
                        "@Data final class Test {\n" +
                        "    public final String field;\n" +
                        "\n" +
                        "    public Test(String field) {\n" +
                        "        this.field = field;\n" +
                        "    }\n" +
                        "\n" +
                        "    // BUG: Diagnostic contains: [Yesdata] Missing field 'field' in equals implementation.\n" +
                        "    @Override public boolean equals(Object o) {\n" +
                        "        return true;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override public int hashCode() {\n" +
                        "        return Objects.hash(field);\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override public String toString() {\n" +
                        "        return \"Test{\" +\n" +
                        "                \"field='\" + field + '\\'' +\n" +
                        "                '}';\n" +
                        "    }\n" +
                        "}\n").doTest();
    }

    @Test
    // language=java
    public void fails_when_multiple_fields_are_not_used_in_a_method() {
        compiler.addSourceLines(TEST, IMPORT_DATA,
                "import java.util.Objects;\n" +
                        "\n" +
                        "@Data final class Test {\n" +
                        "    public final String field1;\n" +
                        "    public final String field2;\n" +
                        "\n" +
                        "    public Test(String field1, String field2) {\n" +
                        "        this.field1 = field1;\n" +
                        "        this.field2 = field2;\n" +
                        "    }\n" +
                        "\n" +
                        "    // BUG: Diagnostic contains: [Yesdata] Missing fields 'field1' and 'field2' in equals implementation.\n" +
                        "    @Override public boolean equals(Object o) {\n" +
                        "        return true;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override public int hashCode() {\n" +
                        "        return Objects.hash(field1, field2);\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override public String toString() {\n" +
                        "        return \"Test{\" +\n" +
                        "                \"field1='\" + field1 + '\\'' +\n" +
                        "                \", field2='\" + field2 + '\\'' +\n" +
                        "                '}';\n" +
                        "    }\n" +
                        "}\n").doTest();
    }

    @Test
    // language=java
    public void fails_with_multiple_messages_if_fields_are_not_used_in_multiple_methods() {
        compiler.addSourceLines(TEST, IMPORT_DATA,
                "@Data final class Test {\n" +
                        "    public final String field1;\n" +
                        "    public final String field2;\n" +
                        "\n" +
                        "    public Test(String field1, String field2) {\n" +
                        "        this.field1 = field1;\n" +
                        "        this.field2 = field2;\n" +
                        "    }\n" +
                        "\n" +
                        "    // BUG: Diagnostic contains: [Yesdata] Missing fields 'field1' and 'field2' in equals implementation.\n" +
                        "    @Override public boolean equals(Object o) {\n" +
                        "        return true;\n" +
                        "    }\n" +
                        "\n" +
                        "    // BUG: Diagnostic contains: [Yesdata] Missing fields 'field1' and 'field2' in hashCode implementation.\n" +
                        "    @Override public int hashCode() {\n" +
                        "        return 0;\n" +
                        "    }\n" +
                        "\n" +
                        "    // BUG: Diagnostic contains: [Yesdata] Missing fields 'field1' and 'field2' in toString implementation.\n" +
                        "    @Override public String toString() {\n" +
                        "        return \"Test{}\";\n" +
                        "    }\n" +
                        "}\n").doTest();
    }
}
