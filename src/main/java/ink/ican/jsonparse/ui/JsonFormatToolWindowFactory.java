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

    // JSON 语法高亮颜色定义
    private static final Color KEY_COLOR = new JBColor(new Color(0, 0, 255), new Color(100, 149, 237)); // 键名 - 蓝色
    private static final Color STRING_COLOR = new JBColor(new Color(0, 128, 0), new Color(144, 238, 144)); // 字符串值 - 绿色
    private static final Color NUMBER_COLOR = new JBColor(new Color(139, 0, 0), new Color(255, 99, 71)); // 数字值 - 红色
    private static final Color BOOLEAN_COLOR = new JBColor(new Color(128, 0, 128), new Color(186, 85, 211)); // 布尔值 - 紫色
    private static final Color NULL_COLOR = new JBColor(Gray._128, Gray._169); // null值 - 灰色
    private static final Color BRACE_COLOR = new JBColor(Gray._0, Gray._200); // 括号 - 黑色/灰色
    private static final Color COMMA_COLOR = new JBColor(new Color(150, 75, 0), new Color(210, 180, 140)); // 逗号 - 棕色

    /**
     * 创建自定义的 ObjectMapper 实现美观的数组格式化
     */
    private static ObjectMapper createCustomObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 自定义美化打印器
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();

        // 数组格式化：每个元素单独一行
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        // 对象格式化：每个属性单独一行
        prettyPrinter.indentObjectsWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        // 设置数组元素间的分隔符（换行）
        prettyPrinter = prettyPrinter.withArrayIndenter(new DefaultIndenter("  ", "\n"));

        // 设置对象属性间的分隔符（换行）
        prettyPrinter = prettyPrinter.withObjectIndenter(new DefaultIndenter("  ", "\n"));

        mapper.setDefaultPrettyPrinter(prettyPrinter);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 1. 创建组件
        final JBTextArea inputArea = new JBTextArea();
        final JTextPane outputPane = createSyntaxHighlightingPane(); // 使用支持语法高亮的组件

        // 2. 创建带滚动条的面板
        JBScrollPane inputScroll = new JBScrollPane(inputArea);
        JBScrollPane outputScroll = new JBScrollPane(outputPane);

        // 3. 优化布局：使用分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputScroll, outputScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(0);

        // 4. 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 5. 将面板绑定到 ToolWindow
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);

        // 6. 添加输入监听
        inputArea.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                formatJson(inputArea, outputPane);
            }
        });
    }

    /**
     * 创建支持语法高亮的文本面板
     */
    private JTextPane createSyntaxHighlightingPane() {
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        return textPane;
    }

    /**
     * 格式化JSON并添加语法高亮
     */
    private void formatJson(JBTextArea inputArea, JTextPane outputPane) {
        String rawJson = inputArea.getText().trim();
        StyledDocument doc = outputPane.getStyledDocument();

        try {
            // 清空文档
            doc.remove(0, doc.getLength());

            if (rawJson.isEmpty()) {
                return;
            }

            // 解析并美化JSON
            Object jsonObject = objectMapper.readValue(rawJson, Object.class);
            String formattedJson = objectMapper.writeValueAsString(jsonObject);

            // 应用语法高亮
            applyJsonSyntaxHighlighting(doc, formattedJson);
        } catch (JsonProcessingException e) {
            setErrorText(doc, "Invalid JSON: " + e.getOriginalMessage());
        } catch (Exception e) {
            setErrorText(doc, "Error: " + e.getMessage());
        }
    }

    /**
     * 为JSON文本应用语法高亮（增强版）
     */
    private void applyJsonSyntaxHighlighting(StyledDocument doc, String json) {
        try {
            // 清空文档
            doc.remove(0, doc.getLength());

            // 定义JSON语法元素的匹配规则（增强版）
            String patternString =
                    "(?<brace>[\\[\\]{}])|" +         // 括号
                            "(?<comma>,)|" +                   // 逗号
                            "(?<key>\"[^\"]*\"\\s*:)|" +       // 键名
                            "(?<string>\"[^\"]*\")|" +         // 字符串值
                            "(?<number>-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?)|" + // 数字值
                            "(?<boolean>true|false)|" +        // 布尔值
                            "(?<null>null)";                   // null值

            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(json);

            int lastEnd = 0;

            while (matcher.find()) {
                // 添加普通文本（非匹配部分）
                if (matcher.start() > lastEnd) {
                    addStyledText(doc, json.substring(lastEnd, matcher.start()), null);
                }

                // 根据匹配类型添加不同样式的文本
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

            // 添加剩余文本
            if (lastEnd < json.length()) {
                addStyledText(doc, json.substring(lastEnd), null);
            }
        } catch (Exception e) {
            setErrorText(doc, "Highlighting error: " + e.getMessage());
        }
    }

    /**
     * 创建文本样式属性（增强版）
     */
    private SimpleAttributeSet createAttributes(Color color, boolean bold) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setBold(attrs, bold);
        return attrs;
    }

    /**
     * 添加带样式的文本到文档
     */
    private void addStyledText(StyledDocument doc, String text, AttributeSet attrs) {
        try {
            int start = doc.getLength();
            doc.insertString(start, text, attrs);
        } catch (BadLocationException e) {
            // 忽略位置错误
        }
    }

    /**
     * 设置错误文本（红色显示）
     */
    private void setErrorText(StyledDocument doc, String text) {
        try {
            doc.remove(0, doc.getLength());
            SimpleAttributeSet errorAttrs = new SimpleAttributeSet();
            StyleConstants.setForeground(errorAttrs, JBColor.RED);
            doc.insertString(0, text, errorAttrs);
        } catch (BadLocationException e) {
            // 忽略位置错误
        }
    }
}
