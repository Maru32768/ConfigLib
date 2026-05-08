package net.kunmc.lab.configlib.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLibAnnotationProcessorTest {
    @TempDir
    Path tempDir;

    @Test
    void rangeAllowsNumericPojoLeafField() throws IOException {
        CompileResult result = compile("TestConfig",
                                       "import net.kunmc.lab.configlib.annotation.Range;\n" + "class TestConfig {\n" + "  @Range(min = 1, max = 10)\n" + "  int maxPlayers = 5;\n" + "}\n");

        assertTrue(result.success(), result.diagnosticsText());
    }

    @Test
    void rangeRejectsObjectField() throws IOException {
        CompileResult result = compile("TestConfig",
                                       "import net.kunmc.lab.configlib.annotation.Range;\n" + "class TestConfig {\n" + "  @Range(min = 1, max = 10)\n" + "  ArenaSettings arena = new ArenaSettings();\n" + "  static class ArenaSettings { int maxArenas = 5; }\n" + "}\n");

        assertFalse(result.success());
        assertTrue(result.contains("@Range can only be used on numeric POJO leaf fields."), result.diagnosticsText());
    }

    @Test
    void rangeRejectsInvertedBounds() throws IOException {
        CompileResult result = compile("TestConfig",
                                       "import net.kunmc.lab.configlib.annotation.Range;\n" + "class TestConfig {\n" + "  @Range(min = 10, max = 1)\n" + "  int maxPlayers = 5;\n" + "}\n");

        assertFalse(result.success());
        assertTrue(result.contains("@Range min must be less than or equal to max."), result.diagnosticsText());
    }

    @Test
    void rangeRejectsValueField() throws IOException {
        CompileResult result = compile("TestConfig",
                                       "import net.kunmc.lab.configlib.annotation.Range;\n" + "import net.kunmc.lab.configlib.value.IntegerValue;\n" + "class TestConfig {\n" + "  @Range(min = 1, max = 10)\n" + "  IntegerValue maxPlayers = new IntegerValue(5);\n" + "}\n");

        assertFalse(result.success());
        assertTrue(result.contains("Value fields must use constructor bounds or addValidator(...) instead of @Range."),
                   result.diagnosticsText());
    }

    @Test
    void configNullableRejectsPrimitiveField() throws IOException {
        CompileResult result = compile("TestConfig",
                                       "import net.kunmc.lab.configlib.annotation.ConfigNullable;\n" + "class TestConfig {\n" + "  @ConfigNullable\n" + "  int maxPlayers = 5;\n" + "}\n");

        assertFalse(result.success());
        assertTrue(result.contains("@ConfigNullable cannot be used on primitive fields."), result.diagnosticsText());
    }

    private CompileResult compile(String className, String source) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run on a JDK, not a JRE.");

        Files.createDirectories(tempDir.resolve("classes"));
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            JavaFileObject sourceFile = new StringJavaFileObject(className, source);
            JavaCompiler.CompilationTask task = compiler.getTask(null,
                                                                 fileManager,
                                                                 diagnostics,
                                                                 List.of("-classpath",
                                                                         System.getProperty("java.class.path"),
                                                                         "-d",
                                                                         tempDir.resolve("classes")
                                                                                .toString()),
                                                                 null,
                                                                 List.of(sourceFile));
            task.setProcessors(List.of(new ConfigLibAnnotationProcessor()));
            return new CompileResult(Boolean.TRUE.equals(task.call()), diagnostics.getDiagnostics());
        }
    }

    private static final class StringJavaFileObject extends SimpleJavaFileObject {
        private final String source;

        private StringJavaFileObject(String className, String source) {
            super(URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
                  JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    private static final class CompileResult {
        private final boolean success;
        private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

        private CompileResult(boolean success, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
            this.success = success;
            this.diagnostics = diagnostics;
        }

        private boolean success() {
            return success;
        }

        private boolean contains(String message) {
            return diagnosticsText().contains(message);
        }

        private String diagnosticsText() {
            StringBuilder sb = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
                sb.append(diagnostic.getKind())
                  .append(": ")
                  .append(diagnostic.getMessage(Locale.ROOT))
                  .append(System.lineSeparator());
            }
            return sb.toString();
        }
    }
}
