package com.oldsboy.ideaplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.oldsboy.ideaplugin.State;
import com.oldsboy.ideaplugin.panel.Panel_UrlListDialog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Action_GetUrl extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        DialogBuilder dialog = new DialogBuilder(e.getProject());
        dialog.setTitle("选择需要生成的URL");
        dialog.resizable(true);

        List<String> black_list = State.getInstance().getState().getBlack_list();
        List<String> regex_list = State.getInstance().getState().getRegex_list();
        boolean keep_self = State.getInstance().getState().isKeep_self();
        Panel_UrlListDialog panel = null;
        try {
            panel = new Panel_UrlListDialog(e, regex_list, black_list, keep_self);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        panel.setOnConfigListener(new Panel_UrlListDialog.OnConfigListener() {
            @Override
            public void onBlackDelete(List<String> result) {
                State.getInstance().getState().setBlack_list(result);
            }

            @Override
            public void onBlackAdd(List<String> result) {
                State.getInstance().getState().setBlack_list(result);
            }

            @Override
            public void onRegexDelete(List<String> result) {
                State.getInstance().getState().setRegex_list(result);
            }

            @Override
            public void onRegexAdd(List<String> result) {
                State.getInstance().getState().setRegex_list(result);
            }

            @Override
            public void onConfigKeepChange(boolean isKeep) {
                State.getInstance().getState().setKeep_self(isKeep);
            }
        });
        dialog.setCenterPanel(panel.container);

        dialog.setOkOperation(new Create_Http_From_List(e, dialog, panel));
        dialog.show();
    }


    /** @description 根据传入的url_list生成http文件
     * @author oldsboy; @date 2022-08-04 11:49 */
    class Create_Http_From_List implements Runnable{
        private Panel_UrlListDialog panel;
        private  AnActionEvent e;
        private DialogBuilder builder;
        public Create_Http_From_List(AnActionEvent e, DialogBuilder builder, Panel_UrlListDialog panel) {
            this.e = e;
            this.builder = builder;
            this.panel = panel;
        }

        @Override
        public void run() {
            List<String> selectedList = panel.getSelectedList();
            builder.getDialogWrapper().close(0);

            if (e.getProject() == null) {
                Messages.showMessageDialog("e.getProject() is null", "tips", Messages.getErrorIcon());
                return;
            }

            String file_path = e.getProject().getBasePath()+"/http/custom_http_" + System.currentTimeMillis() + ".http";
            File http_file = new File(file_path);
            if (http_file.exists()) http_file.delete();
            http_file.getParentFile().mkdirs();
            try {
                if (http_file.createNewFile()) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(http_file));

                    writerToHttp(writer, selectedList);

                    writer.flush();
                    writer.close();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            DialogBuilder dialogBuilder = new DialogBuilder(e.getProject());
            dialogBuilder.setTitle("tips");

            JBPanel<JBPanel> jbPanelJBPanel = new JBPanel<>();
            JBLabel jbLabel = new JBLabel("生成http请求文件成功,文件路径:"+file_path);
            jbPanelJBPanel.add(jbLabel);
            JBLabel jbLabel2 = new JBLabel("点击确认跳转到http文件");
            jbPanelJBPanel.add(jbLabel2);
            dialogBuilder.setCenterPanel(jbPanelJBPanel);
            dialogBuilder.setOkOperation(() -> {
                dialogBuilder.getDialogWrapper().close(0);

                //  获取文件前需要刷新才能获取到虚拟文件
                VirtualFileManager.getInstance().syncRefresh();
                VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(http_file);
                if (virtualFile == null) {
                    Messages.showMessageDialog("找不到这个新建文件", "tips", Messages.getErrorIcon());
                    return;
                }

                FileEditorManager manager = FileEditorManager.getInstance(e.getProject());
                manager.openFile(virtualFile, true);
            });
            dialogBuilder.show();
        }

        private void writerToHttp(BufferedWriter writer, List<String> show_list) throws IOException {
            for (String url : show_list) {
                writer.newLine();
                writer.write("###");
                writer.newLine();
                writer.write("GET {{host}}"+url);
                writer.newLine();
                writer.write("Accept: application/json");
                writer.newLine();
                writer.write("Cookie: {{cookie}}");
                writer.newLine();
            }
        }
    }
}
