package library

import Types.{ISBN, UserID}
import java.util.UUID
import java.time.LocalDateTime
import scala.util.{Try, Success, Failure}

/**
 * Validation system using Either and custom ADTs to handle validation
 * of book data, user information, and transaction requests.
 */
object ValidationSystem:

  // Custom ADT for validation errors
  enum ValidationError:
    case EmptyField(fieldName: String)
    case InvalidFormat(fieldName: String, value: String, expected: String)
    case OutOfRange(fieldName: String, value: Any, min: Any, max: Any)
    case DuplicateValue(fieldName: String, value: String)
    case NotFound(entityType: String, identifier: String)
    case BusinessRule(override val message: String)
    case Multiple(errors: List[ValidationError])

    def message: String = this match
      case EmptyField(field) => s"Field '$field' cannot be empty"
      case InvalidFormat(field, value, expected) => s"Field '$field' has invalid format '$value', expected: $expected"
      case OutOfRange(field, value, min, max) => s"Field '$field' value '$value' is out of range [$min, $max]"
      case DuplicateValue(field, value) => s"Field '$field' value '$value' already exists"
      case NotFound(entityType, id) => s"$entityType with identifier '$id' not found"
      case BusinessRule(msg) => s"Business rule violation: $msg"
      case Multiple(errors) => s"Multiple validation errors:\n${errors.map(e => s"  - ${e.message}").mkString("\n")}"

  // Type alias for validation results
  type ValidationResult[T] = Either[ValidationError, T]

  // Validator trait for composable validations
  trait Validator[T]:
    def validate(value: T): ValidationResult[T]

  // Basic validators
  object Validators:
    
    def nonEmpty(fieldName: String): Validator[String] = new Validator[String]:
      def validate(value: String): ValidationResult[String] =
        if value.trim.nonEmpty then Right(value.trim)
        else Left(ValidationError.EmptyField(fieldName))

    def minLength(fieldName: String, min: Int): Validator[String] = new Validator[String]:
      def validate(value: String): ValidationResult[String] =
        if value.length >= min then Right(value)
        else Left(ValidationError.OutOfRange(fieldName, value.length, min, "∞"))

    def maxLength(fieldName: String, max: Int): Validator[String] = new Validator[String]:
      def validate(value: String): ValidationResult[String] =
        if value.length <= max then Right(value)
        else Left(ValidationError.OutOfRange(fieldName, value.length, 0, max))

    def yearRange(fieldName: String, min: Int, max: Int): Validator[Int] = new Validator[Int]:
      def validate(value: Int): ValidationResult[Int] =
        if value >= min && value <= max then Right(value)
        else Left(ValidationError.OutOfRange(fieldName, value, min, max))

    def isbnFormat(fieldName: String): Validator[String] = new Validator[String]:
      def validate(value: String): ValidationResult[String] =
        val cleaned = value.replaceAll("[^0-9X]", "")
        if cleaned.length == 10 || cleaned.length == 13 then Right(value)
        else Left(ValidationError.InvalidFormat(fieldName, value, "ISBN-10 or ISBN-13"))

    def uuidFormat(fieldName: String): Validator[String] = new Validator[String]:
      def validate(value: String): ValidationResult[String] =
        Try(UUID.fromString(value)) match
          case Success(_) => Right(value)
          case Failure(_) => Left(ValidationError.InvalidFormat(fieldName, value, "UUID format"))

    def passwordStrength(fieldName: String): Validator[String] = new Validator[String]:
      def validate(value: String): ValidationResult[String] =
        if value.length >= 6 then Right(value)
        else Left(ValidationError.BusinessRule(s"$fieldName must be at least 6 characters long"))

  // Combinator for chaining validators
  extension [T](validator: Validator[T])
    def and(other: Validator[T]): Validator[T] = new Validator[T]:
      def validate(value: T): ValidationResult[T] =
        validator.validate(value).flatMap(other.validate)

  // Validation for creating books
  case class BookCreationRequest(
    isbn: String,
    title: String,
    authors: List[String],
    publicationYear: Int,
    genre: String
  )

  object BookValidation:
    
    def validateBookCreation(request: BookCreationRequest, existingBooks: Map[ISBN, Book]): ValidationResult[Book] =
      val currentYear = LocalDateTime.now.getYear
      
      val validations = List(
        Validators.nonEmpty("ISBN").and(Validators.isbnFormat("ISBN")).validate(request.isbn),
        Validators.nonEmpty("title").and(Validators.maxLength("title", 200)).validate(request.title),
        validateAuthors(request.authors),
        Validators.yearRange("publicationYear", 1000, currentYear + 1).validate(request.publicationYear),
        Validators.nonEmpty("genre").and(Validators.maxLength("genre", 50)).validate(request.genre)
      )

      // Check for duplicate ISBN
      val isbnCheck = if existingBooks.contains(ISBN(request.isbn)) then
        Left(ValidationError.DuplicateValue("ISBN", request.isbn))
      else Right(request.isbn)

      // Combine all validations
      combineValidations(validations :+ isbnCheck) match
        case Right(_) => Right(Book(
          ISBN(request.isbn),
          request.title,
          request.authors,
          request.publicationYear,
          request.genre,
          isAvailable = true
        ))
        case Left(error) => Left(error)

    private def validateAuthors(authors: List[String]): ValidationResult[List[String]] =
      if authors.isEmpty then
        Left(ValidationError.EmptyField("authors"))
      else if authors.exists(_.trim.isEmpty) then
        Left(ValidationError.EmptyField("author name"))
      else if authors.exists(_.length > 100) then
        Left(ValidationError.OutOfRange("author name", "length", 1, 100))
      else
        Right(authors.map(_.trim))

  // Validation for creating users
  case class UserCreationRequest(
    name: String,
    password: String,
    userType: String,
    additionalInfo: Map[String, String] // major, department, or employeeId
  )

  object UserValidation:
    
    def validateUserCreation(request: UserCreationRequest, existingUsers: Map[UserID, User]): ValidationResult[User] =
      
      val baseValidations = List(
        Validators.nonEmpty("name").and(Validators.maxLength("name", 100)).validate(request.name),
        Validators.nonEmpty("password").and(Validators.passwordStrength("password")).validate(request.password)
      )

      // Check for duplicate name
      val nameCheck = if existingUsers.values.exists(_.name.equalsIgnoreCase(request.name)) then
        Left(ValidationError.DuplicateValue("name", request.name))
      else Right(request.name)

      val userTypeValidation = request.userType.toLowerCase match
        case "student" =>
          request.additionalInfo.get("major") match
            case Some(major) if major.trim.nonEmpty => 
              Right(User.Student(UserID(UUID.randomUUID()), request.name, major.trim, request.password))
            case _ => Left(ValidationError.EmptyField("major"))
        
        case "faculty" =>
          request.additionalInfo.get("department") match
            case Some(dept) if dept.trim.nonEmpty => 
              Right(User.Faculty(UserID(UUID.randomUUID()), request.name, dept.trim, request.password))
            case _ => Left(ValidationError.EmptyField("department"))
        
        case "librarian" =>
          request.additionalInfo.get("employeeId") match
            case Some(empId) if empId.trim.nonEmpty => 
              Right(User.Librarian(UserID(UUID.randomUUID()), request.name, empId.trim, request.password))
            case _ => Left(ValidationError.EmptyField("employeeId"))
        
        case invalid => Left(ValidationError.InvalidFormat("userType", invalid, "student, faculty, or librarian"))

      combineValidations(baseValidations :+ nameCheck) match
        case Right(_) => userTypeValidation
        case Left(error) => Left(error)

  // Validation for transactions
  case class LoanRequest(isbn: String, userId: String)
  case class ReturnRequest(isbn: String, userId: String)
  case class ReservationRequest(isbn: String, userId: String, startDate: String)

  object TransactionValidation:
    
    def validateLoanRequest(request: LoanRequest, catalog: LibraryCatalog): ValidationResult[LoanRequest] =
      val validations = List(
        validateBookExists(request.isbn, catalog.books),
        validateUserExists(request.userId, catalog.users),
        validateBookAvailable(request.isbn, catalog.books),
        validateUserCanBorrow(request.userId, catalog)
      )

      combineValidations(validations).map(_ => request)

    def validateReturnRequest(request: ReturnRequest, catalog: LibraryCatalog): ValidationResult[ReturnRequest] =
      val validations = List(
        validateBookExists(request.isbn, catalog.books),
        validateUserExists(request.userId, catalog.users),
        validateUserHasBook(request.isbn, request.userId, catalog)
      )

      combineValidations(validations).map(_ => request)

    def validateReservationRequest(request: ReservationRequest, catalog: LibraryCatalog): ValidationResult[ReservationRequest] =
      val validations = List(
        validateBookExists(request.isbn, catalog.books),
        validateUserExists(request.userId, catalog.users),
        validateUserCanReserve(request.userId, catalog.users),
        validateDateFormat(request.startDate)
      )

      combineValidations(validations).map(_ => request)

    private def validateBookExists(isbn: String, books: Map[ISBN, Book]): ValidationResult[String] =
      if books.contains(ISBN(isbn)) then Right(isbn)
      else Left(ValidationError.NotFound("Book", isbn))

    private def validateUserExists(userId: String, users: Map[UserID, User]): ValidationResult[String] =
      Try(UUID.fromString(userId)) match
        case Success(uuid) if users.contains(UserID(uuid)) => Right(userId)
        case Success(_) => Left(ValidationError.NotFound("User", userId))
        case Failure(_) => Left(ValidationError.InvalidFormat("userId", userId, "UUID format"))

    private def validateBookAvailable(isbn: String, books: Map[ISBN, Book]): ValidationResult[String] =
      books.get(ISBN(isbn)) match
        case Some(book) if book.isAvailable => Right(isbn)
        case Some(_) => Left(ValidationError.BusinessRule("Book is not available"))
        case None => Left(ValidationError.NotFound("Book", isbn))

    private def validateUserCanBorrow(userId: String, catalog: LibraryCatalog): ValidationResult[String] =
      Try(UUID.fromString(userId)).toOption.flatMap(uuid => catalog.users.get(UserID(uuid))) match
        case Some(user) =>
          val currentLoans = catalog.activeLoansFor(user.id)
          if currentLoans < user.maxLoans then Right(userId)
          else Left(ValidationError.BusinessRule(s"User has reached maximum loan limit (${user.maxLoans})"))
        case None => Left(ValidationError.NotFound("User", userId))

    private def validateUserHasBook(isbn: String, userId: String, catalog: LibraryCatalog): ValidationResult[String] =
      Try(UUID.fromString(userId)).toOption.map(UserID(_)) match
        case Some(userIdParsed) =>
          val hasBook = catalog.transactions.exists {
            case Transaction.Loan(book, user, _, _) if book.isbn.value == isbn && user.id == userIdParsed =>
              !catalog.transactions.exists {
                case Transaction.Return(returnBook, returnUser, _) => 
                  returnBook.isbn.value == isbn && returnUser.id == userIdParsed
                case _ => false
              }
            case _ => false
          }
          if hasBook then Right(userId)
          else Left(ValidationError.BusinessRule("User does not have this book on loan"))
        case None => Left(ValidationError.InvalidFormat("userId", userId, "UUID format"))

    private def validateUserCanReserve(userId: String, users: Map[UserID, User]): ValidationResult[String] =
      Try(UUID.fromString(userId)).toOption.flatMap(uuid => users.get(UserID(uuid))) match
        case Some(user) if user.canReserve => Right(userId)
        case Some(_) => Left(ValidationError.BusinessRule("User type is not allowed to make reservations"))
        case None => Left(ValidationError.NotFound("User", userId))

    private def validateDateFormat(dateStr: String): ValidationResult[String] =
      Try(java.time.LocalDate.parse(dateStr)) match
        case Success(_) => Right(dateStr)
        case Failure(_) => Left(ValidationError.InvalidFormat("startDate", dateStr, "YYYY-MM-DD"))

  // Helper function to combine multiple validations
  private def combineValidations(validations: List[ValidationResult[Any]]): ValidationResult[List[Any]] =
    val (errors, successes) = validations.partitionMap(identity)
    if errors.nonEmpty then
      Left(if errors.length == 1 then errors.head else ValidationError.Multiple(errors))
    else
      Right(successes)

  // Extension methods for easier validation
  extension [T](value: T)
    def validateWith(validator: Validator[T]): ValidationResult[T] = validator.validate(value)

  extension [T](result: ValidationResult[T])
    def getOrThrow: T = result match
      case Right(value) => value
      case Left(error) => throw new IllegalArgumentException(error.message)
    
    def getOrElse(default: T): T = result.getOrElse(default)
    
    def mapError(f: ValidationError => ValidationError): ValidationResult[T] = result.left.map(f)
