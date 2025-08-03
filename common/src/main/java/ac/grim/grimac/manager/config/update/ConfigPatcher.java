package ac.grim.grimac.manager.config.update;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs robust, surgical patching of a YAML file to preserve formatting.
 * This is the definitive version, rewritten from the ground up with a robust
 * path-finding algorithm and a correct value formatter.
 */
public final class ConfigPatcher {

    private final File file;
    private List<String> lines;
    private final Map<String, NodePosition> locationMap;

    // A set of YAML keywords that must be quoted if they are to be used as strings.
    private static final Set<String> YAML_KEYWORDS_TO_QUOTE = new HashSet<>(Arrays.asList(
            "y", "Y", "yes", "Yes", "YES", "n", "N", "no", "No", "NO",
            "true", "True", "TRUE", "false", "False", "FALSE",
            "on", "On", "ON", "off", "Off", "OFF",
            "null", "Null", "NULL", "~"
    ));

    // A string containing special YAML indicator characters.
    private static final String YAML_SPECIAL_CHARS = ":{}[]#|>&*!%@`'\",-?!^$";

    public ConfigPatcher(File configFile) throws IOException {
        this.file = configFile;
        this.lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
        this.locationMap = new HashMap<>();
        this.mapFile(); // Build the location map immediately.
    }

    /**
     * This is the core of the new path-finding logic. It iterates through the file
     * once and builds a map of every key's location and indentation.
     */
    private void mapFile() {
        List<String> pathStack = new ArrayList<>();
        List<Integer> indentStack = new ArrayList<>();
        Pattern keyPattern = Pattern.compile("^(\\s*)([^#:]+):(.*)");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            Matcher matcher = keyPattern.matcher(line);
            if (matcher.matches()) {
                String indentStr = matcher.group(1);
                int currentIndent = indentStr.length();
                String key = matcher.group(2).trim();

                while (!indentStack.isEmpty() && currentIndent <= indentStack.get(indentStack.size() - 1)) {
                    pathStack.remove(pathStack.size() - 1);
                    indentStack.remove(indentStack.size() - 1);
                }

                pathStack.add(key);
                indentStack.add(currentIndent);

                String currentPath = String.join(".", pathStack);
                locationMap.put(currentPath, new NodePosition(i, currentIndent));
            }
        }
    }

    public void setValue(String path, Object value) {
        NodePosition position = locationMap.get(path);
        if (position == null) {
            // This can happen for migrated keys, which is fine.
            // System.err.println("Could not find path in config: " + path);
            return;
        }

        if (value instanceof List) {
            replaceListBlock(position, (List<?>) value);
        } else {
            replaceScalarValue(position, value);
        }
    }

    private void replaceScalarValue(NodePosition position, Object value) {
        String line = lines.get(position.lineIndex);
        String valueStr = formatValue(value);

        Pattern linePattern = Pattern.compile("^(\\s*[^:]+:\\s*)(.*?)(\\s*#.*)?$");
        Matcher matcher = linePattern.matcher(line);
        if (matcher.matches()) {
            lines.set(position.lineIndex, matcher.group(1) + valueStr + (matcher.group(3) != null ? matcher.group(3) : ""));
        }
    }

    private void replaceListBlock(NodePosition keyPosition, List<?> newList) {
        int blockStartLine = -1;
        int blockEndLine = keyPosition.lineIndex + 1;
        int listIndent = -1;

        for (int i = keyPosition.lineIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                blockEndLine = i + 1;
                continue;
            }
            int currentIndent = getIndentation(line);
            if (currentIndent <= keyPosition.indent) {
                blockEndLine = i;
                break;
            }
            if (line.trim().startsWith("-")) {
                if (blockStartLine == -1) {
                    blockStartLine = i;
                    listIndent = currentIndent;
                }
                blockEndLine = i + 1;
            }
        }
        if (blockStartLine == -1) {
            blockStartLine = keyPosition.lineIndex + 1;
            blockEndLine = blockStartLine;
            listIndent = keyPosition.indent + 2;
        }

        String newBlock = generateListBlock(newList, listIndent);

        if (blockEndLine > blockStartLine) {
            lines.subList(blockStartLine, blockEndLine).clear();
        }
        if (!newBlock.isEmpty()) {
            lines.addAll(blockStartLine, Arrays.asList(newBlock.split("\r?\n")));
        }
    }

    private String generateListBlock(List<?> list, int indentation) {
        if (list.isEmpty()) {
            return "";
        }
        StringBuilder block = new StringBuilder();
        String indentStr = " ".repeat(indentation);
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            block.append(indentStr)
                    .append("- ")
                    .append(formatValue(item));
            if (i < list.size() - 1) {
                block.append(System.lineSeparator());
            }
        }
        return block.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        String s = value.toString();
        if (s.isEmpty()) {
            return "\"\"";
        }
        boolean needsQuotes = false;
        if (YAML_KEYWORDS_TO_QUOTE.contains(s) || s.startsWith(" ") || s.endsWith(" ")) {
            needsQuotes = true;
        } else {
            try {
                // Check if it's a number-like string (e.g., "5.0", "123")
                if (s.matches("^[+-]?([0-9]*[.])?[0-9]+$")) {
                    Double.parseDouble(s);
                    needsQuotes = true;
                }
            } catch (NumberFormatException e) {
                // Not a number, which is good.
            }
        }
        if (!needsQuotes) {
            for (char c : YAML_SPECIAL_CHARS.toCharArray()) {
                if (s.indexOf(c) != -1) {
                    needsQuotes = true;
                    break;
                }
            }
        }
        if (needsQuotes) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        }
        return s;
    }

    private int getIndentation(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != ' ') return i;
        }
        return 0;
    }

    public void save() throws IOException {
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    private static class NodePosition {
        final int lineIndex;
        final int indent;
        NodePosition(int lineIndex, int indent) {
            this.lineIndex = lineIndex;
            this.indent = indent;
        }
    }
}
