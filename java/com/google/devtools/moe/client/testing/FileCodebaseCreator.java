// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.parser.Term;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Creates a codebase based upon a local existing directory. Primarily used for testing purposes.
 * Works with single files and directories.
 */
public class FileCodebaseCreator implements CodebaseCreator  {
  private static final String PATH_OPTION = "path";
  private static final String PROJECT_SPACE_OPTION = "projectspace";

  /**
   * This CodebaseCreator subverts the "CodebaseCreator"-interface as it does not implement the
   * getProjectSpace() property, which is only used by the OneMigrationDirective. In order to
   * implement this method a reference to the options provided with the creation term would be
   * required.
   */
  public String getProjectSpace() {
    throw new UnsupportedOperationException("The getProjectSpace() operation is not available " +
                                            "for a FileCodebaseCreator.");
  }

  public Codebase create(Map<String, String> options) throws CodebaseCreationError {
    // Validate that the options passed are valid.
    Utils.checkKeys(options, ImmutableSet.of(PATH_OPTION, PROJECT_SPACE_OPTION));

    // Get the source path to the file/directory described in the options list.
    String source = options.get(PATH_OPTION);
    if (Strings.isNullOrEmpty(source)) {
      throw new CodebaseCreationError(
        String.format("Please specify the mandatory '%s' option for the FileCodebaseCreator",
                      PATH_OPTION));
    }

    // Create the codebase instance.
    File codebasePath = getCodebasePath(new File(source));
    String projectSpace = options.containsKey(PROJECT_SPACE_OPTION)
                          ? options.get(PROJECT_SPACE_OPTION)
                          : "public";
    CodebaseExpression expression = new CodebaseExpression(new Term("file", options));
    return new Codebase(codebasePath, projectSpace, expression);
  }

  /**
   * Returns a folder reference to the codebase described by the source file.
   * Will extract .tar/.tar.gz automatically.
   * @param sourceFile The file describing a codebase, or a directory.
   * @return A reference to a codebase directory
   * @throws CodebaseCreationError
   */
  public static File getCodebasePath(File sourceFile) throws CodebaseCreationError {
    // Check whether the specified path is valid.
    if (!AppContext.RUN.fileSystem.exists(sourceFile)) {
      throw new CodebaseCreationError(
            String.format("The specified codebase path \"%s\" does not exist.", sourceFile));
    }

    // Get the target path based upon whether we are dealing with a directory or a file.
    if (AppContext.RUN.fileSystem.isDirectory(sourceFile)) {
      // If it is a directory, just return it right away.
      return sourceFile;
    } else if (AppContext.RUN.fileSystem.isFile(sourceFile)) {
      // If it is a file, assume that it is an archive and try to extract it.
      try {
        File extractedArchive = Utils.expandToDirectory(sourceFile);
        if (extractedArchive != null) {
          return extractedArchive;
        }
      } catch (CommandException exception) {
        throw new CodebaseCreationError(
          String.format("Could not extract archive: '%s' %s", sourceFile, exception.getMessage()));
      } catch (IOException exception) {
        throw new CodebaseCreationError(
          String.format("Could not extract archive '%s': %s", sourceFile, exception.getMessage()));
      }
    }

    // If we did not return a codebase-path by now, we have no way of handling it.
    throw new CodebaseCreationError(
        String.format("The '%s'-option of a FileCodebaseCreator must specify either a directory " +
                    "or a .tar/.tar.gz-archive", PATH_OPTION));
  }
}