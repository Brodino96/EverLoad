package dev.brodino.everload.sync;

import java.util.List;

/**
 * @author Claude
 */
public class FileChanges {

	private final List<String> added;
	private final List<String> modified;
	private final List<String> deleted;
	private final List<String> renamed;
	private final boolean isFreshClone;

	/**
	 * Create a new FileChanges instance with the given categorized changes.
	 *
	 * @param added List of file paths that were added
	 * @param modified List of file paths that were modified
	 * @param deleted List of file paths that were deleted
	 * @param renamed List of rename operations (formatted as "oldPath -> newPath")
	 * @param isFreshClone Whether this represents a fresh clone (no previous state)
	 */
	public FileChanges(List<String> added, List<String> modified, List<String> deleted, List<String> renamed, boolean isFreshClone) {
		this.added = List.copyOf(added);
		this.modified = List.copyOf(modified);
		this.deleted = List.copyOf(deleted);
		this.renamed = List.copyOf(renamed);
		this.isFreshClone = isFreshClone;
	}

	/**
	 * Create an empty FileChanges instance representing no changes.
	 */
	public static FileChanges empty() {
		return new FileChanges(List.of(), List.of(), List.of(), List.of(), false);
	}

	/**
	 * Create a FileChanges instance for a fresh clone where all files are new.
	 *
	 * @param allFiles List of all file paths in the cloned repository
	 * @return FileChanges with all files marked as added
	 */
	public static FileChanges forFreshClone(List<String> allFiles) {
		return new FileChanges(allFiles, List.of(), List.of(), List.of(), true);
	}

	public List<String> getAdded() { return added; }
	public List<String> getModified() { return modified; }
	public List<String> getDeleted() { return deleted; }
	public List<String> getRenamed() { return renamed; }

	public int getAddedCount() { return added.size(); }
	public int getModifiedCount() { return modified.size(); }
	public int getDeletedCount() { return deleted.size(); }
	public int getRenamedCount() { return renamed.size(); }
	public int getTotalChanges() { return added.size() + modified.size() + deleted.size() + renamed.size(); }

	public boolean isFreshClone() { return isFreshClone; }
	public boolean hasChanges() {
		return !added.isEmpty() || !modified.isEmpty() || !deleted.isEmpty() || !renamed.isEmpty();
	}

	@Override
	public String toString() {
		return String.format(
			"FileChanges{added=%d, modified=%d, deleted=%d, renamed=%d, freshClone=%s}",
			added.size(), modified.size(), deleted.size(), renamed.size(), isFreshClone
		);
	}
}
