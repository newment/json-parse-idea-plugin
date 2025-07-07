package ink.ican.jsonparse.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import org.cef.CefClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author lzq
 * 2025/7/7 17:33
 */

public class JsonFormatHtmlWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (JBCefApp.isSupported()) {
            JBCefBrowser browser = new JBCefBrowser();
            URL htmlUrl = getClass().getResource("/index.html");
            if(htmlUrl != null){
                // 方法1：直接加载文件URL（推荐）
                // browser.loadURL(htmlUrl.toExternalForm());

                // 方法2：读取文件内容后加载（备选）
                try (InputStream in = htmlUrl.openStream()) {
                    String htmlContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    browser.loadHTML(htmlContent);
                } catch (IOException e) {
                    browser.loadHTML("<h1>Error reading file: " + e.getMessage() + "</h1>");
                }

            }
            else {
                browser.loadHTML("<h1>HTML file not found!</h1>");
            }


            toolWindow.getComponent().add(browser.getComponent());
        }
        else{
            System.out.println("not supported");
        }
    }
}
