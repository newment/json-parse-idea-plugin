package ink.ican.jsonparse.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonFormatToolWindowFactory implements ToolWindowFactory {

    private static final ObjectMapper objectMapper = createCustomObjectMapper();

    // JSON syntax highlighting color definitions
    private static final Color KEY_COLOR = new JBColor(new Color(0, 0, 255), new Color(100, 149, 237)); // Key name - Blue
    private static final Color STRING_COLOR = new JBColor(new Color(0, 128, 0), new Color(144, 238, 144)); // String value - Green
    private static final Color NUMBER_COLOR = new JBColor(new Color(139, 0, 0), new Color(255, 99, 71)); // Number value - Red
    private static final Color BOOLEAN_COLOR = new JBColor(new Color(128, 0, 128), new Color(186, 85, 211)); // Boolean - Purple
    private static final Color NULL_COLOR = new JBColor(Gray._128, Gray._169); // null value - Gray
    private static final Color BRACE_COLOR = new JBColor(Gray._0, Gray._200); // Brackets - Black/Gray
    private static final Color COMMA_COLOR = new JBColor(new Color(150, 75, 0), new Color(210, 180, 140)); // Comma - Brown

    /**
     * Create custom ObjectMapper for pretty array formatting
     */
    private static ObjectMapper createCustomObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Custom pretty printer
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();

        // Array formatting: each element on a separate line
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        // Object formatting: each property on a separate line
        prettyPrinter.indentObjectsWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        // Set separator between array elements (newline)
        prettyPrinter = prettyPrinter.withArrayIndenter(new DefaultIndenter("  ", "\n"));

        // Set separator between object properties (newline)
        prettyPrinter = prettyPrinter.withObjectIndenter(new DefaultIndenter("  ", "\n"));

        mapper.setDefaultPrettyPrinter(prettyPrinter);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 1. Create components
        final JBTextArea inputArea = new JBTextArea();
        final JTextPane outputPane = createSyntaxHighlightingPane(); // Use component that supports syntax highlighting

        // 2. Create scroll pane panels
        JBScrollPane inputScroll = new JBScrollPane(inputArea);
        JBScrollPane outputScroll = new JBScrollPane(outputPane);

        // 3. Optimize layout: use split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputScroll, outputScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(0);

        // 4. Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 5. Bind panel to ToolWindow
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);

        // 6. Add input listener
        inputArea.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                formatJson(inputArea, outputPane);
            }
        });
    }

    /**
     * Create text pane with syntax highlighting support
     */
    private JTextPane createSyntaxHighlightingPane() {
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        return textPane;
    }

    /**
     * Format JSON and add syntax highlighting
     */
    private void formatJson(JBTextArea inputArea, JTextPane outputPane) {
        String rawJson = inputArea.getText().trim();
        StyledDocument doc = outputPane.getStyledDocument();

        try {
            // Clear document
            doc.remove(0, doc.getLength());

            if (rawJson.isEmpty()) {
                return;
            }

            // Parse and format JSON
            Object jsonObject = objectMapper.readValue(rawJson, Object.class);
            String formattedJson = objectMapper.writeValueAsString(jsonObject);

            // Apply syntax highlighting
            applyJsonSyntaxHighlighting(doc, formattedJson);
        } catch (JsonProcessingException e) {
            setErrorText(doc, "Invalid JSON: " + e.getOriginalMessage());
        } catch (Exception e) {
            setErrorText(doc, "Error: " + e.getMessage());
        }
    }

    /**
     * Apply syntax highlighting to JSON text (enhanced version)
     */
    private void applyJsonSyntaxHighlighting(StyledDocument doc, String json) {
        try {
            // Clear document
            doc.remove(0, doc.getLength());

            // Define JSON syntax element matching rules (enhanced version)
            String patternString =
                    "(?<brace>[\\[\\]{}])|" +         // Brackets
                            "(?<comma>,)|" +                   // Comma
                            "(?<key>\"[^\"]*\"\\s*:)|" +       // Key name
                            "(?<string>\"[^\"]*\")|" +         // String value
                            "(?<number>-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?)|" + // Number value
                            "(?<boolean>true|false)|" +        // Boolean
                            "(?<null>null)";                   // null value

            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(json);

            int lastEnd = 0;

            while (matcher.find()) {
                // Add normal text (non-matching parts)
                if (matcher.start() > lastEnd) {
                    addStyledText(doc, json.substring(lastEnd, matcher.start()), null);
                }

                // Add text with different styles based on match type
                if (matcher.group("brace") != null) {
                    addStyledText(doc, matcher.group("brace"), createAttributes(BRACE_COLOR, true));
                }
                else if (matcher.group("comma") != null) {
                    addStyledText(doc, matcher.group("comma"), createAttributes(COMMA_COLOR, false));
                }
                else if (matcher.group("key") != null) {
                    addStyledText(doc, matcher.group("key"), createAttributes(KEY_COLOR, false));
                }
                else if (matcher.group("string") != null) {
                    addStyledText(doc, matcher.group("string"), createAttributes(STRING_COLOR, false));
                }
                else if (matcher.group("number") != null) {
                    addStyledText(doc, matcher.group("number"), createAttributes(NUMBER_COLOR, false));
                }
                else if (matcher.group("boolean") != null) {
                    addStyledText(doc, matcher.group("boolean"), createAttributes(BOOLEAN_COLOR, false));
                }
                else if (matcher.group("null") != null) {
                    addStyledText(doc, matcher.group("null"), createAttributes(NULL_COLOR, false));
                }

                lastEnd = matcher.end();
            }

            // Add remaining text
            if (lastEnd < json.length()) {
                addStyledText(doc, json.substring(lastEnd), null);
            }
        } catch (Exception e) {
            setErrorText(doc, "Highlighting error: " + e.getMessage());
        }
    }

    /**
     * Create text style attributes (enhanced version)
     */
    private SimpleAttributeSet createAttributes(Color color, boolean bold) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setBold(attrs, bold);
        return attrs;
    }

    /**
     * Add styled text to document
     */
    private void addStyledText(StyledDocument doc, String text, AttributeSet attrs) {
        try {
            int start = doc.getLength();
            doc.insertString(start, text, attrs);
        } catch (BadLocationException e) {
            // Ignore position errors
        }
    }

    /**
     * Set error text (displayed in red)
     */
    private void setErrorText(StyledDocument doc, String text) {
        try {
            doc.remove(0, doc.getLength());
            SimpleAttributeSet errorAttrs = new SimpleAttributeSet();
            StyleConstants.setForeground(errorAttrs, JBColor.RED);
            doc.insertString(0, text, errorAttrs);
        } catch (BadLocationException e) {
            // Ignore position errors
        }
    }
}
