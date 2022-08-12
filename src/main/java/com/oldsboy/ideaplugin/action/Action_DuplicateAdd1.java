package com.oldsboy.ideaplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.oldsboy.ideaplugin.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Action_DuplicateAdd1 extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent context) {
        super.update(context);

        //  这一段是设置按钮的显隐
//        Project project = context.getProject();
//        Editor editor = context.getData(CommonDataKeys.EDITOR);
//        context.getPresentation().setEnabledAndVisible(project != null && editor != null && editor.getSelectionModel().hasSelection());
//        System.out.println("update");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent context) {
        Editor editor = null;
        Project project = null;
        try {
            editor = context.getRequiredData(CommonDataKeys.EDITOR);
            project = context.getRequiredData(CommonDataKeys.PROJECT);
        } catch (Exception e) {
            com.intellij.openapi.ui.Messages.showMessageDialog("没有获取到Editor", "tips", Messages.getInformationIcon());
            return;
        }

        if (project == null || editor == null) {
            return;
        }

        Document document = editor.getDocument();

        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int start = primaryCaret.getSelectionStart();
        int end = primaryCaret.getSelectionEnd();
        String selectedText = primaryCaret.getSelectedText();
        CaretModel caretModel = primaryCaret.getCaretModel();

        if (StringUtil.isEmpty(selectedText)) { //  没有选择的文本则将整行的文本作为选择的文本
            LogicalPosition logicalPosition = caretModel.getLogicalPosition();

            int line = logicalPosition.line;
            int startOffset = document.getLineStartOffset(line);
            int endOffset = document.getLineEndOffset(line);
            String textFromLine = document.getText(new TextRange(startOffset, endOffset));

            selectedText = "\n"+textFromLine;

            end = endOffset;
        }

        String selectedText1 = add1(selectedText);

        int finalEnd = end;
        WriteCommandAction.runWriteCommandAction(project, () -> {
            document.insertString(finalEnd, selectedText1);
            primaryCaret.setSelection(finalEnd, finalEnd+selectedText1.length(), true);
            caretModel.moveToOffset(finalEnd+selectedText1.length());
        });


    }

    private static String add1(String origin_text) {
        Pattern pattern = Pattern.compile("[\\d\\.]+");
        Matcher matcher = pattern.matcher(origin_text);
        StringBuilder stringBuilder = new StringBuilder();
        int last_end = 0;
        while (matcher.find()) {
            String dd = matcher.group();
            String ddn = "";
            if (dd.equals(".")) continue;
            if (dd.contains(".")) { //  浮点类型的增加
                int pointAfter = dd.split("\\.")[1].length();
                String addnum = "0.";
                for (int i = 0; i < pointAfter - 1; i++) {
                    addnum+="0";
                }
                addnum+="1";
                double result = new BigDecimal(dd).add(new BigDecimal(addnum)).setScale(pointAfter).doubleValue();
                ddn = String.valueOf(result);
            }else {
                ddn = String.valueOf((Integer.parseInt(dd) + 1));
            }
            int start = origin_text.substring(last_end).indexOf(dd);
            int end = start + last_end + dd.length();

            String sp = origin_text.substring(last_end, end);
            stringBuilder.append(sp.replaceFirst(dd, ddn));
            last_end = end;
        }

        if (last_end < origin_text.length()) {
            stringBuilder.append(origin_text.substring(last_end));
        }

        return stringBuilder.toString();
    }
}
