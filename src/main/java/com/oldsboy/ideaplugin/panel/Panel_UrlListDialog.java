package com.oldsboy.ideaplugin.panel;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.ui.components.JBTextField;
import com.oldsboy.ideaplugin.util.StringUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

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

    private List<String> origin_list;
    private List<String> black_list;
    private AnActionEvent context;
    private OnConfigListener onConfigListener;

    public OnConfigListener getOnConfigListener() {
        return onConfigListener;
    }

    public void setOnConfigListener(OnConfigListener onConfigListener) {
        this.onConfigListener = onConfigListener;
    }

    public Panel_UrlListDialog(AnActionEvent context, List<String> show_list, List<String> black_list) {
        this.origin_list = show_list;
        this.black_list = black_list;
        this.context = context;

        initUrlTable();

        initBlackTable();

        initClickListener();
    }

    private void initClickListener() {
        btn_all.addActionListener(e -> {
            StringTableMode model = (StringTableMode) table1.getModel();

            Object[][] data = Arrays.copyOf(model.listToData(origin_list), model.listToData(origin_list).length);
            for (Object[] objects : data) {
                objects[0] = true;
            }

            model.setDataVector(data, model.getColumns());
            fixTableColumnWidth();
        });

        btn_un_all.addActionListener(e -> {
            StringTableMode model = (StringTableMode) table1.getModel();

            Object[][] data = Arrays.copyOf(model.listToData(origin_list), model.listToData(origin_list).length);
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
                        onConfigListener.onAdd(list);
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
                onConfigListener.onDelete(list);
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
    }

    private void initUrlTable() {
        if (origin_list == null) origin_list = new ArrayList<>();

        StringTableMode stringTableMode = new StringTableMode(origin_list, "url", true, black_list);
        table1.setModel(stringTableMode);
        table1.setRowHeight(30);
        table1.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        fixTableColumnWidth();
    }

    private void initBlackTable() {
        if (black_list == null) black_list = new ArrayList<>();

        StringTableMode stringTableMode = new StringTableMode(black_list, "key", false, null){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table2.setModel(stringTableMode);
        table2.setRowHeight(30);
        table2.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
    }

    private void fixTableColumnWidth() {
        table1.getColumnModel().getColumn(0).setPreferredWidth(50);
        table1.getColumnModel().getColumn(1).setPreferredWidth(750);
    }

    public List<String> getSelectedList(){
        return ((StringTableMode)table1.getModel()).getSelectedList();
    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("content1");
        list.add("content2");
        List<String> list2 = new ArrayList<>();
        list2.add("content3");
        list2.add("content4");
        Panel_UrlListDialog panel = new Panel_UrlListDialog(null, list, list2);

        JFrame frame = new JFrame("Panel_UrlList");
        frame.setContentPane(panel.container);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);
        frame.setSize(800, 500);
        frame.setLocationRelativeTo(panel.container);
        frame.setTitle("选择需要的url");
        frame.setVisible(true);
    }

    public interface OnConfigListener {
        void onDelete(List<String> result);
        void onAdd(List<String> result);
    }
}