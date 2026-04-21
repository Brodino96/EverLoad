package dev.brodino.everload.ui;

import dev.brodino.everload.EverLoad;
import dev.brodino.everload.sync.FileChanges;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChangeConfirmationDialog {

	private static final int DIALOG_WIDTH = 800;
	private static final int DIALOG_HEIGHT = 400;

	// --- Dark Mode Palette ---
	private static class DarkTheme {
		static final Color BG_DARK = new Color(30, 30, 30);
		static final Color BG_LIGHTER = new Color(45, 45, 48);
		static final Color FG_TEXT = new Color(220, 220, 220);
		static final Color FG_SUBTLE = new Color(150, 150, 150);
		static final Color BORDER = new Color(60, 60, 60);

		static final Color ACCENT_GREEN = new Color(75, 181, 67);
		static final Color ACCENT_YELLOW = new Color(229, 192, 123);
		static final Color ACCENT_RED = new Color(244, 71, 71);
		static final Color ACCENT_CYAN = new Color(86, 156, 214);
	}

	private final FileChanges changes;
	private final String repositoryUrl;
	private boolean userAccepted = false;

	/**
	 * Create a new confirmation dialog for the given changes.
	 * @param changes The file changes to display
	 * @param repositoryUrl The repository URL (for display purposes)
	 */
	public ChangeConfirmationDialog(FileChanges changes, String repositoryUrl) {
		this.changes = changes;
		this.repositoryUrl = repositoryUrl;
	}

	/**
	 * Show the dialog and wait for user input.
	 * This method blocks until the user clicks Accept or Decline.
	 * @return true if the user accepted the changes, false if declined
	 */
	public boolean showAndWait() {
		AtomicBoolean result = new AtomicBoolean(false);
		try {
			if (SwingUtilities.isEventDispatchThread()) {
				result.set(showDialogOnEDT());
			} else {
				SwingUtilities.invokeAndWait(() -> result.set(showDialogOnEDT()));
			}
		} catch (InterruptedException | InvocationTargetException e) {
			EverLoad.LOGGER.error("Error showing confirmation dialog", e);
			return false;
		}
		return result.get();
	}

	/**
	 * Create and show the dialog on the EDT.
	 */
	private boolean showDialogOnEDT() {
		JDialog dialog = new JDialog((Frame) null, "EverLoad - Review Changes", Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
		dialog.setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
		dialog.setLocationRelativeTo(null);
		dialog.requestFocus();
		dialog.setResizable(true);

		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBackground(DarkTheme.BG_DARK);
		mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

		mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
		mainPanel.add(createChangesScrollPane(), BorderLayout.CENTER);
		mainPanel.add(createButtonPanel(dialog), BorderLayout.SOUTH);

		dialog.setContentPane(mainPanel);
		dialog.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				userAccepted = false;
			}
		});
		dialog.pack();
		dialog.setVisible(true);
		return userAccepted;
	}
	/**
	 * Create the header panel with title and summary.
	 */
	private JPanel createHeaderPanel() {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JLabel titleLabel = new JLabel("Review Incoming Changes");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
		titleLabel.setForeground(DarkTheme.FG_TEXT);
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(titleLabel);

		panel.add(Box.createVerticalStrut(8));

		JLabel repoLabel = new JLabel("Repository: " + shortenUrl(repositoryUrl));
		repoLabel.setForeground(DarkTheme.FG_SUBTLE);
		repoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(repoLabel);

		panel.add(Box.createVerticalStrut(12));

		JLabel summaryLabel = new JLabel("<html>" + createSummaryText() + "</html>");
		summaryLabel.setForeground(DarkTheme.FG_TEXT);
		summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(summaryLabel);

		return panel;
	}

	private JScrollPane createChangesScrollPane() {
		JPanel changesPanel = new JPanel();
		changesPanel.setLayout(new BoxLayout(changesPanel, BoxLayout.Y_AXIS));
		changesPanel.setBackground(DarkTheme.BG_DARK);

		if (!changes.getAdded().isEmpty()) {
			changesPanel.add(createCategoryPanel("Added", changes.getAdded(), DarkTheme.ACCENT_GREEN));
			changesPanel.add(Box.createVerticalStrut(10));
		}
		if (!changes.getModified().isEmpty()) {
			changesPanel.add(createCategoryPanel("Modified", changes.getModified(), DarkTheme.ACCENT_YELLOW));
			changesPanel.add(Box.createVerticalStrut(10));
		}
		if (!changes.getDeleted().isEmpty()) {
			changesPanel.add(createCategoryPanel("Deleted", changes.getDeleted(), DarkTheme.ACCENT_RED));
			changesPanel.add(Box.createVerticalStrut(10));
		}
		if (!changes.getRenamed().isEmpty()) {
			changesPanel.add(createCategoryPanel("Renamed", changes.getRenamed(), DarkTheme.ACCENT_CYAN));
		}
		
		// Add filler to push content to top
		changesPanel.add(Box.createVerticalGlue());

		JScrollPane scrollPane = new JScrollPane(changesPanel);
		scrollPane.setBorder(BorderFactory.createLineBorder(DarkTheme.BORDER));
		scrollPane.getViewport().setBackground(DarkTheme.BG_DARK);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		return scrollPane;
	}
	
	/**
	 * Create a panel for a category of file changes.
	 */
	private JPanel createCategoryPanel(String title, List<String> files, Color accentColor) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(DarkTheme.BG_LIGHTER);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

		DefaultListModel<String> listModel = new DefaultListModel<>();
		files.forEach(f -> listModel.addElement(" " + f));

		JList<String> fileList = new JList<>(listModel);
		fileList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		fileList.setBackground(DarkTheme.BG_LIGHTER);
		fileList.setForeground(DarkTheme.FG_TEXT);
		fileList.setSelectionBackground(accentColor.darker().darker());

		String borderTitle = String.format("%s (%d)", title, files.size());
		TitledBorder border = BorderFactory.createTitledBorder(
				new LineBorder(accentColor, 1), borderTitle);
		border.setTitleColor(accentColor);
		border.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

		panel.setBorder(border);
		panel.add(fileList, BorderLayout.CENTER);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));

		return panel;
	}
	
	/**
	 * Create the button panel with Accept and Decline buttons.
	 */
	private JPanel createButtonPanel(JDialog dialog) {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
		panel.setOpaque(false);

		JButton acceptButton = createStyledButton("Accept Changes", DarkTheme.ACCENT_GREEN);
		acceptButton.addActionListener(e -> {
			userAccepted = true;
			dialog.dispose();
		});

		JButton declineButton = createStyledButton("Decline Changes", DarkTheme.ACCENT_RED);
		declineButton.addActionListener(e -> {
			userAccepted = false;
			EverLoad.LOGGER.info("User declined changes");
			dialog.dispose();
		});

		panel.add(acceptButton);
		panel.add(declineButton);
		return panel;
	}

	private JButton createStyledButton(String text, Color baseColor) {
		JButton button = new JButton(text);
		button.setPreferredSize(new Dimension(160, 40));
		button.setBackground(baseColor.darker());
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createLineBorder(baseColor));
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));
		return button;
	}

	/**
	 * Create summary text describing the changes.
	 */
	private String createSummaryText() {
		String base = changes.isFreshClone() ? "Fresh clone: " : "Updates: ";
		return String.format("%s <font color='%s'>%d added</font>, <font color='%s'>%d modified</font>, <font color='%s'>%d deleted</font>",
				base,
				toHex(DarkTheme.ACCENT_GREEN), changes.addedCount(),
				toHex(DarkTheme.ACCENT_YELLOW), changes.modifiedCount(),
				toHex(DarkTheme.ACCENT_RED), changes.deletedCount());
	}

	private String toHex(Color c) {
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	private String shortenUrl(String url) {
		if (url == null) return "Unknown";
		
		// Remove protocol
		String shortened = url.replaceFirst("^https?://", "");

		return shortened.length() > 60 ? shortened.substring(0, 57) + "..." : shortened;
	}
}
