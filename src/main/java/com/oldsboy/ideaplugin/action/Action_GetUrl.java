package com.oldsboy.ideaplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.oldsboy.ideaplugin.State;
import com.oldsboy.ideaplugin.panel.Panel_UrlListDialog;
import com.oldsboy.ideaplugin.util.StringUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Action_GetUrl extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        //  获取到当前编辑的文件
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null || psiFile.getVirtualFile() == null) {
            Messages.showMessageDialog("未获取到正在编辑的文件", "tips", Messages.getErrorIcon());
            return;
        }

        List<String> show_list = getRegexListFromFile(psiFile.getVirtualFile(), State.getInstance().getState().getBlack_list());

        showListDialog(e, show_list);
    }

    private void showListDialog(AnActionEvent e, List<String> show_list) {
        DialogBuilder dialog = new DialogBuilder(e.getProject());
        dialog.setTitle("选择需要生成的URL");
        dialog.resizable(true);

        List<String> regex_list = State.getInstance().getState().getRegex_list();
        Panel_UrlListDialog panel = new Panel_UrlListDialog(e, show_list, regex_list);
        panel.setOnConfigListener(new Panel_UrlListDialog.OnConfigListener() {
            @Override
            public void onDelete(List<String> result) {
                State.getInstance().getState().setRegex_list(result);
            }

            @Override
            public void onAdd(List<String> result) {
                State.getInstance().getState().setRegex_list(result);
            }
        });
        dialog.setCenterPanel(panel.container);

        dialog.setOkOperation(new Create_Http_From_List(e, dialog, panel));
        dialog.show();
    }

    /** @description 从文件中获取正则的字符串
     * @param file 当前编辑的文件
     * @param black_list 黑名单
     * @return java.util.List<java.lang.String> 筛选出来的字符串(url)
     * @author oldsboy; @date 2022-08-04 11:19 */
    private List<String> getRegexListFromFile(VirtualFile file, List<String> black_list) {
        List<String> show_list = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            while (reader.ready()) {
                String line = reader.readLine();
                if (StringUtil.isEmpty(line)) continue;

                // TODO: 2022/8/3 此处的正则可以考虑让用户加入,以覆盖这个正则, 考虑做个配置界面以编辑黑名单列表和正则列表
                Matcher matcher = Pattern.compile("\\w+/[(\\w+/)]+(\\?\\w+=\\w+[(\\&\\w+=\\w+)]+)?").matcher(line);
                ring_2: while (matcher.find()) {
                    String like_url = matcher.group();

                    if (StringUtil.isEmpty(like_url)) continue ring_2;

                    if(black_list != null) for (String black_str : black_list) {       //  遍历黑名单
                        if (like_url.contains(black_str)) {
                            continue ring_2;    //  跳到下一个like_url
                        }
                    }

                    //  将这个像是url的字符串加入到展示列表
                    show_list.add(like_url);
                }
            }
        } catch (IOException ex) {
            Messages.showMessageDialog("读取文件失败", "tips", Messages.getErrorIcon());
            throw new RuntimeException(ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    Messages.showMessageDialog("关闭流", "tips", Messages.getErrorIcon());
                    throw new RuntimeException(ex);
                }
            }
        }
        return show_list;
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
