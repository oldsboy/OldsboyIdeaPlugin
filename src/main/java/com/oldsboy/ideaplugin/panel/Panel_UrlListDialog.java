package com.oldsboy.ideaplugin.panel;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBTextField;
import com.oldsboy.ideaplugin.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Panel_UrlListDialog {
    public JPanel container;

    private JPanel table_container;
    private JTable table1;
    private JPanel config_container;
    private JButton btn_all;
    private JButton btn_un_all;
    private JButton btn_config_add;
    private JButton btn_config_delete;
    private JTable table2;
    private JButton btn_config_apply;
    private JTable table3;
    private JButton btn_regex_add;
    private JButton btn_regex_delete;
    private JButton btn_regex_apply;
    private JCheckBox cb_regex_keep;

    private List<String> origin_list;
    private List<String> regex_list;
    private List<String> black_list;
    private AnActionEvent context;
    private OnConfigListener onConfigListener;
    private boolean keep_self;

    public void setOnConfigListener(OnConfigListener onConfigListener) {
        this.onConfigListener = onConfigListener;
    }

    /** @description
     * @param context
     * @param regex_list
     * @param black_list
     * @param keep_self 是否使用本体的正则
     * @author oldsboy; @date 2022-08-09 15:50 */
    public Panel_UrlListDialog(AnActionEvent context, List<String> regex_list, List<String> black_list, boolean keep_self) throws Exception {
        this.regex_list = regex_list;
        this.black_list = black_list;
        this.context = context;
        this.keep_self = keep_self;

        //  首先将origin_list填充完成
        initOriginList(regex_list);

        initUrlTable();

        initBlackTable();
        
        initRegexTable();

        initClickListener();

        initCheckBox();
    }

    private void initCheckBox() {
        cb_regex_keep.setSelected(keep_self);
        cb_regex_keep.addActionListener(e -> {
            keep_self = cb_regex_keep.isSelected();

            try {
                initOriginList(regex_list);
                initUrlTable();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            // TODO: 2022/8/9 将值保存起来
            if (onConfigListener != null) {
                onConfigListener.onConfigKeepChange(keep_self);
            }
        });
    }

    private void initOriginList(List<String> regex_list) throws Exception {
        if (context == null) throw new Exception("context is null");

        //  获取到当前编辑的文件
        PsiFile psiFile = context.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null || psiFile.getVirtualFile() == null) {
            Messages.showMessageDialog("未获取到正在编辑的文件", "tips", Messages.getErrorIcon());
            return;
        }

        this.origin_list = getRegexListFromFile(psiFile.getVirtualFile().getInputStream(), regex_list);
    }

    private void initClickListener() {
        btn_all.addActionListener(e -> {
            StringTableMode model = (StringTableMode) table1.getModel();

            Object[][] origin_data = model.listToData(origin_list);
            Object[][] data = Arrays.copyOf(origin_data, origin_data.length);
            for (Object[] objects : data) {
                objects[0] = true;
            }

            model.setDataVector(data, model.getColumns());
            fixTableColumnWidth();
        });

        btn_un_all.addActionListener(e -> {
            StringTableMode model = (StringTableMode) table1.getModel();

            Object[][] origin_data = model.listToData(origin_list);
            Object[][] data = Arrays.copyOf(origin_data, origin_data.length);
            for (Object[] objects : data) {
                objects[0] = false;
            }

            model.setDataVector(data, model.getColumns());
            fixTableColumnWidth();
        });

        btn_config_add.addActionListener(e -> {
            if (context == null) {
                return;
            }
            
            DialogBuilder dialog = new DialogBuilder(context.getProject());
            dialog.setTitle("关键词");
            dialog.resizable(true);

            JBTextField jbTextField = new JBTextField();

            dialog.setCenterPanel(jbTextField);
            
            dialog.setOkOperation(() -> {
                String text = jbTextField.getText();
                if (!StringUtil.isEmpty(text)) {
                    StringTableMode model = (StringTableMode) table2.getModel();
                    Vector<Vector> dataVector = model.getDataVector();
                    List<String> list = new ArrayList<>();
                    for (Vector vector : dataVector) {
                        if (vector != null) {
                            list.add(String.valueOf(vector.get(0)));
                        }
                    }
                    list.add(text);

                    model.setDataVector(model.listToData(list), model.getColumns());

                    if (onConfigListener != null) {
                        onConfigListener.onBlackAdd(list);
                    }
                }

                dialog.getDialogWrapper().close(0);
            });
            dialog.show();
        });

        btn_config_delete.addActionListener(e -> {
            StringTableMode model = (StringTableMode) table2.getModel();
            int index = table2.getSelectedRow();
            Vector<Vector> dataVector = model.getDataVector();

            if (index != -1) dataVector.remove(dataVector.get(index));

            List<String> list = new ArrayList<>();
            for (Vector vector : dataVector) {
                if (vector != null) {
                    list.add(String.valueOf(vector.get(0)));
                }
            }

            model.setDataVector(model.listToData(list), model.getColumns());

            if (onConfigListener != null) {
                onConfigListener.onBlackDelete(list);
            }
        });

        btn_config_apply.addActionListener(e -> {
            StringTableMode model = (StringTableMode) table1.getModel();
            model.setBlack_list(((StringTableMode)table2.getModel()).getList());

            Object[][] origin_data = model.listToData(origin_list);
            Object[][] data = Arrays.copyOf(origin_data, origin_data.length);

            model.setDataVector(data, model.getColumns());
            fixTableColumnWidth();
        });

        btn_regex_add.addActionListener(e -> {
            if (context == null) {
                return;
            }

            DialogBuilder dialog = new DialogBuilder(context.getProject());
            dialog.setTitle("关键词");
            dialog.resizable(true);

            JBTextField jbTextField = new JBTextField();

            dialog.setCenterPanel(jbTextField);

            dialog.setOkOperation(() -> {
                String text = jbTextField.getText();
                if (!StringUtil.isEmpty(text)) {
                    StringTableMode model = (StringTableMode) table3.getModel();
                    Vector<Vector> dataVector = model.getDataVector();
                    List<String> list = new ArrayList<>();
                    for (Vector vector : dataVector) {
                        if (vector != null) {
                            list.add(String.valueOf(vector.get(0)));
                        }
                    }
                    list.add(text);

                    model.setDataVector(model.listToData(list), model.getColumns());

                    if (onConfigListener != null) {
                        onConfigListener.onRegexAdd(list);
                    }
                }

                dialog.getDialogWrapper().close(0);
            });
            dialog.show();
        });

        btn_regex_delete.addActionListener(e -> {
            StringTableMode model = (StringTableMode) table3.getModel();
            int index = table3.getSelectedRow();
            Vector<Vector> dataVector = model.getDataVector();

            if (index != -1) dataVector.remove(dataVector.get(index));

            List<String> list = new ArrayList<>();
            for (Vector vector : dataVector) {
                if (vector != null) {
                    list.add(String.valueOf(vector.get(0)));
                }
            }

            model.setDataVector(model.listToData(list), model.getColumns());

            if (onConfigListener != null) {
                onConfigListener.onRegexDelete(list);
            }
        });

        btn_regex_apply.addActionListener(e -> {
            if (context == null) {
                return;
            }

            StringTableMode model = (StringTableMode) table1.getModel();
            StringTableMode model3 = (StringTableMode) table3.getModel();

            try {
                initOriginList(model3.getList());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            Object[][] origin_data = model.listToData(origin_list);

            Object[][] data = Arrays.copyOf(origin_data, origin_data.length);

            model.setDataVector(data, model.getColumns());
            fixTableColumnWidth();
        });
    }

    private void initUrlTable() {
        if (origin_list == null) origin_list = new ArrayList<>();

        StringTableMode stringTableMode = new StringTableMode(origin_list, "url_list", true, black_list);
        table1.setModel(stringTableMode);
        table1.setRowHeight(30);
        table1.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        fixTableColumnWidth();
    }

    private void initBlackTable() {
        if (black_list == null) black_list = new ArrayList<>();

        StringTableMode stringTableMode = new StringTableMode(black_list, "black_list", false, null){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table2.setModel(stringTableMode);
        table2.setRowHeight(30);
        table2.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
    }

    private void initRegexTable() {
        if (regex_list == null) regex_list = new ArrayList<>();

        StringTableMode stringTableMode = new StringTableMode(regex_list, "regex_list", false, null){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table3.setModel(stringTableMode);
        table3.setRowHeight(30);
        table3.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
    }

    private void fixTableColumnWidth() {
        table1.getColumnModel().getColumn(0).setPreferredWidth(50);
        table1.getColumnModel().getColumn(1).setPreferredWidth(750);
    }

    public List<String> getSelectedList(){
        return ((StringTableMode)table1.getModel()).getSelectedList();
    }

    /** @description 
     * @param inputStream 文件流
     * @return java.util.List<java.lang.String> 自定义正则
     * @description 从文件中获取正则的字符串,增加判定,如果有自定义的就把本来的保底屏蔽了
     * @author oldsboy; @date 2022-08-04 11:19 */
    private List<String> getRegexListFromFile(@NotNull InputStream inputStream, List<String> regex_list) {
        List<String> show_list = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            while (reader.ready()) {
                String line = reader.readLine();
                if (StringUtil.isEmpty(line)) continue;

                if (keep_self) {
                    Matcher matcher = Pattern.compile("\\w+/[(\\w+/)]+([(\\?\\w+=\\&\\$\\{\\.\\}\\!)]+)?").matcher(line);
                    while (matcher.find()) {
                        String like_url = matcher.group();

                        if (StringUtil.isEmpty(like_url)) continue;

                        //  将这个像是url的字符串加入到展示列表
                        show_list.add(like_url);
                    }
                }


                for (String regex : regex_list) {
                    Matcher matcher2 = Pattern.compile(regex).matcher(line);
                    ring3: while (matcher2.find()) {
                        String like_url = matcher2.group();

                        if (StringUtil.isEmpty(like_url)) continue;

                        //  避免重复加入
                        for (String show_url : show_list) {
                            if (show_url.equals(like_url)) {
                                continue ring3;
                            }
                        }

                        //  将这个像是url的字符串加入到展示列表
                        show_list.add(like_url);
                    }
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
                    Messages.showMessageDialog("关闭流失败", "tips", Messages.getErrorIcon());
                    throw new RuntimeException(ex);
                }
            }
        }
        return show_list;
    }

    public interface OnConfigListener {
        void onBlackDelete(List<String> result);
        void onBlackAdd(List<String> result);
        void onRegexDelete(List<String> result);
        void onRegexAdd(List<String> result);
        void onConfigKeepChange(boolean isKeep);
    }
}