package ac.grim.grimac.checks;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.api.storage.verbose.VerboseTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Build-time verification of every verbose template and writer chain in the
 * check sources. This is the no-codegen substitute for compile-time order
 * checking: it fails the build (instead of the first flag) when
 *
 * <ul>
 *   <li>a template is malformed or references an unregistered tag,</li>
 *   <li>a {@code V.write(verbose())...} chain's wire types drift from the
 *       template's field order, or</li>
 *   <li>a shape selector constant goes out of range.</li>
 * </ul>
 *
 * <p>Chains whose shape selector or continuation can't be resolved statically
 * (selector passed as a variable, writer extended across statements) are
 * listed in {@link #PARTIALLY_AUDITED}; the always-on runtime writer
 * validation covers those. The allowlist is asserted both ways so it can't
 * rot.
 */
class VerboseTemplateAuditTest {

    /** file name -> reason; chains here are validated at runtime by Verbose.Writer instead. */
    private static final Map<String, String> PARTIALLY_AUDITED = Map.of(
            "ExploitB.java", "flagLiteral passes the shape selector as a variable",
            "PacketOrderC.java", "writeKind passes the shape selector as a variable",
            "MultiActionsF.java", "writeAction passes the shape selector as a variable",
            "MultiActionsG.java", "writeAction passes the shape selector as a variable",
            "PacketOrderK.java", "writer extended conditionally after write(buf, kind)");

    private static final Map<String, List<VerboseSchema.TypeTag>> WRITER_METHODS = writerMethods();

    private static Path checksRoot;

    @BeforeAll
    static void setUp() throws IOException {
        checksRoot = Path.of("src/main/java/ac/grim/grimac/checks").toAbsolutePath();
        assertTrue(Files.isDirectory(checksRoot), "expected to run from the common module dir");
        registerStandInTags();
    }

    @Test
    void everyTemplateParses() throws IOException {
        List<String> failures = new ArrayList<>();
        int templates = 0;
        for (Map.Entry<Path, List<String>> entry : templatesByFile().entrySet()) {
            for (String template : entry.getValue()) {
                templates++;
                try {
                    Verbose.of(template).schema();
                } catch (RuntimeException e) {
                    failures.add(entry.getKey().getFileName() + ": \"" + template + "\" -> " + e.getMessage());
                }
            }
        }
        assertTrue(templates > 50, "template extraction looks broken; found only " + templates);
        if (!failures.isEmpty()) fail(String.join("\n", failures));
    }

    @Test
    void writeChainsMatchTemplateFieldOrder() throws IOException {
        List<String> failures = new ArrayList<>();
        Set<String> filesNeedingAllowlist = new TreeSet<>();
        int auditedChains = 0;

        for (Map.Entry<Path, List<String>> entry : templatesByFile().entrySet()) {
            Path file = entry.getKey();
            String fileName = file.getFileName().toString();
            if (entry.getValue().size() != 1) continue; // multiple V constants in one file: not used today
            Verbose verbose = Verbose.of(entry.getValue().get(0));
            String source = Files.readString(file);

            for (Chain chain : extractChains(source)) {
                Integer shape = chain.shapeExpr == null ? null : resolveShape(source, chain.shapeExpr);
                if (verbose.shapes() > 1 && shape == null) {
                    filesNeedingAllowlist.add(fileName);
                    continue;
                }
                if (verbose.shapes() > 1 && shape >= verbose.shapes()) {
                    failures.add(fileName + ": shape " + chain.shapeExpr + "=" + shape
                            + " out of range for " + verbose.shapes() + " shapes");
                    continue;
                }
                List<VerboseSchema.TypeTag> expected = typesOf(
                        verbose.shapeFields(verbose.shapes() > 1 ? shape : 0));
                List<VerboseSchema.TypeTag> actual = new ArrayList<>();
                boolean resolvable = true;
                for (String method : chain.methods) {
                    List<VerboseSchema.TypeTag> wire = WRITER_METHODS.get(method);
                    if (wire == null) {
                        resolvable = false; // unknown continuation, e.g. helper call
                        break;
                    }
                    actual.addAll(wire);
                }
                if (!resolvable) {
                    filesNeedingAllowlist.add(fileName);
                    continue;
                }
                auditedChains++;
                if (!actual.equals(expected)) {
                    if (actual.size() < expected.size() && expected.subList(0, actual.size()).equals(actual)) {
                        // prefix only: chain probably continues elsewhere
                        filesNeedingAllowlist.add(fileName);
                        auditedChains--;
                    } else {
                        failures.add(fileName + ": chain " + chain.methods + " writes " + names(actual)
                                + " but template shape expects " + names(expected));
                    }
                }
            }
        }

        assertTrue(auditedChains > 50, "chain extraction looks broken; audited only " + auditedChains);
        if (!failures.isEmpty()) fail(String.join("\n", failures));

        // Allowlist hygiene: exactly the files that need it, no rot.
        for (String needed : filesNeedingAllowlist) {
            assertTrue(PARTIALLY_AUDITED.containsKey(needed),
                    needed + " has statically unresolvable write chains; audit them manually and add to PARTIALLY_AUDITED");
        }
        for (String listed : PARTIALLY_AUDITED.keySet()) {
            assertTrue(filesNeedingAllowlist.contains(listed),
                    listed + " no longer needs PARTIALLY_AUDITED; remove it");
        }
    }

    @Test
    void standInTagsMirrorVerboseCodecs() throws IOException {
        // If VerboseCodecs gains/changes a tag, registerStandInTags() must learn it,
        // otherwise template parsing here would diverge from runtime.
        String source = Files.readString(checksRoot.resolve("impl/verbose/VerboseCodecs.java"));
        Set<String> registered = new HashSet<>();
        Matcher m = Pattern.compile("register(?:Enum|EnumLower)?\\(\\s*\"([a-z_0-9]+)\"").matcher(source);
        while (m.find()) registered.add(m.group(1));
        assertFalse(registered.isEmpty());
        for (String tag : registered) {
            assertTrue(STAND_IN_TAGS.containsKey(tag),
                    "VerboseCodecs registers {" + tag + "} but the audit test doesn't; add it to STAND_IN_TAGS");
        }
    }

    // ---------------------------------------------------------------- helpers

    private static final Map<String, List<VerboseSchema.TypeTag>> STAND_IN_TAGS = standInTags();

    private static Map<String, List<VerboseSchema.TypeTag>> standInTags() {
        Map<String, List<VerboseSchema.TypeTag>> tags = new LinkedHashMap<>();
        for (String n : new String[]{"face", "digging", "digging_lower", "clicktype",
                "clicktype_lower", "entityaction", "hand", "entity"}) {
            tags.put(n, List.of(VerboseSchema.TypeTag.VI));
        }
        tags.put("block", List.of(VerboseSchema.TypeTag.ZZ));
        tags.put("item", List.of(VerboseSchema.TypeTag.ZZ));
        tags.put("packet", List.of(VerboseSchema.TypeTag.ZZ));
        tags.put("offset", List.of(VerboseSchema.TypeTag.F64));
        tags.put("stdnum", List.of(VerboseSchema.TypeTag.F64));
        return tags;
    }

    private static void registerStandInTags() {
        // Same names + wire shapes as VerboseCodecs, no PacketEvents needed.
        STAND_IN_TAGS.forEach((name, wire) ->
                VerboseTags.register(name, wire, (in, ctx, out, fmt) -> {
                    for (VerboseSchema.TypeTag tag : wire) in.skip(tag.tag());
                }));
    }

    private static Map<String, List<VerboseSchema.TypeTag>> writerMethods() {
        Map<String, List<VerboseSchema.TypeTag>> m = new HashMap<>();
        m.put("f64", List.of(VerboseSchema.TypeTag.F64));
        m.put("f32", List.of(VerboseSchema.TypeTag.F32));
        m.put("uint", List.of(VerboseSchema.TypeTag.VI));
        m.put("sint", List.of(VerboseSchema.TypeTag.ZZ));
        m.put("ulong", List.of(VerboseSchema.TypeTag.VL));
        m.put("bool", List.of(VerboseSchema.TypeTag.BOOL));
        m.put("str", List.of(VerboseSchema.TypeTag.STR));
        m.put("mcPos", List.of(VerboseSchema.TypeTag.VL, VerboseSchema.TypeTag.ZZ));
        m.put("cursor", List.of(VerboseSchema.TypeTag.F32, VerboseSchema.TypeTag.F32, VerboseSchema.TypeTag.F32));
        m.put("slong", List.of(VerboseSchema.TypeTag.ZZ, VerboseSchema.TypeTag.ZZ));
        m.put("end", List.of());
        return m;
    }

    private static Map<Path, List<String>> templatesByFile() throws IOException {
        Map<Path, List<String>> result = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(checksRoot)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                List<String> templates = extractTemplates(source);
                if (!templates.isEmpty()) result.put(file, templates);
            }
        }
        return result;
    }

    /** Reconstruct each {@code Verbose.of("...").or("...")} statement's template string. */
    static List<String> extractTemplates(String source) {
        List<String> templates = new ArrayList<>();
        Matcher m = Pattern.compile("Verbose\\s*\\.\\s*of\\s*\\(").matcher(source);
        int searchFrom = 0;
        while (m.find(searchFrom)) {
            int statementEnd = source.indexOf(';', m.end());
            if (statementEnd < 0) break;
            String statement = source.substring(m.start(), statementEnd);
            // Split shapes on .or( boundaries, then concat the literals in each segment.
            String[] segments = statement.split("\\.or\\(");
            StringBuilder template = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                if (i > 0) template.append('|');
                template.append(concatLiterals(segments[i]));
            }
            if (template.length() > 0) templates.add(template.toString());
            searchFrom = statementEnd;
        }
        return templates;
    }

    /** Concatenate all Java string literals in {@code segment}, honouring escapes. */
    private static String concatLiterals(String segment) {
        StringBuilder out = new StringBuilder();
        Matcher m = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(segment);
        while (m.find()) out.append(unescapeJava(m.group(1)));
        return out.toString();
    }

    private static String unescapeJava(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case '\\' -> out.append('\\');
                    case '"' -> out.append('"');
                    default -> out.append('\\').append(n); // template escapes like \[ stay
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** A writer chain anchored at {@code V.write(verbose()} with its method names in order. */
    private record Chain(String shapeExpr, List<String> methods) {
    }

    static List<Chain> extractChains(String source) {
        List<Chain> chains = new ArrayList<>();
        Matcher m = Pattern.compile("\\bV\\s*\\.write\\(verbose\\(\\)").matcher(source);
        while (m.find()) {
            int pos = m.end();
            // optional ", SHAPE" before the closing paren of write(...)
            String shapeExpr = null;
            int close = skipToCloseParen(source, pos);
            String writeArgsTail = source.substring(pos, close).trim();
            if (writeArgsTail.startsWith(",")) shapeExpr = writeArgsTail.substring(1).trim();
            pos = close + 1;

            List<String> methods = new ArrayList<>();
            while (true) {
                int next = skipWhitespace(source, pos);
                if (next >= source.length() || source.charAt(next) != '.') break;
                int nameStart = next + 1;
                int nameEnd = nameStart;
                while (nameEnd < source.length() && Character.isJavaIdentifierPart(source.charAt(nameEnd))) nameEnd++;
                int parenStart = skipWhitespace(source, nameEnd);
                if (parenStart >= source.length() || source.charAt(parenStart) != '(') break;
                methods.add(source.substring(nameStart, nameEnd));
                pos = skipToCloseParen(source, parenStart + 1) + 1;
            }
            chains.add(new Chain(shapeExpr, methods));
        }
        return chains;
    }

    /** Given index just after '(', return the index of its matching ')'. */
    private static int skipToCloseParen(String source, int pos) {
        int depth = 1;
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == '"') {
                pos++;
                while (pos < source.length() && source.charAt(pos) != '"') {
                    if (source.charAt(pos) == '\\') pos++;
                    pos++;
                }
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) return pos;
            }
            pos++;
        }
        throw new IllegalStateException("unbalanced parens");
    }

    private static int skipWhitespace(String source, int pos) {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
            } else if (c == '/' && pos + 1 < source.length() && source.charAt(pos + 1) == '/') {
                int eol = source.indexOf('\n', pos);
                pos = eol < 0 ? source.length() : eol + 1;
            } else if (c == '/' && pos + 1 < source.length() && source.charAt(pos + 1) == '*') {
                int end = source.indexOf("*/", pos + 2);
                pos = end < 0 ? source.length() : end + 2;
            } else {
                break;
            }
        }
        return pos;
    }

    /** Resolve a shape expression: int literal or same-file {@code static final int} constant. */
    private static Integer resolveShape(String source, String expr) {
        if (expr.matches("\\d+")) return Integer.parseInt(expr);
        Matcher m = Pattern.compile("static\\s+final\\s+int\\s+" + Pattern.quote(expr) + "\\s*=\\s*(\\d+)\\s*;")
                .matcher(source);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private static List<VerboseSchema.TypeTag> typesOf(List<VerboseSchema.Field> fields) {
        return fields.stream().map(VerboseSchema.Field::type).toList();
    }

    private static List<String> names(List<VerboseSchema.TypeTag> tags) {
        return tags.stream().map(VerboseSchema.TypeTag::wireName).toList();
    }
}
