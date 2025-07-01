package com.sayiza.oracle2postgre.writing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileWriter {

  private static final Logger log = LoggerFactory.getLogger(FileWriter.class);
  public static void write(Path targetDir, String filename, String content) {
    try {
      Files.createDirectories(targetDir);
      Path file = targetDir.resolve(filename);
      Files.writeString(file, content);
      log.info("File written: {} {}", targetDir, filename);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
