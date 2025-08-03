package ac.grim.grimac.manager.config.update;

import com.amihaiemil.eoyaml.*;
import java.util.stream.Collectors;

/**
 * A final, custom YamlVisitor that prints a YAML document with configurable
 * indentation, preserving comments and correctly handling empty strings.
 */
public class CustomYamlVisitor implements YamlVisitor<String> {

    private final StringBuilder result = new StringBuilder();
    private final int indentSize;
    private int currentIndentLevel = 0;

    public CustomYamlVisitor(int indentSize) {
        this.indentSize = indentSize;
    }

    @Override
    public String visitYamlNode(YamlNode node) {
        node.accept(this);
        return this.result.toString();
    }

    @Override
    public String visitYamlMapping(YamlMapping mapping) {
        printComment(mapping.comment());

        for (YamlNode keyNode : mapping.keys()) {
            YamlNode valueNode = mapping.value(keyNode);
            this.result.append(getIndentationString());
            printNode(keyNode, valueNode);
        }
        return this.result.toString();
    }

    @Override
    public String visitYamlSequence(YamlSequence sequence) {
        printComment(sequence.comment());

        for (YamlNode valueNode : sequence.values()) {
            this.result.append(getIndentationString()).append("- ");
            printNode(null, valueNode); // No key for sequence items
        }
        return this.result.toString();
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

    /**
     * The core printing logic for any key-value or sequence-value pair.
     * @param keyNode The key node (can be null for sequence items).
     * @param valueNode The value node.
     */
    private void printNode(YamlNode keyNode, YamlNode valueNode) {
        Comment valueComment = valueNode.comment();
        String aboveComment = "";
        String inlineComment = "";

        // Heuristic: Multi-line comments go above, single-line go inline.
        if (!valueComment.value().isEmpty()) {
            if (valueComment.value().contains("\n")) {
                aboveComment = valueComment.value();
            } else {
                inlineComment = " # " + valueComment.value();
            }
        }
        
        // If there's an "above" comment, print it on its own indented line(s) first.
        if (!aboveComment.isEmpty()) {
            // Un-indent one level to print the comment *before* the key/list item.
            this.currentIndentLevel--;
            this.result.append(indent(aboveComment)).append(System.lineSeparator());
            this.currentIndentLevel++;
            // Re-add the indentation for the actual key/value line.
             this.result.append(getIndentationString());
        }

        // Print the key if it exists
        if (keyNode != null) {
            this.result.append(keyNode.asScalar().value()).append(": ");
        }

        // Handle the value node
        if (valueNode instanceof YamlMapping || valueNode instanceof YamlSequence) {
            this.result.append(System.lineSeparator());
            this.currentIndentLevel++;
            valueNode.accept(this); // Recursive call
            this.currentIndentLevel--;
        } else {
            // It's a scalar.
            Scalar scalar = valueNode.asScalar();
            
            // **THE FIX for "" vs null **
            if (scalar.value().isEmpty()) {
                this.result.append("\"\""); // Explicitly quote empty strings
            } else {
                this.result.append(scalar.value());
            }
            
            this.result.append(inlineComment).append(System.lineSeparator());
        }
    }

    private void printComment(Comment comment) {
        if (!comment.value().isEmpty()) {
            this.result.append(indent(comment.value())).append(System.lineSeparator());
        }
    }

    private String indent(String text) {
        String indentation = getIndentationString();
        return text.lines().map(line -> indentation + line).collect(Collectors.joining(System.lineSeparator()));
    }

    private String getIndentationString() {
        return " ".repeat(this.currentIndentLevel * this.indentSize);
    }
}
