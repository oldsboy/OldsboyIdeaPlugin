package com.oldsboy.ideaplugin.panel;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/** @description    就给这个获取url界面做的tableMode,没有通用性
 * @author oldsboy; @date 2022-08-08 17:55 */
public class StringTableMode extends DefaultTableModel {
    private List<String> stringList;
    private List<String> black_list;
    private String[] columns;
    private String column;
    private boolean need_selected;

    public StringTableMode(List<String> stringList, String column, boolean need_selected, List<String> black_list) {
        this.stringList = stringList;
        this.column = column;
        this.need_selected = need_selected;
        this.black_list = black_list;

        setDataVector(listToData(stringList), columns);
    }

    public String[] getColumns() {
        return columns;
    }

    public Object[][] listToData(List<String> list){
        String[] columns = {column};
        if (need_selected) {
            columns = new String[]{" ", column};
        }
        this.columns = columns;

        Object[][] data = new Object[list.size()][columns.length];
        for (int i = 0; i < list.size(); i++) {
            if (need_selected) {
                boolean is_black = false;
                if (black_list != null) for (String black_name : black_list) {
                    if ((list.get(i)).contains(black_name)) {
                        is_black = true;
                        break;
                    }
                }
                data[i][0] = !is_black;
                data[i][1] = list.get(i);
            }else {
                data[i][0] = list.get(i);
            }
        }
        return data;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (need_selected && columnIndex == 0) {
            return Boolean.class;
        }
        return String.class;
    }

    public List<String> getSelectedList() {
        Vector<Vector> vectors = getDataVector();
        ArrayList<String> result = new ArrayList<>();
        if (!need_selected) return stringList;

        for (Vector vector : vectors) {
            boolean selected = (boolean) vector.get(0);
            if (selected) {
                result.add(String.valueOf(vector.get(1)));
            }
        }

        return result;
    }

    public List<String> getList() {
        Vector<Vector> vectors = getDataVector();
        ArrayList<String> result = new ArrayList<>();

        for (Vector vector : vectors) {
            result.add(String.valueOf(vector.get(need_selected?1:0)));
        }

        return result;
    }

    public void setBlack_list(List<String> black_list) {
        this.black_list = black_list;
    }
}
