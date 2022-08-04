package com.oldsboy.ideaplugin.action;

import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.oldsboy.ideaplugin.State;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class Action_StorageTest extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DialogBuilder dialogBuilder = new DialogBuilder(e.getProject());
        dialogBuilder.setTitle("存储测试");

        JBPanel<JBPanel> jbPanelJBPanel = new JBPanel<>();

        List<String> regex_list = State.getInstance().getState().getRegex_list();
        JBLabel jbLabel = new JBLabel("存储数据:" + new Gson().toJson(regex_list));
        jbPanelJBPanel.add(jbLabel);

        regex_list.add(new Date().toString());
        JBLabel jbLabel2 = new JBLabel("增加数据后:" + new Gson().toJson(regex_list));
        jbPanelJBPanel.add(jbLabel2);
        State.getInstance().getState().setRegex_list(regex_list);

        dialogBuilder.setCenterPanel(jbPanelJBPanel);
        dialogBuilder.show();
    }
}
