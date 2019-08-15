package org.gotson.komga.domain.service

import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.Serie
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.streams.asSequence
import kotlin.streams.toList

private val logger = KotlinLogging.logger {}

@Service
class FileSystemScanner(
) {

  val supportedExtensions = listOf("cbr", "rar", "cbz", "zip")

  fun scanRootFolder(root: Path): List<Serie> {
    logger.info { "Scanning folder: $root" }

    return Files.walk(root).asSequence()
        .filter { !Files.isHidden(it) }
        .filter { Files.isDirectory(it) }
        .mapNotNull { dir ->
          val books = Files.list(dir)
              .filter { Files.isRegularFile(it) }
              .filter { supportedExtensions.contains(FilenameUtils.getExtension(it.fileName.toString())) }
              .map {
                Book(
                    name = FilenameUtils.getBaseName(it.fileName.toString()),
                    url = it.toUri().toURL(),
                    updated = it.getUpdatedTime()
                )
              }.toList()
          if (books.isNullOrEmpty()) return@mapNotNull null
          Serie(
              name = dir.fileName.toString(),
              url = dir.toUri().toURL(),
              updated = dir.getUpdatedTime()
          ).also { it.books = books.toMutableList() }
        }.toList()
  }
}

fun Path.getUpdatedTime() =
    Files.readAttributes(this, BasicFileAttributes::class.java).let {
      maxOf(it.creationTime(), it.lastModifiedTime()).toLocalDateTime()
    }

fun FileTime.toLocalDateTime() =
    LocalDateTime.ofInstant(this.toInstant(), ZoneId.systemDefault())