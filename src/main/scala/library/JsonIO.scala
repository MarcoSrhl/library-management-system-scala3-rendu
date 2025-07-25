package library

import Types.{ISBN, UserID}
import Results.{LibraryResult, LibraryError}
import upickle.default.*
import java.io.{File, PrintWriter, IOException}
import java.util.UUID
import java.time.LocalDateTime
import scala.io.Source
import scala.util.{Try, Success, Failure}

/**
 * JsonIO.scala - JSON Serialization and File I/O Operations
 * 
 * This file handles all file input/output operations for the library management system,
 * providing robust JSON serialization/deserialization with comprehensive error handling.
 * It implements functional error handling patterns using Try, Either, and Option types.
 * 
 * Key Features:
 * - JSON Serialization: Complete serialization support for all domain types
 * - File I/O Operations: Safe reading and writing of library data to disk
 * - Error Handling: Functional error handling with detailed error types
 * - Type Safety: Given instances for opaque types and complex domain objects
 * - Persistence Layer: Complete persistence solution for the library catalog
 * 
 * The file demonstrates interesting Scala 3 features:
 * - Given instances for JSON serialization
 * - Functional error handling with Try, Either, Option
 * - Enum-based error types for I/O operations
 * - Pattern matching for error handling
 * - Resource management with proper cleanup
 * 
 * Functionality Provided:
 * - saveCatalog: Persists complete library catalog to JSON files
 * - loadCatalog: Loads library catalog from JSON files with fallback
 * - Individual save/load operations for books, users, transactions
 * - Error recovery and meaningful error messages
 * - Directory creation and file permission handling
 * 
 * Error Types:
 * - FileNotFound: When required files don't exist
 * - Permission errors: For read/write access issues
 * - JSON parsing errors: For malformed JSON data
 * - Directory creation errors: When directories can't be created
 * 
 * Usage:
 * - Use saveCatalog/loadCatalog for complete persistence operations
 * - Handle errors functionally with pattern matching
 * - Fallback to empty catalog when loading fails
 * - Check IOResult types for error handling
 */
object JsonIO:

  // Opaque types
  given ReadWriter[ISBN] = readwriter[String].bimap(_.value, ISBN(_))
  given ReadWriter[UserID] = readwriter[UUID].bimap(_.value, UserID(_))

  // Enums & case classes
  given rwUser: ReadWriter[User] = ReadWriter.merge(
    macroRW[User.Student],
    macroRW[User.Faculty],
    macroRW[User.Librarian]
  )

  given rwTransaction: ReadWriter[Transaction] = ReadWriter.merge(
    macroRW[Transaction.Loan],
    macroRW[Transaction.Return],
    macroRW[Transaction.Reservation]
  )

  given rwBook: ReadWriter[Book] = macroRW
  given rwUUID: ReadWriter[UUID] = readwriter[String].bimap(_.toString, UUID.fromString)
  given rwTime: ReadWriter[LocalDateTime] = readwriter[String].bimap(_.toString, LocalDateTime.parse)

  // Error handling types
  enum IOError:
    case FileNotFound(path: String)
    case WritePermissionDenied(path: String)
    case ReadPermissionDenied(path: String)
    case JsonParsingError(path: String, message: String)
    case JsonSerializationError(message: String)
    case DirectoryCreationError(path: String)
    case UnexpectedIOError(path: String, message: String)

  type IOResult[T] = Either[IOError, T]

  // Enhanced save operations with error handling
  def saveCatalog(catalog: LibraryCatalog, basePath: String): IOResult[Unit] =
    for
      _ <- ensureDirectoryExists(basePath)
      _ <- saveBooks(catalog.books.values.toList, s"$basePath/books.json")
      _ <- saveUsers(catalog.users.values.toList, s"$basePath/users.json")
      _ <- saveTransactions(catalog.transactions, s"$basePath/transactions.json")
    yield ()

  def saveCatalogSafe(catalog: LibraryCatalog, basePath: String): Try[Unit] =
    saveCatalog(catalog, basePath) match
      case Right(_) => Success(())
      case Left(error) => Failure(new IOException(s"Failed to save catalog: $error"))

  // Enhanced load operations with error handling
  def loadCatalog(basePath: String): LibraryCatalog =
    loadCatalogSafe(basePath) match
      case Right(catalog) => catalog
      case Left(error) =>
        println(s"Warning: Failed to load catalog ($error), creating empty catalog")
        LibraryCatalog.empty

  def loadCatalogSafe(basePath: String): IOResult[LibraryCatalog] =
    for
      books <- loadBooks(s"$basePath/books.json")
      users <- loadUsers(s"$basePath/users.json")
      transactions <- loadTransactions(s"$basePath/transactions.json")
    yield LibraryCatalog(
      books.map(b => b.isbn -> b).toMap,
      users.map(u => u.id -> u).toMap,
      transactions
    )

  // Specific save operations
  private def saveBooks(books: List[Book], path: String): IOResult[Unit] =
    writeToFile(path, books)

  private def saveUsers(users: List[User], path: String): IOResult[Unit] =
    writeToFile(path, users)

  private def saveTransactions(transactions: List[Transaction], path: String): IOResult[Unit] =
    writeToFile(path, transactions)

  // Specific load operations with fallback
  private def loadBooks(path: String): IOResult[List[Book]] =
    readFromFile[List[Book]](path).map(_.getOrElse(List.empty))

  private def loadUsers(path: String): IOResult[List[User]] =
    readFromFile[List[User]](path).map(_.getOrElse(List.empty))

  private def loadTransactions(path: String): IOResult[List[Transaction]] =
    readFromFile[List[Transaction]](path).map(_.getOrElse(List.empty))

  // Core I/O operations with comprehensive error handling
  private def writeToFile[T: Writer](path: String, data: T): IOResult[Unit] =
    Try {
      val json = write(data, indent = 2)
      val file = new File(path)
      
      // Ensure parent directory exists
      Option(file.getParentFile).foreach { parent =>
        if !parent.exists() then parent.mkdirs()
      }
      
      val writer = new PrintWriter(file)
      try
        writer.write(json)
        writer.flush()
      finally
        writer.close()
    } match
      case Success(_) => Right(())
      case Failure(ex: IOException) if ex.getMessage.contains("Permission denied") =>
        Left(IOError.WritePermissionDenied(path))
      case Failure(ex: IllegalArgumentException) =>
        Left(IOError.JsonSerializationError(ex.getMessage))
      case Failure(ex) =>
        Left(IOError.UnexpectedIOError(path, ex.getMessage))

  private def readFromFile[T: Reader](path: String): IOResult[Option[T]] =
    val file = new File(path)
    
    if !file.exists() then
      Right(None) // File not existing is OK, we return None
    else if !file.canRead() then
      Left(IOError.ReadPermissionDenied(path))
    else if file.length() == 0 then
      Right(None) // Empty file is OK
    else
      Try {
        val source = Source.fromFile(file)
        try
          val content = source.mkString.trim
          if content.nonEmpty then Some(read[T](content))
          else None
        finally
          source.close()
      } match
        case Success(result) => Right(result)
        case Failure(ex: ujson.ParseException) =>
          Left(IOError.JsonParsingError(path, s"Invalid JSON: ${ex.getMessage}"))
        case Failure(ex: upickle.core.AbortException) =>
          Left(IOError.JsonParsingError(path, s"JSON structure mismatch: ${ex.getMessage}"))
        case Failure(ex: IOException) if ex.getMessage.contains("Permission denied") =>
          Left(IOError.ReadPermissionDenied(path))
        case Failure(ex) =>
          Left(IOError.UnexpectedIOError(path, ex.getMessage))

  private def ensureDirectoryExists(path: String): IOResult[Unit] =
    Try {
      val dir = new File(path)
      if !dir.exists() && !dir.mkdirs() then
        throw new IOException(s"Failed to create directory: $path")
    } match
      case Success(_) => Right(())
      case Failure(_) => Left(IOError.DirectoryCreationError(path))

  // Backup operations
  def createBackup(catalog: LibraryCatalog, basePath: String): IOResult[String] =
    val timestamp = LocalDateTime.now().toString.replace(":", "-")
    val backupPath = s"$basePath/backups/backup-$timestamp"
    saveCatalog(catalog, backupPath).map(_ => backupPath)

  def restoreFromBackup(backupPath: String): IOResult[LibraryCatalog] =
    loadCatalogSafe(backupPath)

  // Utility functions for error handling
  extension [T](result: IOResult[T])
    def getOrLog(default: T): T = result match
      case Right(value) => value
      case Left(error) =>
        println(s"IO Error: $error")
        default

    def logError(): IOResult[T] = result match
      case Right(value) => Right(value)
      case Left(error) =>
        println(s"IO Error: $error")
        Left(error)

  // JSON validation utilities
  def validateJsonFile(path: String): IOResult[Boolean] =
    readFromFile[ujson.Value](path).map(_.isDefined)

  def getFileInfo(path: String): Map[String, String] =
    val file = new File(path)
    Map(
      "exists" -> file.exists().toString,
      "readable" -> file.canRead().toString,
      "writable" -> file.canWrite().toString,
      "size" -> (if file.exists() then file.length().toString else "0"),
      "lastModified" -> (if file.exists() then java.time.Instant.ofEpochMilli(file.lastModified()).toString else "unknown")
    )
