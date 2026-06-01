package com.tbg.wms.cli.gui;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Font;
import java.util.List;
import java.util.Objects;

/**
 * Shared presenter for small contextual help dialogs.
 *
 * <p>Views own their workflow and shortcut content while this helper owns button creation,
 * formatting, and display. Keeping those responsibilities separate avoids duplicating Swing
 * dialog construction across the frame, modal dialogs, and tools.</p>
 */
public final class GuiHelpSupport {
    private static final int HELP_COLUMNS = 64;
    private static final int HELP_ROWS = 18;

    private GuiHelpSupport() {
    }

    /**
     * Creates the standard corner help button for a view or tool.
     *
     * @param owner    component used as the help dialog owner
     * @param title    user-facing view/tool title
     * @param sections help sections to display
     * @return configured help button
     */
    public static JButton createHelpButton(Component owner, String title, List<HelpSection> sections) {
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(sections, "sections cannot be null");
        JButton helpButton = new JButton("Help");
        helpButton.setFocusable(false);
        helpButton.setToolTipText("Show help for " + title);
        helpButton.addActionListener(e -> showHelp(owner, title, sections));
        return helpButton;
    }

    static String formatHelpText(List<HelpSection> sections) {
        Objects.requireNonNull(sections, "sections cannot be null");
        StringBuilder builder = new StringBuilder();
        for (HelpSection section : sections) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
            }
            builder.append(section.heading()).append(System.lineSeparator());
            for (String line : section.lines()) {
                builder.append("- ").append(line).append(System.lineSeparator());
            }
        }
        return builder.toString().stripTrailing();
    }

    private static void showHelp(Component owner, String title, List<HelpSection> sections) {
        JTextArea textArea = new JTextArea(formatHelpText(sections), HELP_ROWS, HELP_COLUMNS);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        textArea.setCaretPosition(0);
        JOptionPane.showMessageDialog(
                owner,
                new JScrollPane(textArea),
                title + " Help",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Immutable help topic section.
     *
     * @param heading section heading
     * @param lines   bullets shown under the heading
     */
    public record HelpSection(String heading, List<String> lines) {
        public HelpSection {
            Objects.requireNonNull(heading, "heading cannot be null");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines cannot be null"));
        }
    }
}
