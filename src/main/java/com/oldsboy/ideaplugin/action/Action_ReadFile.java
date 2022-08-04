package com.oldsboy.ideaplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

public class Action_ReadFile extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DialogBuilder dialogBuilder = new DialogBuilder(e.getProject());
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        dialogBuilder.setTitle("读取文件");

        JBPanel<JBPanel> jbPanelJBPanel = new JBPanel<>();
        if (psiFile != null && psiFile.getVirtualFile() != null) {
            JBLabel jbLabel = new JBLabel("文件路径:" + psiFile.getVirtualFile().getPath());
            jbPanelJBPanel.add(jbLabel);
        }

        dialogBuilder.setCenterPanel(jbPanelJBPanel);
        dialogBuilder.show();
    }
}
