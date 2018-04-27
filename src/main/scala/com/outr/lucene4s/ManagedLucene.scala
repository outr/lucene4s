package com.outr.lucene4s

import java.nio.file.{Files, Path}
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

import com.outr.lucene4s.keyword.KeywordIndexing

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

class ManagedLucene(directory: Option[Path] = None,
                    appendIfExists: Boolean = true,
                    defaultFullTextSearchable: Boolean = false,
                    autoCommit: Boolean = false,
                    stopWords: Set[String] = KeywordIndexing.DefaultStopWords,
                    stopWordsIgnoreCase: Boolean = true) extends ReplaceableLucene {
  private val lock = new ReentrantLock()

  attemptMigration()
  load()

  override protected def instance_=(instance: Lucene): Unit = {
    while (lock.isLocked) {         // Wait if migration is occurring
      Thread.sleep(10L)
    }
    super.instance_=(instance)
  }

  private implicit def toConsumer[A](function: A => Unit): Consumer[A] = new Consumer[A]() {
    override def accept(arg: A): Unit = function.apply(arg)
  }

  def replace(f: Lucene => Future[Unit]): Future[Unit] = {
    val lucene = create(temp = true)              // Create a new temp Lucene instance
    f(lucene).map { _ =>                          // Allow building of the replacement
      directory match {
        case Some(path) => {
          lucene.dispose()                        // Dispose the temp instance to release file locks
          lock.lock()                             // Establish a lock to avoid any access during swapping
          try {
            instance.dispose()                    // Dispose the active instance to release file locks
            Files.move(                           // Move from the temp path to working
              path.resolve("temp"),
              path.resolve("working")
            )
            attemptMigration()                    // Manages the shift from working to active
            load()                                // Re-loads the Lucene index with the new active
          } finally {
            lock.unlock()
          }
        }
        case None => {                            // No file locking is an issue for in-memeory
          val previous = instance
          instance = lucene
          previous.dispose()
        }
      }
    }
  }

  /**
    * Migrates from direct lucene to revision structure or from old revision to new revision. This does no locking or
    * any other management. Simply manages the files.
    */
  protected def attemptMigration(): Unit = directory match {
    case Some(path) => {
      val active = path.resolve("active")
      val temp = path.resolve("temp")
      val working = path.resolve("working")
      val directories = path.iterator().asScala.toList.filter(Files.isDirectory(_))
      if (Files.isDirectory(active)) {                    // Already has an active index
        // TODO: only do this if `recoverable` is false
        if (Files.isDirectory(temp)) {                    // Delete temporary directory
          Files.walk(temp).forEach(Files.delete(_))
        }
        if (Files.isDirectory(working)) {                 // Replace current with working
          Files.walk(active).forEach(Files.delete(_))     // Delete current active directory
          Files.move(working, active)                     // Move the working directory to be the active
        }
      } else if (directories.nonEmpty) {                  // Migrate from direct lucene
        Files.createDirectory(active)
        directories.foreach { dir =>
          val target = active.resolve(dir.getFileName)
          Files.move(dir, target)
        }
      }
    }
    case None => // In-Memory
  }

  protected def load(): Unit = instance = create(temp = false)

  protected def create(temp: Boolean): Lucene = new DirectLucene(
    directory = directory.map(_.resolve(if (temp) "temp" else "active")),
    appendIfExists = appendIfExists,
    defaultFullTextSearchable = defaultFullTextSearchable,
    autoCommit = autoCommit,
    stopWords = stopWords,
    stopWordsIgnoreCase = stopWordsIgnoreCase
  )
}