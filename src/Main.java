import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (!SystemTray.isSupported()) {
                JOptionPane.showMessageDialog(null, "SystemTray not supported on this system.");
                return;
            }
            AppSettings.loadGlobalSettings();
            NotesManager.loadNotes();
            createSystemTrayIcon();
        });
    }


    private static void createSystemTrayIcon() {
        try {
            Image trayImage = Toolkit.getDefaultToolkit().createImage(
                    Main.class.getResource("/images/app_tray_icon.png"));
            final TrayIcon trayIcon = new TrayIcon(trayImage, "Notes App");
            trayIcon.setImageAutoSize(true);

            PopupMenu popup = new PopupMenu();
            MenuItem newNoteItem = new MenuItem("New Note");
            newNoteItem.addActionListener(e -> NotesManager.createNewNote());
            popup.add(newNoteItem);

            MenuItem notesListItem = new MenuItem("Notes List");
            notesListItem.addActionListener(e -> NotesManager.showNotesList());
            popup.add(notesListItem);

            MenuItem settingsItem = new MenuItem("Settings");
            settingsItem.addActionListener(e -> GlobalSettingsWindow.showGlobalSettings());
            popup.add(settingsItem);

            MenuItem quitItem = new MenuItem("Quit");
            quitItem.addActionListener(e -> {
                NotesManager.saveNotes();
                AppSettings.saveGlobalSettings();
                System.exit(0);
            });
            popup.add(quitItem);

            trayIcon.setPopupMenu(popup);
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
}

// ----------------------------------------------------------------
// NOTES MANAGER
class NotesManager {
    private static final List<NoteWindow> NOTES = new ArrayList<>();
    private static final File NOTES_DATA_FILE = new File("notes_data.properties");

    public static void createNewNote() {
        NoteData data = new NoteData();
        data.id = UUID.randomUUID().toString();
        data.title = "Title Here";
        data.content = "";
        data.x = 100;
        data.y = 100;
        data.width = 300;
        data.height = 300;
        data.isLocked = false;
        data.alwaysOnTop = false;
        data.transparency = 1.0f;
        data.noteBackground = AppSettings.globalBgColor;
        data.toolbarColor = AppSettings.globalToolbarColor;
        data.fontFamily = AppSettings.globalFontFamily;
        data.fontSize = AppSettings.globalFontSize;
        // Set initial width and height as the lower bound for resizing
        data.minWidth = data.width;
        data.minHeight = data.height;

        NoteWindow noteWindow = new NoteWindow(data);
        NOTES.add(noteWindow);
        noteWindow.setVisible(true);
    }

    public static void showNotesList() {
        JDialog dialog = new JDialog((Frame) null, "All Notes", true);
        dialog.setLayout(new BorderLayout());
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        for (NoteWindow note : NOTES) {
            NoteData data = note.getNoteData();
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton openButton = new JButton(data.title);
            openButton.addActionListener(e -> {
                note.setVisible(true);
                note.toFront();
                dialog.dispose();
            });
            JButton deleteButton = new JButton("Delete");
            deleteButton.addActionListener(e -> {
                int result = JOptionPane.showConfirmDialog(dialog,
                        "Are you sure you want to delete this note?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    NotesManager.deleteNote(note);
                    dialog.dispose();
                    showNotesList();
                }
            });
            rowPanel.add(openButton);
            rowPanel.add(deleteButton);
            listPanel.add(rowPanel);
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setSize(300, 300);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

public static void saveNotes() {
    Properties props = new Properties();
    int index = 0;

    // Ensure the RTF folder exists
    File rtfFolder = new File(NOTES_DATA_FILE.getParent(), "notes_rtf");
    if (!rtfFolder.exists()) {
        rtfFolder.mkdirs();
    }

    for (NoteWindow note : NOTES) {
        NoteData data = note.getNoteData();
        String prefix = "note." + index + ".";
        props.setProperty(prefix + "id", data.id);
        props.setProperty(prefix + "title", data.title);
        props.setProperty(prefix + "x", String.valueOf(data.x));
        props.setProperty(prefix + "y", String.valueOf(data.y));
        props.setProperty(prefix + "width", String.valueOf(data.width));
        props.setProperty(prefix + "height", String.valueOf(data.height));
        props.setProperty(prefix + "locked", String.valueOf(data.isLocked));
        props.setProperty(prefix + "ontop", String.valueOf(data.alwaysOnTop));
        props.setProperty(prefix + "transparency", String.valueOf(data.transparency));
        props.setProperty(prefix + "noteBackground", String.valueOf(data.noteBackground.getRGB()));
        props.setProperty(prefix + "toolbarColor", String.valueOf(data.toolbarColor.getRGB()));
        props.setProperty(prefix + "fontFamily", data.fontFamily);
        props.setProperty(prefix + "fontSize", String.valueOf(data.fontSize));
        props.setProperty(prefix + "minWidth", String.valueOf(data.minWidth));
        props.setProperty(prefix + "minHeight", String.valueOf(data.minHeight));

        // Save styled content to a separate file in the "notes_rtf" folder
        File styledContentFile = new File(rtfFolder, data.id + ".rtf");
        try (FileOutputStream fos = new FileOutputStream(styledContentFile)) {
            RTFEditorKit rtfEditorKit = new RTFEditorKit();
            rtfEditorKit.write(fos, note.getStyledDocument(), 0, note.getStyledDocument().getLength());
        } catch (IOException | BadLocationException e) {
            e.printStackTrace();
        }
        props.setProperty(prefix + "contentFile", styledContentFile.getName());
        index++;
    }
    props.setProperty("count", String.valueOf(index));

    try (FileOutputStream fos = new FileOutputStream(NOTES_DATA_FILE)) {
        props.store(fos, "Notes Data");
    } catch (IOException e) {
        e.printStackTrace();
    }
}

public static void loadNotes() {
    if (!NOTES_DATA_FILE.exists()) return;
    Properties props = new Properties();
    try (FileInputStream fis = new FileInputStream(NOTES_DATA_FILE)) {
        props.load(fis);
    } catch (IOException e) {
        e.printStackTrace();
        return;
    }

    // Ensure the RTF folder exists
    File rtfFolder = new File(NOTES_DATA_FILE.getParent(), "notes_rtf");
    if (!rtfFolder.exists()) {
        rtfFolder.mkdirs();
    }

    int count = Integer.parseInt(props.getProperty("count", "0"));
    for (int i = 0; i < count; i++) {
        String prefix = "note." + i + ".";
        NoteData data = new NoteData();
        data.id = props.getProperty(prefix + "id");
        data.title = props.getProperty(prefix + "title");
        data.x = Integer.parseInt(props.getProperty(prefix + "x"));
        data.y = Integer.parseInt(props.getProperty(prefix + "y"));
        data.width = Integer.parseInt(props.getProperty(prefix + "width"));
        data.height = Integer.parseInt(props.getProperty(prefix + "height"));
        data.isLocked = Boolean.parseBoolean(props.getProperty(prefix + "locked"));
        data.alwaysOnTop = Boolean.parseBoolean(props.getProperty(prefix + "ontop"));
        data.transparency = Float.parseFloat(props.getProperty(prefix + "transparency"));
        data.noteBackground = new Color(Integer.parseInt(props.getProperty(prefix + "noteBackground")));
        data.toolbarColor = new Color(Integer.parseInt(props.getProperty(prefix + "toolbarColor")));
        data.fontFamily = props.getProperty(prefix + "fontFamily");
        data.fontSize = Integer.parseInt(props.getProperty(prefix + "fontSize"));
        data.minWidth = Integer.parseInt(props.getProperty(prefix + "minWidth", String.valueOf(data.width)));
        data.minHeight = Integer.parseInt(props.getProperty(prefix + "minHeight", String.valueOf(data.height)));

        NoteWindow noteWindow = new NoteWindow(data);

        // Load styled content from the file in the "notes_rtf" folder
        String contentFileName = props.getProperty(prefix + "contentFile");
        if (contentFileName != null) {
            File styledContentFile = new File(rtfFolder, contentFileName);
            try (FileInputStream fis = new FileInputStream(styledContentFile)) {
                RTFEditorKit rtfEditorKit = new RTFEditorKit();
                rtfEditorKit.read(fis, noteWindow.getStyledDocument(), 0);
            } catch (IOException | BadLocationException e) {
                e.printStackTrace();
            }
        }

        NOTES.add(noteWindow);
        noteWindow.setVisible(true);
    }
}

    public static void deleteNote(NoteWindow noteWindow) {
        NOTES.remove(noteWindow);
    
        // Delete the associated RTF file
        NoteData data = noteWindow.getNoteData();
        File rtfFolder = new File(NOTES_DATA_FILE.getParent(), "notes_rtf");
        File styledContentFile = new File(rtfFolder, data.id + ".rtf");
        if (styledContentFile.exists()) {
            if (!styledContentFile.delete()) {
                System.err.println("Failed to delete RTF file: " + styledContentFile.getAbsolutePath());
            }
        }
    
        noteWindow.dispose();
        saveNotes();
    }
}

// ----------------------------------------------------------------
// NOTE DATA MODEL (with minWidth and minHeight to enforce lower bound)
class NoteData {
    public String id;
    public String title;
    public String content;
    public int x;
    public int y;
    public int width;
    public int height;
    public boolean isLocked;
    public boolean alwaysOnTop;
    public float transparency;
    public Color noteBackground;
    public Color toolbarColor;
    public String fontFamily;
    public int fontSize;
    public int minWidth;
    public int minHeight;
}

// ----------------------------------------------------------------
// NOTE WINDOW
class NoteWindow extends JFrame {
    private static final int TOOLBAR_HEIGHT = 30;
    private NoteData noteData;

    private JTextField titleField;
    private JTextPane notePane;
    private JPanel toolbarPanel;

    // Icon labels and separators
    private JLabel closeLabel, settingsLabel, onTopLabel, lockLabel;
    private JLabel boldLabel, italicLabel, bulletLabel;
    private JLabel fontLabel, colorLabel, fontSizeLabel;
    private JLabel dragCornerLabel;
    private JLabel sep1, sep2;

    // Toggle icons for states
    private ImageIcon lockIcon, unlockIcon;
    private ImageIcon onTopIcon, normalIcon;

    // Typing mode toggles for bold/italic/font/color/size
    private boolean typingBold = false;
    private boolean typingItalic = false;
    private boolean typingFont = false;
    private boolean typingColor = false;
    private boolean typingFontSize = false;
    private Color currentTypingColor = Color.BLACK;
    private String currentTypingFont = "Arial";
    private int currentTypingFontSize;

    // Resizing support
    private boolean resizing = false;
    private int resizeEdge = 0; // 1=left,2=right,4=top,8=bottom
    private Point initialMousePos;
    private Rectangle initialBounds;

    // For dragging via corner
    private Point cornerDragInitialScreen;

    public NoteWindow(NoteData data) {
        super();
        setIconImage(Toolkit.getDefaultToolkit().createImage(
                getClass().getResource("/images/app_icon.png")));
        this.noteData = data;
        currentTypingFontSize = noteData.fontSize;
        setUndecorated(true);
        setLayout(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setBounds(data.x, data.y, data.width, data.height);
        setAlwaysOnTop(data.alwaysOnTop);
        setBackground(new Color(0, 0, 0, 0));
        getRootPane().setDoubleBuffered(true);
        initComponents();
        initListeners();
        applyTypingAttributes();
        // Round the corners of the window
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
    }

    public NoteData getNoteData() {
        return noteData;
    }

    public StyledDocument getStyledDocument() {
        return notePane.getStyledDocument();
    }

    private void initComponents() {
        JPanel contentPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, noteData.transparency));
                g2.setColor(noteData.noteBackground);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        contentPanel.setOpaque(false);
        setContentPane(contentPanel);

        toolbarPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, noteData.transparency));
                g2.setColor(noteData.toolbarColor);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        toolbarPanel.setBounds(0, 0, getWidth(), TOOLBAR_HEIGHT);

        // Load toggle icons from resources
        lockIcon = scaleIcon("/images/lock.png", 20, 20);
        unlockIcon = scaleIcon("/images/unlock.png", 20, 20);
        onTopIcon = scaleIcon("/images/layers.png", 20, 20);
        normalIcon = scaleIcon("/images/layers_off.png", 20, 20);

        // Create icon labels (all scaled to 20x20 except the drag corner)
        closeLabel = createIconLabel("/images/close.png", "Hide", 20, 20);
        settingsLabel = createIconLabel("/images/cog.png", "Note Settings", 20, 20);
        onTopLabel = new JLabel(noteData.alwaysOnTop ? onTopIcon : normalIcon);
        onTopLabel.setToolTipText("Toggle Always On Top");
        onTopLabel.setBounds(0, 0, 20, 20);
        lockLabel = new JLabel(noteData.isLocked ? lockIcon : unlockIcon);
        lockLabel.setToolTipText("Lock/Unlock");
        lockLabel.setBounds(0, 0, 20, 20);
        boldLabel = createIconLabel("/images/bold.png", "Bold", 20, 20);
        italicLabel = createIconLabel("/images/italic.png", "Italic", 20, 20);
        bulletLabel = createIconLabel("/images/bullet.png", "Bullet", 20, 20);
        fontLabel = createIconLabel("/images/font.png", "Change Font", 20, 20);
        colorLabel = createIconLabel("/images/color.png", "Change Text Color", 20, 20);
        fontSizeLabel = createIconLabel("/images/font_size.png", "Change Font Size", 20, 20);

        dragCornerLabel = createIconLabel("/images/drag_triangle.png", "Drag corner", 25, 30);

        // Vertical separators
        sep1 = createVerticalLine();
        sep2 = createVerticalLine();

        int xPos = 5;
        int iconSize = 20;
        closeLabel.setBounds(xPos, 5, iconSize, iconSize);
        toolbarPanel.add(closeLabel);
        xPos += iconSize + 5;

        sep1.setBounds(xPos, 5, 1, iconSize);
        toolbarPanel.add(sep1);
        xPos += 6;

        settingsLabel.setBounds(xPos, 5, iconSize, iconSize);
        toolbarPanel.add(settingsLabel);
        xPos += iconSize + 5;

        onTopLabel.setBounds(xPos, 5, iconSize, iconSize);
        toolbarPanel.add(onTopLabel);
        xPos += iconSize + 5;

        lockLabel.setBounds(xPos, 5, iconSize, iconSize);
        toolbarPanel.add(lockLabel);
        xPos += iconSize + 5;

        sep2.setBounds(xPos, 5, 1, iconSize);
        toolbarPanel.add(sep2);
        xPos += 6;

        boldLabel.setBounds(xPos, 5, iconSize, iconSize);
        toolbarPanel.add(boldLabel);
        xPos += iconSize + 5;

        italicLabel.setBounds(xPos, 5, iconSize, iconSize);
        toolbarPanel.add(italicLabel);
        xPos += iconSize + 5;

        bulletLabel.setBounds(xPos, 5, iconSize, iconSize);
        toolbarPanel.add(bulletLabel);
        xPos += iconSize + 5;

        fontLabel.setBounds(xPos, 5, iconSize, iconSize);
        toolbarPanel.add(fontLabel);
        xPos += iconSize + 5;

        colorLabel.setBounds(xPos, 5, iconSize, iconSize);
        toolbarPanel.add(colorLabel);
        xPos += iconSize + 5;

        fontSizeLabel.setBounds(xPos, 5, iconSize, iconSize);
        toolbarPanel.add(fontSizeLabel);
        xPos += iconSize + 5;

        dragCornerLabel.setBounds(getWidth() - 25, 0, 25, TOOLBAR_HEIGHT);
        toolbarPanel.add(dragCornerLabel);

        // Title field and separator line
        titleField = new JTextField(noteData.title);
        titleField.setBorder(null);
        titleField.setFont(new Font(noteData.fontFamily, Font.PLAIN, noteData.fontSize));
        titleField.setOpaque(false);
        titleField.setBounds(0, TOOLBAR_HEIGHT, getWidth(), 30);
        JLabel titleSeparator = new JLabel();
        titleSeparator.setOpaque(true);
        titleSeparator.setBackground(Color.DARK_GRAY);
        titleSeparator.setBounds(0, TOOLBAR_HEIGHT + 30, getWidth(), 1);

        // Note text pane with scrollpane (hidden scrollbars but scrolling works)
        notePane = new JTextPane();
        notePane.setText(noteData.content);
        notePane.setFont(new Font(noteData.fontFamily, Font.PLAIN, noteData.fontSize));
        notePane.setOpaque(false);
        notePane.setEditable(!noteData.isLocked);
        JScrollPane scrollPane = new JScrollPane(notePane);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        styleScrollBar(scrollPane);
        scrollPane.setBounds(0, TOOLBAR_HEIGHT + 31, getWidth(), getHeight() - TOOLBAR_HEIGHT - 31);

        contentPanel.add(toolbarPanel);
        contentPanel.add(titleField);
        contentPanel.add(titleSeparator);
        contentPanel.add(scrollPane);

        toolbarPanel.setVisible(false);
    }

    private void initListeners() {
        // Close
        closeLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                saveState();
                setVisible(false);
            }
        });
        // Settings
        settingsLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                NoteSettingsWindow.showNoteSettings(NoteWindow.this);
            }
        });
        // Toggle OnTop
        onTopLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                noteData.alwaysOnTop = !noteData.alwaysOnTop;
                setAlwaysOnTop(noteData.alwaysOnTop);
                onTopLabel.setIcon(noteData.alwaysOnTop ? onTopIcon : normalIcon);
                NotesManager.saveNotes();
            }
        });
        // Toggle Lock
        lockLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                noteData.isLocked = !noteData.isLocked;
                notePane.setEditable(!noteData.isLocked);
                lockLabel.setIcon(noteData.isLocked ? lockIcon : unlockIcon);
                NotesManager.saveNotes();
            }
        });
        // Bold: single-click for selection; double-click toggles typing mode
        boldLabel.addMouseListener(new MouseAdapter() {
            long lastClickTime = 0;
            public void mouseClicked(MouseEvent e) {
                long now = System.currentTimeMillis();
                if (now - lastClickTime < 300) {
                    typingBold = !typingBold;
                    applyTypingAttributes();
                } else {
                    boldSelectedText();
                }
                lastClickTime = now;
            }
        });
        // Italic: similar behavior
        italicLabel.addMouseListener(new MouseAdapter() {
            long lastClickTime = 0;
            public void mouseClicked(MouseEvent e) {
                long now = System.currentTimeMillis();
                if (now - lastClickTime < 300) {
                    typingItalic = !typingItalic;
                    applyTypingAttributes();
                } else {
                    italicSelectedText();
                }
                lastClickTime = now;
            }
        });
        // Bullet
        bulletLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                notePane.replaceSelection("\u2022 ");
            }
        });
        // Font: single-click applies to selection; double-click toggles typing mode
        fontLabel.addMouseListener(new MouseAdapter() {
            long lastClickTime = 0;
            public void mouseClicked(MouseEvent e) {
                long now = System.currentTimeMillis();
                if (now - lastClickTime < 300) {
                    typingFont = !typingFont;
                    if (typingFont) chooseAndApplyFont(null);
                    applyTypingAttributes();
                } else {
                    chooseAndApplyFont("selection");
                }
                lastClickTime = now;
            }
        });
        // Color: similar behavior
        colorLabel.addMouseListener(new MouseAdapter() {
            long lastClickTime = 0;
            public void mouseClicked(MouseEvent e) {
                long now = System.currentTimeMillis();
                if (now - lastClickTime < 300) {
                    typingColor = !typingColor;
                    if (typingColor) chooseAndApplyColor(null);
                    applyTypingAttributes();
                } else {
                    chooseAndApplyColor("selection");
                }
                lastClickTime = now;
            }
        });
        // Font Size: single-click applies to selection; double-click toggles typing mode
        fontSizeLabel.addMouseListener(new MouseAdapter() {
            long lastClickTime = 0;
            public void mouseClicked(MouseEvent e) {
                long now = System.currentTimeMillis();
                if (now - lastClickTime < 300) {
                    typingFontSize = !typingFontSize;
                    if (typingFontSize) chooseAndApplyFontSize(null);
                    applyTypingAttributes();
                } else {
                    chooseAndApplyFontSize("selection");
                }
                lastClickTime = now;
            }
        });
        // Save state on title/text changes
        titleField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { saveState(); }
            public void removeUpdate(DocumentEvent e) { saveState(); }
            public void changedUpdate(DocumentEvent e) { saveState(); }
        });
        notePane.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { saveState(); }
            public void removeUpdate(DocumentEvent e) { saveState(); }
            public void changedUpdate(DocumentEvent e) { saveState(); }
        });
        // Show/hide toolbar on hover
        getContentPane().addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) { toolbarPanel.setVisible(true); }
        });
        getContentPane().addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), NoteWindow.this);
                if (!new Rectangle(0, 0, getWidth(), getHeight()).contains(p))
                    toolbarPanel.setVisible(false);
            }
        });
        // Draggable corner
        dragCornerLabel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { cornerDragInitialScreen = e.getLocationOnScreen(); }
            public void mouseReleased(MouseEvent e) { cornerDragInitialScreen = null; saveState(); }
        });
        dragCornerLabel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (cornerDragInitialScreen != null) {
                    int thisX = getX(), thisY = getY();
                    Point current = e.getLocationOnScreen();
                    int movedX = current.x - cornerDragInitialScreen.x;
                    int movedY = current.y - cornerDragInitialScreen.y;
                    setLocation(thisX + movedX, thisY + movedY);
                    cornerDragInitialScreen = current;
                }
            }
        });
        // Edge resizing
        getContentPane().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                resizeEdge = getResizeEdge(e.getPoint());
                if (resizeEdge != 0) {
                    resizing = true;
                    initialMousePos = e.getLocationOnScreen();
                    initialBounds = getBounds();
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (resizing) {
                    resizing = false;
                    saveState();
                }
            }
        });
        getContentPane().addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (resizing) {
                    Point current = e.getLocationOnScreen();
                    int deltaX = current.x - initialMousePos.x;
                    int deltaY = current.y - initialMousePos.y;
                    Rectangle newBounds = new Rectangle(initialBounds);
                    if ((resizeEdge & 1) != 0) { newBounds.x += deltaX; newBounds.width -= deltaX; }
                    if ((resizeEdge & 2) != 0) { newBounds.width += deltaX; }
                    if ((resizeEdge & 4) != 0) { newBounds.y += deltaY; newBounds.height -= deltaY; }
                    if ((resizeEdge & 8) != 0) { newBounds.height += deltaY; }
                    if (newBounds.width < noteData.minWidth) newBounds.width = noteData.minWidth;
                    if (newBounds.height < noteData.minHeight) newBounds.height = noteData.minHeight;
                    setBounds(newBounds);
                    layoutComponents();
                }
            }
        });
        addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) { saveState(); }
            public void componentResized(ComponentEvent e) { saveState(); layoutComponents(); }
        });
    }

    private int getResizeEdge(Point p) {
        int margin = 5;
        int w = getWidth(), h = getHeight();
        boolean left = (p.x < margin);
        boolean right = (p.x > w - margin);
        boolean top = (p.y < margin);
        boolean bottom = (p.y > h - margin);
        int edgeMask = 0;
        if (left) edgeMask |= 1;
        if (right) edgeMask |= 2;
        if (top) edgeMask |= 4;
        if (bottom) edgeMask |= 8;
        return edgeMask;
    }

    public void layoutComponents() {
        toolbarPanel.setBounds(0, 0, getWidth(), TOOLBAR_HEIGHT);
        titleField.setBounds(0, TOOLBAR_HEIGHT, getWidth(), 30);
        getContentPane().getComponent(2).setBounds(0, TOOLBAR_HEIGHT + 30, getWidth(), 1);
        int textPaneHeight = getHeight() - TOOLBAR_HEIGHT - 31;
        if (textPaneHeight < 0) textPaneHeight = 0;
        getContentPane().getComponent(3).setBounds(0, TOOLBAR_HEIGHT + 31, getWidth(), textPaneHeight);
        dragCornerLabel.setBounds(getWidth() - 25, 0, 25, TOOLBAR_HEIGHT);
        repaint();
    }

    private void saveState() {
        noteData.x = getX();
        noteData.y = getY();
        noteData.width = getWidth();
        noteData.height = getHeight();
        noteData.title = titleField.getText();
        noteData.content = notePane.getText();
        NotesManager.saveNotes();
    }

    private void boldSelectedText() {
        StyledDocument doc = notePane.getStyledDocument();
        int start = notePane.getSelectionStart(), end = notePane.getSelectionEnd();
        if (start < end) {
            MutableAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setBold(attr, true);
            doc.setCharacterAttributes(start, end - start, attr, false);
        }
    }

    private void italicSelectedText() {
        StyledDocument doc = notePane.getStyledDocument();
        int start = notePane.getSelectionStart(), end = notePane.getSelectionEnd();
        if (start < end) {
            MutableAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setItalic(attr, true);
            doc.setCharacterAttributes(start, end - start, attr, false);
        }
    }

    private void applyTypingAttributes() {
        StyledEditorKit kit = (StyledEditorKit) notePane.getEditorKit();
        MutableAttributeSet inputAttrs = kit.getInputAttributes();
        StyleConstants.setBold(inputAttrs, false);
        StyleConstants.setItalic(inputAttrs, false);
        StyleConstants.setForeground(inputAttrs, Color.BLACK);
        StyleConstants.setFontFamily(inputAttrs, noteData.fontFamily);
        StyleConstants.setFontSize(inputAttrs, noteData.fontSize);
        if (typingBold) StyleConstants.setBold(inputAttrs, true);
        if (typingItalic) StyleConstants.setItalic(inputAttrs, true);
        if (typingColor && currentTypingColor != null) StyleConstants.setForeground(inputAttrs, currentTypingColor);
        if (typingFont && currentTypingFont != null) StyleConstants.setFontFamily(inputAttrs, currentTypingFont);
        if (typingFontSize) StyleConstants.setFontSize(inputAttrs, currentTypingFontSize);
        notePane.setCharacterAttributes(inputAttrs, false);
    }

    private void chooseAndApplyFont(String mode) {
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        String newFont = (String) JOptionPane.showInputDialog(this, "Choose a font:", "Font",
                JOptionPane.PLAIN_MESSAGE, null, fonts, currentTypingFont);
        if (newFont == null) return;
        if ("selection".equals(mode)) {
            int start = notePane.getSelectionStart(), end = notePane.getSelectionEnd();
            if (start < end) {
                StyledDocument doc = notePane.getStyledDocument();
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setFontFamily(attr, newFont);
                doc.setCharacterAttributes(start, end - start, attr, false);
            }
        } else {
            currentTypingFont = newFont;
            applyTypingAttributes();
        }
    }

    private void chooseAndApplyColor(String mode) {
        Color chosen = JColorChooser.showDialog(this, "Choose Text Color", currentTypingColor);
        if (chosen == null) return;
        if ("selection".equals(mode)) {
            int start = notePane.getSelectionStart(), end = notePane.getSelectionEnd();
            if (start < end) {
                StyledDocument doc = notePane.getStyledDocument();
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setForeground(attr, chosen);
                doc.setCharacterAttributes(start, end - start, attr, false);
            }
        } else {
            currentTypingColor = chosen;
            applyTypingAttributes();
        }
    }

    private void chooseAndApplyFontSize(String mode) {
        String input = JOptionPane.showInputDialog(this, "Enter font size (e.g., 12):", currentTypingFontSize);
        if (input == null) return;
        try {
            int newSize = Integer.parseInt(input.trim());
            if ("selection".equals(mode)) {
                int start = notePane.getSelectionStart(), end = notePane.getSelectionEnd();
                if (start < end) {
                    StyledDocument doc = notePane.getStyledDocument();
                    MutableAttributeSet attr = new SimpleAttributeSet();
                    StyleConstants.setFontSize(attr, newSize);
                    doc.setCharacterAttributes(start, end - start, attr, false);
                }
            } else {
                currentTypingFontSize = newSize;
                applyTypingAttributes();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid font size.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel createIconLabel(String path, String tooltip, int w, int h) {
        JLabel label = new JLabel();
        label.setToolTipText(tooltip);
        label.setIcon(scaleIcon(path, w, h));
        return label;
    }

    private ImageIcon scaleIcon(String path, int w, int h) {
        ImageIcon rawIcon = new ImageIcon(getClass().getResource(path));
        Image scaled = rawIcon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private JLabel createVerticalLine() {
        JLabel line = new JLabel();
        line.setOpaque(true);
        line.setBackground(Color.BLACK);
        return line;
    }

    private void styleScrollBar(JScrollPane scrollPane) {
        JScrollBar vBar = scrollPane.getVerticalScrollBar();
        JScrollBar hBar = scrollPane.getHorizontalScrollBar();
        vBar.setUI(new CustomScrollBarUI());
        hBar.setUI(new CustomScrollBarUI());
    }

    private static class CustomScrollBarUI extends BasicScrollBarUI {
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
        }
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
    }
}

// ----------------------------------------------------------------
// NOTE SETTINGS WINDOW
class NoteSettingsWindow {
    public static void showNoteSettings(NoteWindow noteWindow) {
        NoteData data = noteWindow.getNoteData();
        JDialog dialog = new JDialog(noteWindow, "Note Settings", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5,5,5,5);

        // Background color
        JLabel bgLabel = new JLabel("Background Color:");
        dialog.add(bgLabel, gbc);
        gbc.gridx = 1;
        JButton bgButton = new JButton("Change");
        bgButton.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(dialog, "Choose Background Color", data.noteBackground);
            if (chosen != null) {
                data.noteBackground = chosen;
                noteWindow.repaint();
                NotesManager.saveNotes();
            }
        });
        dialog.add(bgButton, gbc);

        // Toolbar color
        gbc.gridx = 0; gbc.gridy++;
        JLabel tbLabel = new JLabel("Toolbar Color:");
        dialog.add(tbLabel, gbc);
        gbc.gridx = 1;
        JButton tbButton = new JButton("Change");
        tbButton.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(dialog, "Choose Toolbar Color", data.toolbarColor);
            if (chosen != null) {
                data.toolbarColor = chosen;
                noteWindow.repaint();
                NotesManager.saveNotes();
            }
        });
        dialog.add(tbButton, gbc);

        // Transparency
        gbc.gridx = 0; gbc.gridy++;
        JLabel trLabel = new JLabel("Transparency (5% - 100%):");
        dialog.add(trLabel, gbc);
        gbc.gridx = 1;
        JSlider transparencySlider = new JSlider(5, 100, (int)(data.transparency * 100));
        transparencySlider.addChangeListener(e -> {
            data.transparency = transparencySlider.getValue() / 100f;
            noteWindow.repaint();
            NotesManager.saveNotes();
        });
        dialog.add(transparencySlider, gbc);

        // Width: change note size immediately (cannot go below default of 300)
        gbc.gridx = 0; gbc.gridy++;
        JLabel widthLabel = new JLabel("Width:");
        dialog.add(widthLabel, gbc);
        gbc.gridx = 1;
        SpinnerNumberModel widthModel = new SpinnerNumberModel(data.width, 300, 2000, 10);
        JSpinner widthSpinner = new JSpinner(widthModel);
        widthSpinner.addChangeListener(e -> {
            int newWidth = (int) widthSpinner.getValue();
            data.width = newWidth;
            data.minWidth = newWidth; // update lower bound for edge-resizing
            noteWindow.setSize(newWidth, noteWindow.getHeight());
            noteWindow.layoutComponents();
            NotesManager.saveNotes();
        });
        dialog.add(widthSpinner, gbc);

        // Height: change note size immediately (cannot go below default of 300)
        gbc.gridx = 0; gbc.gridy++;
        JLabel heightLabel = new JLabel("Height:");
        dialog.add(heightLabel, gbc);
        gbc.gridx = 1;
        SpinnerNumberModel heightModel = new SpinnerNumberModel(data.height, 300, 2000, 10);
        JSpinner heightSpinner = new JSpinner(heightModel);
        heightSpinner.addChangeListener(e -> {
            int newHeight = (int) heightSpinner.getValue();
            data.height = newHeight;
            data.minHeight = newHeight; // update lower bound for edge-resizing
            noteWindow.setSize(noteWindow.getWidth(), newHeight);
            noteWindow.layoutComponents();
            NotesManager.saveNotes();
        });
        dialog.add(heightSpinner, gbc);

        // Delete note button
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        JButton deleteButton = new JButton("Delete This Note");
        deleteButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(dialog, "Are you sure you want to delete this note?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                NotesManager.deleteNote(noteWindow);
                dialog.dispose();
            }
        });
        dialog.add(deleteButton, gbc);
        dialog.getContentPane().setBackground(new Color(50, 50, 50));
        dialog.getContentPane().setForeground(Color.WHITE);
        for (Component c : dialog.getContentPane().getComponents()) {
            c.setBackground(new Color(50, 50, 50));
            c.setForeground(Color.WHITE);
        }

        dialog.pack();
        dialog.setLocationRelativeTo(noteWindow);
        dialog.setVisible(true);
    }
}

// ----------------------------------------------------------------
// GLOBAL SETTINGS WINDOW
class GlobalSettingsWindow {
    public static void showGlobalSettings() {
        JDialog dialog = new JDialog((Frame) null, "Global Settings", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5,5,5,5);

        JLabel bgLabel = new JLabel("Default Note Background:");
        dialog.add(bgLabel, gbc);
        gbc.gridx = 1;
        JButton bgButton = new JButton("Change");
        bgButton.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(dialog, "Choose Default Note Background", AppSettings.globalBgColor);
            if (chosen != null) {
                AppSettings.globalBgColor = chosen;
                AppSettings.saveGlobalSettings();
            }
        });
        dialog.add(bgButton, gbc);

        gbc.gridx = 0; gbc.gridy++;
        JLabel tbLabel = new JLabel("Default Toolbar Color:");
        dialog.add(tbLabel, gbc);
        gbc.gridx = 1;
        JButton tbButton = new JButton("Change");
        tbButton.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(dialog, "Choose Default Toolbar Color", AppSettings.globalToolbarColor);
            if (chosen != null) {
                AppSettings.globalToolbarColor = chosen;
                AppSettings.saveGlobalSettings();
            }
        });
        dialog.add(tbButton, gbc);

        gbc.gridx = 0; gbc.gridy++;
        JLabel fontLabel = new JLabel("Default Font Family:");
        dialog.add(fontLabel, gbc);
        gbc.gridx = 1;
        JComboBox<String> fontCombo = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontCombo.setSelectedItem(AppSettings.globalFontFamily);
        fontCombo.addActionListener(e -> {
            AppSettings.globalFontFamily = (String) fontCombo.getSelectedItem();
            AppSettings.saveGlobalSettings();
        });
        dialog.add(fontCombo, gbc);

        gbc.gridx = 0; gbc.gridy++;
        JLabel fontSizeLabel = new JLabel("Default Font Size:");
        dialog.add(fontSizeLabel, gbc);
        gbc.gridx = 1;
        SpinnerNumberModel model = new SpinnerNumberModel(AppSettings.globalFontSize, 8, 72, 1);
        JSpinner fontSizeSpinner = new JSpinner(model);
        fontSizeSpinner.addChangeListener(e -> {
            AppSettings.globalFontSize = (int) fontSizeSpinner.getValue();
            AppSettings.saveGlobalSettings();
        });
        dialog.add(fontSizeSpinner, gbc);

        gbc.gridx = 0; gbc.gridy++;
        JLabel storageLabel = new JLabel("Data Storage Location:");
        dialog.add(storageLabel, gbc);
        gbc.gridx = 1;
        JTextField storageField = new JTextField(AppSettings.dataStorageLocation.getAbsolutePath(), 15);
        storageField.setEditable(false);
        dialog.add(storageField, gbc);
        gbc.gridx = 2;
        JButton changeLocButton = new JButton("Change...");
        changeLocButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                AppSettings.dataStorageLocation = chooser.getSelectedFile();
                storageField.setText(AppSettings.dataStorageLocation.getAbsolutePath());
                AppSettings.saveGlobalSettings();
            }
        });
        dialog.add(changeLocButton, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 3;
        JLabel versionLabel = new JLabel("Version: 1.0.0    Developer: Dominic Minnich");
        dialog.add(versionLabel, gbc);
        gbc.gridy++;
        JButton githubLink = new JButton("View GitHub Repo");
        githubLink.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new URI("https://github.com/YourRepoHere")); }
            catch (Exception ex) { ex.printStackTrace(); }
        });
        dialog.add(githubLink, gbc);

        dialog.getContentPane().setBackground(new Color(50, 50, 50));
        dialog.getContentPane().setForeground(Color.WHITE);
        for (Component c : dialog.getContentPane().getComponents()) {
            c.setBackground(new Color(50, 50, 50));
            c.setForeground(Color.WHITE);
        }

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}

// ----------------------------------------------------------------
// GLOBAL APP SETTINGS
class AppSettings {
    private static final File GLOBAL_SETTINGS_FILE = new File("global_settings.properties");
    public static Color globalBgColor = new Color(255, 255, 224, 255);
    public static Color globalToolbarColor = new Color(204, 229, 241, 255);
    public static String globalFontFamily = "Arial";
    public static int globalFontSize = 14;
    public static File dataStorageLocation = new File(".");

    public static void loadGlobalSettings() {
        if (!GLOBAL_SETTINGS_FILE.exists()) return;
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(GLOBAL_SETTINGS_FILE)) {
            props.load(fis);
            globalBgColor = new Color(Integer.parseInt(props.getProperty("globalBgColor", String.valueOf(globalBgColor.getRGB()))));
            globalToolbarColor = new Color(Integer.parseInt(props.getProperty("globalToolbarColor", String.valueOf(globalToolbarColor.getRGB()))));
            globalFontFamily = props.getProperty("globalFontFamily", globalFontFamily);
            globalFontSize = Integer.parseInt(props.getProperty("globalFontSize", String.valueOf(globalFontSize)));
            dataStorageLocation = new File(props.getProperty("dataStorageLocation", dataStorageLocation.getAbsolutePath()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void saveGlobalSettings() {
        Properties props = new Properties();
        props.setProperty("globalBgColor", String.valueOf(globalBgColor.getRGB()));
        props.setProperty("globalToolbarColor", String.valueOf(globalToolbarColor.getRGB()));
        props.setProperty("globalFontFamily", globalFontFamily);
        props.setProperty("globalFontSize", String.valueOf(globalFontSize));
        props.setProperty("dataStorageLocation", dataStorageLocation.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(GLOBAL_SETTINGS_FILE)) {
            props.store(fos, "Global Settings");
        } catch (IOException e) { e.printStackTrace(); }
    }
}
