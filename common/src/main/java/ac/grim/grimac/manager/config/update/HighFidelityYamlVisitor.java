package ac.grim.grimac.manager.config.update;

import com.amihaiemil.eoyaml.*;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A modified, high-fidelity version of eo-yaml's internal YamlPrintVisitor.
 * This visitor is designed to preserve original formatting as closely as possible,
 * including configurable indentation, correct comment placement, and proper
 * handling of empty strings.
 */
public final class HighFidelityYamlVisitor implements YamlVisitor<String> {

    private final int indentation;
    private final String lineSeparator;

    public HighFidelityYamlVisitor(int indentation) {
        this.indentation = indentation;
        this.lineSeparator = System.lineSeparator();
    }

    // This method is the main dispatcher. It correctly calls the specific visit methods.
    @Override
    public String visitYamlNode(final YamlNode node) {
        if (node == null) {
            return "null";
        }
        final StringWriter writer = new StringWriter();
        if (node.isEmpty()) {
            if (node instanceof YamlSequence) {
                writer.append("[]");
            } else if (node instanceof YamlMapping) {
                writer.append("{}");
            } else {
                // MODIFICATION: Handle empty string "" correctly instead of outputting null.
                if (node instanceof Scalar && ((Scalar) node).value().isEmpty()) {
                    writer.append("\"\"");
                } else {
                    writer.append("null");
                }
            }
        } else {
            if (node instanceof Scalar) {
                writer.append(this.visitScalar((Scalar) node));
            } else if (node instanceof YamlSequence) {
                writer.append(this.visitYamlSequence((YamlSequence) node));
            } else if (node instanceof YamlMapping) {
                writer.append(this.visitYamlMapping((YamlMapping) node));
            }
        }
        return writer.toString();
    }

    @Override
    public String visitYamlMapping(final YamlMapping node) {
        final StringWriter writer = new StringWriter();
        this.printBlockMapping(node, writer);
        final String printedMapping = writer.toString();
        if (printedMapping.endsWith(this.lineSeparator)) {
            return printedMapping.substring(0, printedMapping.length() - this.lineSeparator.length());
        }
        return printedMapping;
    }

    @Override
    public String visitYamlSequence(final YamlSequence node) {
        final StringWriter writer = new StringWriter();
        this.printBlockSequence(node, writer);
        final String printedSequence = writer.toString();
        if (printedSequence.endsWith(this.lineSeparator)) {
            return printedSequence.substring(0, printedSequence.length() - this.lineSeparator.length());
        }
        return printedSequence;
    }

    @Override
    public String defaultResult() {
        return "";
    }

    @Override
    public String aggregateResult(
            final String aggregate, final String nextResult
    ) {
        return aggregate + nextResult;
    }

    @Override
    public String visitScalar(final Scalar node) {
        // We handle scalar printing within the mapping/sequence context to manage comments.
        // This method is called as a fallback if a scalar is visited directly.
        String value = node.value();
        if(value.isEmpty()) return "\"\"";
        return value;
    }

    private void printBlockMapping(final YamlMapping node, final StringWriter writer) {
        for (final YamlNode key : node.keys()) {
            final YamlNode value = node.value(key);
            writer.append(this.printPossibleComment(value)); // Print "above" comments
            writer.append(this.indent(this.indentation));
            writer.append(this.visitYamlNode(key)); // Print the key
            writer.append(":");

            if (value instanceof YamlMapping || value instanceof YamlSequence) {
                writer.append(this.lineSeparator);
                writer.append(this.indent(this.indentation));
                writer.append(this.indent(this.visitYamlNode(value), this.indentation));
            } else {
                writer.append(" ");
                writer.append(this.visitYamlNode(value));
            }
            // MODIFICATION: Print inline comment correctly.
            writer.append(this.printPossibleInlineComment(value));
            writer.append(this.lineSeparator);
        }
    }

    private void printBlockSequence(final YamlSequence node, final StringWriter writer) {
        for (final YamlNode value : node.values()) {
            writer.append(this.printPossibleComment(value));
            writer.append(this.indent(this.indentation));
            writer.append("-");

            if (value instanceof YamlMapping || value instanceof YamlSequence) {
                writer.append(this.lineSeparator);
                writer.append(this.indent(this.indentation));
                writer.append(this.indent(this.visitYamlNode(value), this.indentation));
            } else {
                writer.append(" ");
                writer.append(this.visitYamlNode(value));
            }
            writer.append(this.printPossibleInlineComment(value));
            writer.append(this.lineSeparator);
        }
    }

    private String printPossibleComment(final YamlNode node) {
        if (node != null && node.comment() != null) {
            String com = node.comment().value();
            if (!com.isEmpty()) {
                StringBuilder indentedComment = new StringBuilder();
                String[] lines = com.split("\r?\n");
                for (String line : lines) {
                    // MODIFICATION: Check if it's NOT an inline comment before printing above.
                    if (!line.trim().startsWith("#")) {
                        indentedComment.append(this.indent(this.indentation))
                                       .append("# ")
                                       .append(line)
                                       .append(this.lineSeparator);
                    }
                }
                return indentedComment.toString();
            }
        }
        return "";
    }
    
    // MODIFICATION: New method to specifically handle inline comments.
    private String printPossibleInlineComment(final YamlNode node) {
        if (node != null && node.comment() != null) {
            String com = node.comment().value();
            if (!com.isEmpty() && !com.contains("\n")) {
                return " # " + com;
            }
        }
        return "";
    }

    private String indent(final int spaces) {
        return " ".repeat(spaces);
    }
    
    private String indent(final String text, final int spaces) {
        String indentation = indent(spaces);
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\r?\n");
        for (int i = 0; i < lines.length; i++) {
            result.append(indentation).append(lines[i]);
            if (i < lines.length - 1) {
                result.append(this.lineSeparator);
            }
        }
        return result.toString();
    }
}
