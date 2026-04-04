package dev.brodino.everload.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 100% AI generated
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
	public FileChanges(List<String> added, List<String> modified, List<String> deleted,
					   List<String> renamed, boolean isFreshClone) {
		this.added = Collections.unmodifiableList(new ArrayList<>(added));
		this.modified = Collections.unmodifiableList(new ArrayList<>(modified));
		this.deleted = Collections.unmodifiableList(new ArrayList<>(deleted));
		this.renamed = Collections.unmodifiableList(new ArrayList<>(renamed));
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

	/**
	 * @return List of file paths that were added
	 */
	public List<String> getAdded() {
		return added;
	}

	/**
	 * @return List of file paths that were modified
	 */
	public List<String> getModified() {
		return modified;
	}

	/**
	 * @return List of file paths that were deleted
	 */
	public List<String> getDeleted() {
		return deleted;
	}

	/**
	 * @return List of rename operations (formatted as "oldPath -> newPath")
	 */
	public List<String> getRenamed() {
		return renamed;
	}

	/**
	 * @return true if this represents a fresh clone operation
	 */
	public boolean isFreshClone() {
		return isFreshClone;
	}

	/**
	 * @return true if there are any changes (added, modified, deleted, or renamed files)
	 */
	public boolean hasChanges() {
		return !added.isEmpty() || !modified.isEmpty() || !deleted.isEmpty() || !renamed.isEmpty();
	}

	/**
	 * @return The total number of file changes across all categories
	 */
	public int totalChanges() {
		return added.size() + modified.size() + deleted.size() + renamed.size();
	}

	/**
	 * @return The number of added files
	 */
	public int addedCount() {
		return added.size();
	}

	/**
	 * @return The number of modified files
	 */
	public int modifiedCount() {
		return modified.size();
	}

	/**
	 * @return The number of deleted files
	 */
	public int deletedCount() {
		return deleted.size();
	}

	/**
	 * @return The number of renamed files
	 */
	public int renamedCount() {
		return renamed.size();
	}

	@Override
	public String toString() {
		return String.format("FileChanges{added=%d, modified=%d, deleted=%d, renamed=%d, freshClone=%s}",
				added.size(), modified.size(), deleted.size(), renamed.size(), isFreshClone);
	}
}
