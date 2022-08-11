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
        Document document = editor.getDocument();

        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int start = primaryCaret.getSelectionStart();
        int end = primaryCaret.getSelectionEnd();
        String selectedText = primaryCaret.getSelectedText();

        if (StringUtil.isEmpty(selectedText)) { //  没有选择的文本则将整行的文本作为选择的文本
            primaryCaret.getVisualLineStart();
            CaretModel caretModel = primaryCaret.getCaretModel();
            LogicalPosition logicalPosition = caretModel.getLogicalPosition();

            int line = logicalPosition.line;
            int startOffset = document.getLineStartOffset(line);
            int endOffset = document.getLineEndOffset(line);
            String textFromLine = document.getText(new TextRange(startOffset, endOffset));

            selectedText = "\n"+textFromLine;
        }

        String selectedText1 = add1(selectedText);

        WriteCommandAction.runWriteCommandAction(project, () -> document.insertString(end, selectedText1));

        primaryCaret.setSelection(end, end+selectedText1.length(), true);
    }

    private String add1(String text) {
        Pattern pattern = Pattern.compile("[\\d+\\.]+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String dd = matcher.group();
            String ddn = "";
            if (dd.contains(".")) { //  浮点类型的增加
                int pointAfter = dd.split("\\.")[1].length();
                double result = new BigDecimal(dd).add(new BigDecimal(1)).setScale(pointAfter).doubleValue();
                ddn = String.valueOf(result);
            }else {
                ddn = String.valueOf((Integer.parseInt(dd) + 1));
            }

            text = text.replace(dd, ddn);
        }
        return text;
    }
}
