package com.jbaplugin.dummyplugin;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TodoFactory implements ToolWindowFactory, DumbAware {

    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final CollectionListModel<Todo> todoListModel = new CollectionListModel<>();
    private final AtomicBoolean shouldSave = new AtomicBoolean(false);
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Add content to tool window
        toolWindow.getContentManager().addContent(ContentFactory.SERVICE.getInstance().createContent(contentPanel, "", false));

        // Loading placeholder
        contentPanel.add(new JLabel("Loading..."), BorderLayout.CENTER);

        // Load data
        initialize();
    }

    private void initialize() {
        executor.execute(() -> {
            // Load from file in business thread
            // It's important to not do it in EDT to not block UI
            List<Todo> todos = loadFile();

            // Update UI in EDT
            try {
                SwingUtilities.invokeAndWait(() -> {
                    todoListModel.removeAll();
                    todoListModel.add(todos);
                    buildInterface();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            // save every second if something changed
            executor.scheduleAtFixedRate(() -> {
                if (shouldSave.get()) {
                    saveFile();
                    shouldSave.set(false);
                }
            }, 1000, 1000, TimeUnit.MILLISECONDS);
        });
    }

    private void buildInterface() {
        JList<Todo> todos = buildList();
        JPanel southPanel = buildControls();
        contentPanel.removeAll();
        contentPanel.add(new JBScrollPane(todos));
        contentPanel.add(southPanel, BorderLayout.SOUTH);
        contentPanel.revalidate();
    }

    @NotNull
    private JPanel buildControls() {
        JTextField text = new JTextField();
        JButton add = new JButton("Add todo");
        add.setEnabled(false);
        AbstractAction submitAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                todoListModel.add(new Todo(text.getText()));
                text.setText("");
                shouldSave.set(true);
            }
        };
        add.addActionListener(submitAction);
        text.addActionListener(submitAction);
        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                add.setEnabled(text.getText().trim().length() > 0);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                add.setEnabled(text.getText().trim().length() > 0);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                add.setEnabled(text.getText().trim().length() > 0);
            }
        });

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BorderLayout());
        southPanel.add(text);
        southPanel.add(add, BorderLayout.EAST);
        return southPanel;
    }

    @NotNull
    private JList<Todo> buildList() {
        JList<Todo> todos = new JBList<>(todoListModel);
        todos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                    int index = todos.locationToIndex(e.getPoint());
                    Todo elementAt = todoListModel.getElementAt(index);
                    elementAt.done = !elementAt.done;
                    todoListModel.contentsChanged(elementAt);
                    shouldSave.set(true);
                } else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int index = todos.locationToIndex(e.getPoint());
                    Todo elementAt = todoListModel.getElementAt(index);
                    todoListModel.remove(elementAt);
                    shouldSave.set(true);
                }
            }
        });

        todos.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JCheckBox checkBox = new JCheckBox(value.todo);
            checkBox.setSelected(value.done);
            if (value.done) {
                checkBox.setFont(checkBox.getFont().deriveFont(Font.ITALIC));
            } else {
                checkBox.setFont(checkBox.getFont().deriveFont(Font.PLAIN));
            }
            checkBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            return checkBox;
        });
        return todos;
    }

    public List<Todo> loadFile() {
        // make following code safe
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("todos.bin"))) {
            return (List<Todo>) ois.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void saveFile() {
        // make following code safe
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("todos.bin"))) {
            oos.writeObject(todoListModel.getItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Todo implements Serializable {
        public Todo(String todo) {
            this.id = UUID.randomUUID();
            this.todo = todo;
            this.done = false;
        }

        public UUID id;
        public String todo;
        public boolean done;

        @Override
        public String toString() {
            return todo + " / " + done;
        }
    }
}
