package ink.ican.jsonparse.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * @author lzq
 * 2025/7/7 17:33
 */

public class JsonFormatHtmlWindowFactory implements ToolWindowFactory {
    private static volatile File tempResourcesDir;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (JBCefApp.isSupported()) {
            JBCefBrowser browser = new JBCefBrowser();
            File resourceDir = getOrExtractResources();
            if (resourceDir != null) {
                File indexFile = new File(resourceDir, "index.html");
                if (indexFile.exists()) {
                    try {
                        browser.loadURL(indexFile.toURI().toURL().toExternalForm());
                    } catch (Exception e) {
                        browser.loadHTML("<h1>Failed to load HTML: " + e.getMessage() + "</h1>");
                    }
                } else {
                    browser.loadHTML("<h1>HTML file not found!</h1>");
                }
            } else {
                browser.loadHTML("<h1>Failed to extract resources!</h1>");
            }
            toolWindow.getComponent().add(browser.getComponent());
        } else {
            System.out.println("not supported");
        }
    }

    /**
     * 将 JAR 包内的资源文件提取到临时目录
     */
    private File getOrExtractResources() {
        if (tempResourcesDir != null && tempResourcesDir.exists()) {
            return tempResourcesDir;
        }

        try {
            // 创建临时目录
            File tempDir = Files.createTempDirectory("json-parse-resources").toFile();
            tempDir.deleteOnExit();

            // 需要提取的 HTML 文件列表
            String[] resources = {"index.html","json-format.html", "text-compare.html", "json-escape.html", "sidebar-component.js"};

            for (String resourceName : resources) {
                String resourcePath = "/" + resourceName;
                try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                    if (is != null) {
                        Path targetPath = new File(tempDir, resourceName).toPath();
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            tempResourcesDir = tempDir;
            return tempDir;
        } catch (IOException e) {
            System.err.println("Failed to extract resources: " + e.getMessage());
            return null;
        }
    }
}