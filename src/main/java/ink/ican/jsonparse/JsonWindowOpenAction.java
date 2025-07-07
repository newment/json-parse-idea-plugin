package ink.ican.jsonparse;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author lzq
 * 2025/7/3 13:43
 */

public class JsonWindowOpenAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if(project == null ){
            return ;
        }
        final ToolWindow toolWindow= ToolWindowManager.getInstance(project).getToolWindow("JsonParse");
        toolWindow.show();
    }
}
