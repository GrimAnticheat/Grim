package ac.grim.grimac.manager.config.update;// File: CommentedYamlConfiguration.java
import com.amihaiemil.eoyaml.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A YAML configuration wrapper that preserves comments and formatting.
 * It provides a high-level, path-based API for getting and setting values.
 *
 * (Final Corrected Version - Heuristic based on comment content)
 */
public class CommentedYamlConfiguration {

    private final File file;
    private YamlMapping root;

    public CommentedYamlConfiguration(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }
        this.file = file;
        this.root = Yaml.createYamlInput(file).readYamlMapping();
    }

    public YamlNode getNode(String path) {
        String[] keys = path.split("\\.");
        YamlNode currentNode = this.root;
        for (String key : keys) {
            if (currentNode instanceof YamlMapping) {
                YamlNode nextNode = ((YamlMapping) currentNode).value(key);
                if (nextNode == null) return null;
                currentNode = nextNode;
            } else {
                return null;
            }
        }
        return currentNode;
    }

    public void setValue(String path, Object value) {
        if (getNode(path) == null) {
            return;
        }
        List<String> keys = new ArrayList<>(Arrays.asList(path.split("\\.")));
        this.root = (YamlMapping) replaceValue(this.root, keys, value);
    }

    /**
     * Saves the modified configuration using our perfect CustomYamlVisitor.
     */
    public void save() throws IOException {
        HighFidelityYamlVisitor visitor = new HighFidelityYamlVisitor(4); // Or 2 for standard YAML
        String perfectlyFormattedYaml = visitor.visitYamlNode(this.root);

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.print(perfectlyFormattedYaml);
        }
    }

    private YamlNode replaceValue(YamlNode currentNode, List<String> path, Object value) {
        if (path.isEmpty() || !(currentNode instanceof YamlMapping)) {
            return currentNode;
        }

        String key = path.get(0);
        List<String> remainingPath = path.subList(1, path.size());
        YamlMapping currentMapping = (YamlMapping) currentNode;
        YamlNode childNode = currentMapping.value(key);

        if (childNode == null) {
            return currentMapping;
        }

        YamlNode newChildNode;
        if (remainingPath.isEmpty()) {
            // We've reached the target node. Rebuild it with the new value.
            if (value instanceof List) {
                newChildNode = buildYamlSequence((List<?>) value, childNode.comment());
            } else {
                newChildNode = buildNewScalar(childNode, value.toString());
            }
        } else {
            // We need to go deeper. Recurse.
            newChildNode = replaceValue(childNode, remainingPath, value);
        }

        // Rebuild the parent mapping to include the modified child.
        YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
        for (YamlNode existingKey : currentMapping.keys()) {
            if (existingKey.asScalar().value().equals(key)) {
                builder = builder.add(key, newChildNode);
            } else {
                builder = builder.add(existingKey, currentMapping.value(existingKey));
            }
        }
        return builder.build(currentMapping.comment().value());
    }

    private Scalar buildNewScalar(YamlNode originalNode, String newValue) {
        Comment comment = originalNode.comment();
        String commentText = comment.value();

        if (commentText.isEmpty()) {
            return Yaml.createYamlScalarBuilder().addLine(newValue).buildPlainScalar();
        }

        // HEURISTIC: If the comment contains a newline, it's an "above" comment.
        // Otherwise, it's an "inline" comment. This is robust for config files.
        if (commentText.contains("\n")) {
            // "Above" comment
            return Yaml.createYamlScalarBuilder()
                    .addLine(newValue)
                    .buildPlainScalar(commentText, ""); // Pass comment as "above"
        } else {
            // "Inline" comment
            return Yaml.createYamlScalarBuilder()
                    .addLine(newValue)
                    .buildPlainScalar("", commentText); // Pass comment as "inline"
        }
    }

    private YamlSequence buildYamlSequence(List<?> values, Comment comment) {
        YamlSequenceBuilder builder = Yaml.createYamlSequenceBuilder();
        for (Object value : values) {
            // This handles lists of strings correctly
            if (value instanceof String) {
                builder = builder.add((String) value);
            } else {
                builder = builder.add(value.toString());
            }
        }
        // Sequences generally only have "above" comments.
        return builder.build(comment.value());
    }
}
