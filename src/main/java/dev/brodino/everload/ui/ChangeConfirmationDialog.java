package dev.brodino.everload.ui;

import dev.brodino.everload.EverLoad;
import dev.brodino.everload.sync.FileChanges;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// 100% AI generated
public class ChangeConfirmationDialog {
	
	private static final int DIALOG_WIDTH = 800;
	private static final int DIALOG_HEIGHT = 900;
	private static final int MAX_FILES_PER_CATEGORY = Integer.MAX_VALUE;
	
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
			// Ensure we run on the Swing EDT
			if (SwingUtilities.isEventDispatchThread()) {
				result.set(showDialogOnEDT());
			} else {
				SwingUtilities.invokeAndWait(() -> result.set(showDialogOnEDT()));
			}
		} catch (InterruptedException e) {
			EverLoad.LOGGER.error("Dialog interrupted", e);
			Thread.currentThread().interrupt();
			return false;
		} catch (InvocationTargetException e) {
			EverLoad.LOGGER.error("Error showing confirmation dialog", e.getCause());
			return false;
		}
		
		return result.get();
	}
	
	/**
	 * Create and show the dialog on the EDT.
	 */
	private boolean showDialogOnEDT() {
		// Set system look and feel for native appearance
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// Fall back to default look and feel
			EverLoad.LOGGER.warn("Could not set system look and feel: {}", e.getMessage());
		}
		
		// Create the dialog
		JDialog dialog = new JDialog((Frame) null, "EverLoad - Review Changes", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
		dialog.setLocationRelativeTo(null); // Center on screen
		dialog.setResizable(true);
		
		// Create main panel
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
		
		// Header
		JPanel headerPanel = createHeaderPanel();
		mainPanel.add(headerPanel, BorderLayout.NORTH);
		
		// Changes list
		JScrollPane scrollPane = createChangesScrollPane();
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		
		// Buttons
		JPanel buttonPanel = createButtonPanel(dialog);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		dialog.setContentPane(mainPanel);
		
		// Handle window close as decline
		dialog.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				userAccepted = false;
			}
		});
		
		// Show dialog (blocks until closed)
		dialog.setVisible(true);
		
		return userAccepted;
	}
	
	/**
	 * Create the header panel with title and summary.
	 */
	private JPanel createHeaderPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		// Title
		JLabel titleLabel = new JLabel("Review Incoming Changes");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(titleLabel);
		
		panel.add(Box.createVerticalStrut(8));
		
		// Repository info
		String shortUrl = shortenUrl(repositoryUrl);
		JLabel repoLabel = new JLabel("Repository: " + shortUrl);
		repoLabel.setFont(repoLabel.getFont().deriveFont(Font.PLAIN, 12f));
		repoLabel.setForeground(Color.DARK_GRAY);
		repoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(repoLabel);
		
		panel.add(Box.createVerticalStrut(8));
		
		// Summary
		String summary = createSummaryText();
		JLabel summaryLabel = new JLabel("<html>" + summary + "</html>");
		summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(summaryLabel);
		
		panel.add(Box.createVerticalStrut(5));
		
		return panel;
	}
	
	/**
	 * Create the scrollable panel containing the file changes.
	 */
	private JScrollPane createChangesScrollPane() {
		JPanel changesPanel = new JPanel();
		changesPanel.setLayout(new BoxLayout(changesPanel, BoxLayout.Y_AXIS));
		changesPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		// Added files
		if (!changes.getAdded().isEmpty()) {
			JPanel addedPanel = createCategoryPanel("Added", changes.getAdded(), 
					new Color(40, 167, 69)); // Green
			changesPanel.add(addedPanel);
			changesPanel.add(Box.createVerticalStrut(10));
		}
		
		// Modified files
		if (!changes.getModified().isEmpty()) {
			JPanel modifiedPanel = createCategoryPanel("Modified", changes.getModified(),
					new Color(255, 193, 7)); // Yellow/Orange
			changesPanel.add(modifiedPanel);
			changesPanel.add(Box.createVerticalStrut(10));
		}
		
		// Deleted files
		if (!changes.getDeleted().isEmpty()) {
			JPanel deletedPanel = createCategoryPanel("Deleted", changes.getDeleted(),
					new Color(220, 53, 69)); // Red
			changesPanel.add(deletedPanel);
			changesPanel.add(Box.createVerticalStrut(10));
		}
		
		// Renamed files
		if (!changes.getRenamed().isEmpty()) {
			JPanel renamedPanel = createCategoryPanel("Renamed", changes.getRenamed(),
					new Color(23, 162, 184)); // Cyan
			changesPanel.add(renamedPanel);
		}
		
		// Add filler to push content to top
		changesPanel.add(Box.createVerticalGlue());
		
		JScrollPane scrollPane = new JScrollPane(changesPanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		
		return scrollPane;
	}
	
	/**
	 * Create a panel for a category of file changes.
	 */
	private JPanel createCategoryPanel(String title, List<String> files, Color accentColor) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		// Title border with count
		String borderTitle = String.format("%s (%d file%s)", title, files.size(), 
				files.size() == 1 ? "" : "s");
		TitledBorder border = BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(accentColor, 2),
				borderTitle);
		border.setTitleColor(accentColor.darker());
		border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
		panel.setBorder(border);
		
		// File list
		int displayCount = Math.min(files.size(), MAX_FILES_PER_CATEGORY);
		for (int i = 0; i < displayCount; i++) {
			JLabel fileLabel = new JLabel("  " + files.get(i));
			fileLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
			fileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(fileLabel);
		}
		
		// Show truncation message if needed
		if (files.size() > MAX_FILES_PER_CATEGORY) {
			int remaining = files.size() - MAX_FILES_PER_CATEGORY;
			JLabel moreLabel = new JLabel("  ... and " + remaining + " more file(s)");
			moreLabel.setFont(moreLabel.getFont().deriveFont(Font.ITALIC));
			moreLabel.setForeground(Color.GRAY);
			moreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(moreLabel);
		}
		
		return panel;
	}
	
	/**
	 * Create the button panel with Accept and Decline buttons.
	 */
	private JPanel createButtonPanel(JDialog dialog) {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
		
		// Accept button
		JButton acceptButton = new JButton("Accept Changes");
		acceptButton.setPreferredSize(new Dimension(150, 35));
		acceptButton.setBackground(new Color(40, 167, 69));
		acceptButton.setForeground(Color.WHITE);
		acceptButton.setFocusPainted(false);
		acceptButton.addActionListener(e -> {
			userAccepted = true;
			EverLoad.LOGGER.info("User accepted changes");
			dialog.dispose();
		});
		panel.add(acceptButton);
		
		// Decline button
		JButton declineButton = new JButton("Decline Changes");
		declineButton.setPreferredSize(new Dimension(150, 35));
		declineButton.setBackground(new Color(220, 53, 69));
		declineButton.setForeground(Color.WHITE);
		declineButton.setFocusPainted(false);
		declineButton.addActionListener(e -> {
			userAccepted = false;
			EverLoad.LOGGER.info("User declined changes");
			dialog.dispose();
		});
		panel.add(declineButton);
		
		return panel;
	}
	
	/**
	 * Create summary text describing the changes.
	 */
	private String createSummaryText() {
		StringBuilder sb = new StringBuilder();
		
		if (changes.isFreshClone()) {
			sb.append("This is a <b>fresh clone</b>. ");
			sb.append("The following <b>").append(changes.totalChanges()).append(" file(s)</b> ");
			sb.append("will be copied to your game directory:");
		} else {
			sb.append("The following changes will be applied to your game directory:<br>");
			
			List<String> parts = new java.util.ArrayList<>();
			if (changes.addedCount() > 0) {
				parts.add("<font color='green'>" + changes.addedCount() + " added</font>");
			}
			if (changes.modifiedCount() > 0) {
				parts.add("<font color='orange'>" + changes.modifiedCount() + " modified</font>");
			}
			if (changes.deletedCount() > 0) {
				parts.add("<font color='red'>" + changes.deletedCount() + " deleted</font>");
			}
			if (changes.renamedCount() > 0) {
				parts.add("<font color='teal'>" + changes.renamedCount() + " renamed</font>");
			}
			
			sb.append("<b>").append(String.join(", ", parts)).append("</b>");
		}
		
		return sb.toString();
	}
	
	/**
	 * Shorten a URL for display purposes.
	 */
	private String shortenUrl(String url) {
		if (url == null) return "Unknown";
		
		// Remove protocol
		String shortened = url.replaceFirst("^https?://", "");
		
		// Truncate if too long
		if (shortened.length() > 50) {
			shortened = shortened.substring(0, 47) + "...";
		}
		
		return shortened;
	}
}
