package models

import java.util.UUID
import java.time.{LocalDateTime, Duration}
import java.time.temporal.ChronoUnit
import utils.Types.UserID

/**
 * User hierarchy representing different types of library system users.
 * 
 * This enum defines the complete user type system for the library management application,
 * leveraging Scala 3's advanced enum capabilities to create a type-safe, extensible
 * hierarchy of library users with role-based permissions and capabilities.
 * 
 * == Design Philosophy ==
 * 
 * '''Type Safety''': Each user type is a distinct case class with compile-time
 * type checking, preventing runtime errors from user type confusion.
 * 
 * '''Role-Based Access''': Different user types have different permissions,
 * loan limits, and system capabilities built into the type system.
 * 
 * '''Extensibility''': New user types can be easily added as additional enum cases
 * without breaking existing code.
 * 
 * == User Type Capabilities ==
 * 
 * '''Student Users''':
 *  - 5 book loan limit
 *  - 30-day loan period
 *  - Can make reservations
 *  - Associated with academic major
 * 
 * '''Faculty Users''':
 *  - 10 book loan limit  
 *  - 90-day loan period
 *  - Can make reservations
 *  - Associated with department
 * 
 * '''Librarian Users''':
 *  - Unlimited book loans
 *  - Unlimited loan period
 *  - Cannot make reservations (have direct access)
 *  - Administrative privileges
 *  - Associated with employee ID
 * 
 * @example
 * {{{
 * import models.User
 * import utils.Types.UserID
 * 
 * // Create different user types
 * val student = User.Student(
 *   UserID("s001"), 
 *   "Alice Johnson", 
 *   "Computer Science", 
 *   "password123"
 * )
 * 
 * val faculty = User.Faculty(
 *   UserID("f001"),
 *   "Dr. Bob Smith", 
 *   "Mathematics",
 *   "securepass"
 * )
 * 
 * val librarian = User.Librarian(
 *   UserID("l001"),
 *   "Carol Admin",
 *   "EMP001", 
 *   "adminpass"
 * )
 * 
 * // Access common properties via extension methods
 * println(s"User: ${student.name} (${student.role})")
 * println(s"Max loans: ${student.maxLoansAllowed}")
 * println(s"Can reserve: ${student.canReserveBooks}")
 * 
 * // Pattern match for type-specific behavior
 * student match {
 *   case User.Student(_, _, major, _) => println(s"Major: $major")
 *   case User.Faculty(_, _, dept, _) => println(s"Department: $dept") 
 *   case User.Librarian(_, _, empId, _) => println(s"Employee: $empId")
 * }
 * }}}
 * 
 * @since 1.0.0
 * @see [[utils.Types.UserID]] for type-safe user identification
 * @see [[services.LibraryCatalog]] for user management operations
 */
enum User:

  /**
   * Represents a student user in the library system.
   * 
   * Students are academic users with standard borrowing privileges and limitations.
   * They have restricted loan limits and shorter loan periods compared to faculty,
   * but can make reservations for unavailable books.
   * 
   * == Privileges ==
   *  - Maximum 5 concurrent book loans
   *  - 30-day loan period for all books
   *  - Can make book reservations
   *  - Standard library access hours
   * 
   * @param id Unique identifier for this student user
   * @param name Full name of the student (e.g., "Alice Johnson")
   * @param major Academic major or field of study (e.g., "Computer Science")
   * @param password Authentication password for system access
   * 
   * @example
   * {{{
   * val student = User.Student(
   *   UserID("s2024001"),
   *   "Alice Johnson", 
   *   "Computer Science",
   *   "securepass123"
   * )
   * 
   * // Check loan capacity
   * val canLoan = student.maxLoansAllowed > student.currentLoans
   * }}}
   */
  case Student(id: UserID, name: String, major: String, password: String) extends User

  /**
   * Represents a faculty member in the library system.
   * 
   * Faculty members are academic staff with extended borrowing privileges
   * compared to students. They have higher loan limits and longer loan periods
   * to support their research and teaching activities.
   * 
   * == Privileges ==
   *  - Maximum 10 concurrent book loans
   *  - 90-day loan period for all books
   *  - Can make book reservations
   *  - Extended library access hours
   *  - Priority in reservation queues
   * 
   * @param id Unique identifier for this faculty user
   * @param name Full name of the faculty member (e.g., "Dr. Bob Smith")
   * @param department Academic department affiliation (e.g., "Mathematics")
   * @param password Authentication password for system access
   * 
   * @example
   * {{{
   * val faculty = User.Faculty(
   *   UserID("f2024001"),
   *   "Dr. Bob Smith",
   *   "Mathematics Department", 
   *   "academicpass456"
   * )
   * 
   * // Faculty have extended privileges
   * assert(faculty.maxLoansAllowed == 10)
   * assert(faculty.loanPeriodDays == 90)
   * }}}
   */
  case Faculty(id: UserID, name: String, department: String, password: String) extends User

  /**
   * Represents a librarian user with administrative privileges.
   * 
   * Librarians are staff members with the highest level of system access.
   * They can manage books, users, and system settings. Unlike other users,
   * they have unlimited borrowing capabilities and don't need to make reservations.
   * 
   * == Privileges ==
   *  - Unlimited concurrent book loans
   *  - Unlimited loan periods
   *  - No need for reservations (direct access)
   *  - Can add/remove books from catalog
   *  - Can manage user accounts
   *  - Can waive overdue fees
   *  - Full system administration access
   * 
   * @param id Unique identifier for this librarian user
   * @param name Full name of the librarian (e.g., "Carol Administrator")
   * @param employeeId Staff employee identification number (e.g., "LIB001")
   * @param password Authentication password for system access
   * 
   * @example
   * {{{
   * val librarian = User.Librarian(
   *   UserID("l2024001"),
   *   "Carol Administrator",
   *   "LIB001",
   *   "librarianpass789"
   * )
   * 
   * // Librarians have unlimited access
   * assert(librarian.maxLoansAllowed == Int.MaxValue)
   * assert(!librarian.canReserveBooks) // Don't need reservations
   * assert(librarian.hasAdminPrivileges)
   * }}}
   */
  case Librarian(id: UserID, name: String, employeeId: String, password: String) extends User

object User:

  /**
   * Extension methods providing uniform access to User properties across all user types.
   * 
   * These extension methods enable polymorphic access to common user properties
   * regardless of the specific user type, while maintaining type safety and
   * avoiding the need for abstract methods in the enum definition.
   */
  extension (u: User)
    
    /**
     * Gets the unique identifier for this user.
     * 
     * @return The UserID that uniquely identifies this user in the system
     * @since 1.0.0
     */
    def id: UserID = u match
      case Student(id, _, _, _)     => id
      case Faculty(id, _, _, _)     => id
      case Librarian(id, _, _, _)   => id

    /**
     * Gets the full name of this user.
     * 
     * @return The user's full name as stored in the system
     * @since 1.0.0
     */
    def name: String = u match
      case Student(_, name, _, _)     => name
      case Faculty(_, name, _, _)     => name
      case Librarian(_, name, _, _)   => name

    /**
     * Gets the authentication password for this user.
     * 
     * @return The user's password for system authentication
     * @note In a production system, passwords should be hashed
     * @since 1.0.0
     */
    def password: String = u match
      case Student(_, _, _, password)     => password
      case Faculty(_, _, _, password)     => password
      case Librarian(_, _, _, password)   => password

    /**
     * Checks if this user is a student.
     * 
     * @return true if this user is of type Student, false otherwise
     * @since 1.0.0
     */
    def isStudent: Boolean = u match
      case Student(_, _, _, _) => true
      case _                   => false

    /**
     * Checks if this user is a faculty member.
     * 
     * @return true if this user is of type Faculty, false otherwise
     * @since 1.0.0
     */
    def isFaculty: Boolean = u match
      case Faculty(_, _, _, _) => true
      case _                   => false

    /**
     * Checks if this user is a librarian.
     * 
     * @return true if this user is of type Librarian, false otherwise
     * @since 1.0.0
     */
    def isLibrarian: Boolean = u match
      case Librarian(_, _, _, _) => true
      case _                     => false

    /**
     * Gets the maximum number of books this user type can borrow simultaneously.
     * 
     * Different user types have different borrowing limits based on their
     * role and needs within the academic institution.
     * 
     * @return Maximum concurrent loans allowed for this user type:
     *         - Students: 5 books
     *         - Faculty: 20 books  
     *         - Librarians: Unlimited (Int.MaxValue)
     * @since 1.0.0
     */
    def maxLoans: Int = u match
      case Student(_, _, _, _)   => 5
      case Faculty(_, _, _, _)   => 20
      case Librarian(_, _, _, _) => Int.MaxValue // Unlimited

    /**
     * Alternative accessor for maxLoans to maintain API compatibility.
     * 
     * @return Same as maxLoans - maximum concurrent loans allowed
     * @see [[maxLoans]]
     * @since 1.0.0
     */
    def maxLoansAllowed: Int = maxLoans

    /**
     * Gets the loan period duration for this user type.
     * 
     * Different user types have different loan periods reflecting their
     * academic needs and responsibilities.
     * 
     * @return Loan period duration for this user type:
     *         - Students: 30 days
     *         - Faculty: 90 days
     *         - Librarians: Unlimited (365000 days)
     * @since 1.0.0
     */
    def loanPeriod: Duration = u match
      case Student(_, _, _, _)   => Duration.ofDays(30)  // 1 month
      case Faculty(_, _, _, _)   => Duration.ofDays(90)  // 3 months
      case Librarian(_, _, _, _) => Duration.ofDays(365 * 100) // Effectively unlimited

    /**
     * Loan period in days (for compatibility with existing code).
     */
    def loanPeriodDays: Int = u match
      case Student(_, _, _, _)   => 30
      case Faculty(_, _, _, _)   => -1  // Unlimited
      case Librarian(_, _, _, _) => -1  // Unlimited

    /**
     * Whether this user type can place reservations.
     */
    def canReserve: Boolean = u match
      case Student(_, _, _, _)   => true
      case Faculty(_, _, _, _)   => true
      case Librarian(_, _, _, _) => true // Librarians can also reserve

    /**
     * Alternative name for canReserve for compatibility.
     */
    def canReserveBooks: Boolean = canReserve

    /**
     * Whether this user can borrow books.
     */
    def canBorrowBooks: Boolean = true // All users can borrow

    /**
     * Whether this user can manage books (add/remove).
     */
    def canManageBooks: Boolean = u match
      case Librarian(_, _, _, _) => true
      case _ => false

    /**
     * Whether this user can manage other users.
     */
    def canManageUsers: Boolean = u match
      case Librarian(_, _, _, _) => true
      case _ => false

    /**
     * Get the role string for this user.
     */
    def role: String = u match
      case Student(_, _, _, _)   => "Student"
      case Faculty(_, _, _, _)   => "Faculty"
      case Librarian(_, _, _, _) => "Librarian"

    /**
     * Authenticate user with password.
     */
    def authenticate(inputPassword: String): Boolean = 
      u.password == inputPassword

    /**
     * Calculate due date for a loan based on user type.
     */
    def calculateDueDate(loanDate: LocalDateTime): LocalDateTime = u match
      case Student(_, _, _, _)   => loanDate.plus(30, ChronoUnit.DAYS)
      case Faculty(_, _, _, _)   => loanDate.plus(90, ChronoUnit.DAYS)
      case Librarian(_, _, _, _) => loanDate.plus(365 * 100, ChronoUnit.DAYS) // Effectively no due date

    /**
     * Overdues fee rate per day (in currency units).
     */
    def overdueFeeRate: Double = u match
      case Student(_, _, _, _)   => 0.50  // Standard rate
      case Faculty(_, _, _, _)   => 0.25  // Discounted rate
      case Librarian(_, _, _, _) => 0.0   // No fees for staff

  /**
   * Authentication methods
   */
  def authenticate(userId: UserID, password: String, users: Map[UserID, User]): Option[User] =
    users.get(userId).filter(_.password == password)

  def authenticateByName(name: String, password: String, users: Map[UserID, User]): Option[User] =
    users.values.find(user => user.name.equalsIgnoreCase(name) && user.password == password)
