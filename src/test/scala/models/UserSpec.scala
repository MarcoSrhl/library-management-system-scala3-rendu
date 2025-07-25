package models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.Types.*

/**
 * Comprehensive test suite for the User model.
 * Tests the User enum hierarchy and its role-based behaviors.
 */
class UserSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  // Test data
  val studentId = UserID.random()
  val facultyId = UserID.random()  
  val librarianId = UserID.random()

  val student = User.Student(studentId, "John Doe", "Computer Science", "password123")
  val faculty = User.Faculty(facultyId, "Dr. Smith", "Mathematics", "securepass")
  val librarian = User.Librarian(librarianId, "Jane Admin", "EMP001", "adminpass")

  // Property-based test generators
  implicit val arbUser: Arbitrary[User] = Arbitrary {
    Gen.oneOf(
      Gen.const(student),
      Gen.const(faculty),
      Gen.const(librarian)
    )
  }

  implicit val arbUserRole: Arbitrary[String] = Arbitrary {
    Gen.oneOf("Student", "Faculty", "Librarian")
  }

  "User.Student" should "create student users correctly" in {
    student.id shouldBe studentId
    student.name shouldBe "John Doe"
    student match {
      case User.Student(id, name, major, password) =>
        id shouldBe studentId
        name shouldBe "John Doe"
        major shouldBe "Computer Science"
        password shouldBe "password123"
      case _ => fail("Expected Student")
    }
  }

  it should "have correct role and permissions" in {
    student.role shouldBe "Student"
    student.canBorrowBooks shouldBe true
    student.canReserveBooks shouldBe true
    student.canManageBooks shouldBe false
    student.canManageUsers shouldBe false
    student.isLibrarian shouldBe false
    student.isFaculty shouldBe false
    student.isStudent shouldBe true
  }

  it should "have loan limits" in {
    student.maxLoansAllowed shouldBe 5
    student.loanPeriodDays shouldBe 30
  }

  "User.Faculty" should "create faculty users correctly" in {
    faculty.id shouldBe facultyId
    faculty.name shouldBe "Dr. Smith"
    faculty match {
      case User.Faculty(id, name, department, password) =>
        id shouldBe facultyId
        name shouldBe "Dr. Smith"
        department shouldBe "Mathematics"
        password shouldBe "securepass"
      case _ => fail("Expected Faculty")
    }
  }

  it should "have correct role and permissions" in {
    faculty.role shouldBe "Faculty"
    faculty.canBorrowBooks shouldBe true
    faculty.canReserveBooks shouldBe true
    faculty.canManageBooks shouldBe false
    faculty.canManageUsers shouldBe false
    faculty.isLibrarian shouldBe false
    faculty.isFaculty shouldBe true
    faculty.isStudent shouldBe false
  }

  it should "have extended loan privileges" in {
    faculty.maxLoansAllowed shouldBe 20
    faculty.loanPeriodDays shouldBe -1 // Unlimited
  }

  "User.Librarian" should "create librarian users correctly" in {
    librarian.id shouldBe librarianId
    librarian.name shouldBe "Jane Admin"
    librarian match {
      case User.Librarian(id, name, employeeId, password) =>
        id shouldBe librarianId
        name shouldBe "Jane Admin"
        employeeId shouldBe "EMP001"
        password shouldBe "adminpass"
      case _ => fail("Expected Librarian")
    }
  }

  it should "have correct role and permissions" in {
    librarian.role shouldBe "Librarian"
    librarian.canBorrowBooks shouldBe true
    librarian.canReserveBooks shouldBe true
    librarian.canManageBooks shouldBe true
    librarian.canManageUsers shouldBe true
    librarian.isLibrarian shouldBe true
    librarian.isFaculty shouldBe false
    librarian.isStudent shouldBe false
  }

  it should "have unlimited loan privileges" in {
    librarian.maxLoansAllowed shouldBe Int.MaxValue
    librarian.loanPeriodDays shouldBe -1 // Unlimited
  }

  "User authentication" should "authenticate with correct password" in {
    student.authenticate("password123") shouldBe true
    faculty.authenticate("securepass") shouldBe true
    librarian.authenticate("adminpass") shouldBe true
  }

  it should "reject incorrect passwords" in {
    student.authenticate("wrongpass") shouldBe false
    faculty.authenticate("wrongpass") shouldBe false
    librarian.authenticate("wrongpass") shouldBe false
  }

  it should "be case-sensitive" in {
    student.authenticate("PASSWORD123") shouldBe false
    faculty.authenticate("SECUREPASS") shouldBe false
  }

  it should "reject empty passwords" in {
    student.authenticate("") shouldBe false
    faculty.authenticate("") shouldBe false
    librarian.authenticate("") shouldBe false
  }

  "User extension methods" should "extract id correctly" in {
    student.id shouldBe studentId
    faculty.id shouldBe facultyId
    librarian.id shouldBe librarianId
  }

  it should "extract name correctly" in {
    student.name shouldBe "John Doe"
    faculty.name shouldBe "Dr. Smith"
    librarian.name shouldBe "Jane Admin"
  }

  it should "extract password correctly" in {
    student.password shouldBe "password123"
    faculty.password shouldBe "securepass"
    librarian.password shouldBe "adminpass"
  }

  "User role checks" should "identify roles correctly" in {
    student.isStudent shouldBe true
    student.isFaculty shouldBe false
    student.isLibrarian shouldBe false

    faculty.isStudent shouldBe false
    faculty.isFaculty shouldBe true
    faculty.isLibrarian shouldBe false

    librarian.isStudent shouldBe false
    librarian.isFaculty shouldBe false
    librarian.isLibrarian shouldBe true
  }

  "User permissions" should "grant appropriate borrowing rights" in {
    List(student, faculty, librarian).foreach { user =>
      user.canBorrowBooks shouldBe true
      user.canReserveBooks shouldBe true
    }
  }

  it should "restrict management rights appropriately" in {
    student.canManageBooks shouldBe false
    student.canManageUsers shouldBe false

    faculty.canManageBooks shouldBe false
    faculty.canManageUsers shouldBe false

    librarian.canManageBooks shouldBe true
    librarian.canManageUsers shouldBe true
  }

  "User loan limits" should "vary by role" in {
    student.maxLoansAllowed shouldBe 5
    faculty.maxLoansAllowed shouldBe 20
    librarian.maxLoansAllowed shouldBe Int.MaxValue
  }

  it should "have appropriate loan periods" in {
    student.loanPeriodDays shouldBe 30
    faculty.loanPeriodDays shouldBe -1 // Unlimited
    librarian.loanPeriodDays shouldBe -1 // Unlimited
  }

  "User pattern matching" should "work correctly with different types" in {
    val users = List(student, faculty, librarian)
    
    val studentCount = users.count {
      case _: User.Student => true
      case _ => false
    }
    
    val facultyCount = users.count {
      case _: User.Faculty => true
      case _ => false
    }
    
    val librarianCount = users.count {
      case _: User.Librarian => true
      case _ => false
    }
    
    studentCount shouldBe 1
    facultyCount shouldBe 1
    librarianCount shouldBe 1
  }

  it should "extract specific fields through pattern matching" in {
    student match {
      case User.Student(id, name, major, password) =>
        id shouldBe studentId
        name shouldBe "John Doe"
        major shouldBe "Computer Science"
        password shouldBe "password123"
      case _ => fail("Expected Student")
    }
    
    faculty match {
      case User.Faculty(id, name, department, password) =>
        id shouldBe facultyId
        name shouldBe "Dr. Smith"
        department shouldBe "Mathematics"
        password shouldBe "securepass"
      case _ => fail("Expected Faculty")
    }
    
    librarian match {
      case User.Librarian(id, name, employeeId, password) =>
        id shouldBe librarianId
        name shouldBe "Jane Admin"
        employeeId shouldBe "EMP001"
        password shouldBe "adminpass"
      case _ => fail("Expected Librarian")
    }
  }

  // Property-based tests
  "User property-based tests" should "maintain type consistency" in {
    forAll { (user: User) =>
      user match {
        case student: User.Student =>
          student.isStudent shouldBe true
          student.isFaculty shouldBe false
          student.isLibrarian shouldBe false
          student.role shouldBe "Student"
        case faculty: User.Faculty =>
          faculty.isStudent shouldBe false
          faculty.isFaculty shouldBe true
          faculty.isLibrarian shouldBe false
          faculty.role shouldBe "Faculty"
        case librarian: User.Librarian =>
          librarian.isStudent shouldBe false
          librarian.isFaculty shouldBe false
          librarian.isLibrarian shouldBe true
          librarian.role shouldBe "Librarian"
      }
    }
  }

  it should "always have valid authentication" in {
    forAll { (user: User) =>
      // Password should authenticate successfully with itself
      user.authenticate(user.password) shouldBe true
      
      // Wrong password should fail
      user.authenticate("wrongpassword") shouldBe false
      
      // Empty password should fail
      user.authenticate("") shouldBe false
    }
  }

  it should "have consistent permission logic" in {
    forAll { (user: User) =>
      // All users can borrow and reserve books
      user.canBorrowBooks shouldBe true
      user.canReserveBooks shouldBe true
      
      // Only librarians can manage books and users
      if (user.isLibrarian) {
        user.canManageBooks shouldBe true
        user.canManageUsers shouldBe true
      } else {
        user.canManageBooks shouldBe false
        user.canManageUsers shouldBe false
      }
    }
  }

  "User collections" should "support filtering by role" in {
    val users = List(
      User.Student(UserID.random(), "Student1", "CS", "pass1"),
      User.Faculty(UserID.random(), "Faculty1", "Math", "pass2"),
      User.Librarian(UserID.random(), "Librarian1", "EMP001", "pass3"),
      User.Student(UserID.random(), "Student2", "Physics", "pass4"),
      User.Faculty(UserID.random(), "Faculty2", "Chemistry", "pass5")
    )
    
    val students = users.filter(_.isStudent)
    val faculty = users.filter(_.isFaculty)
    val librarians = users.filter(_.isLibrarian)
    
    students should have size 2
    faculty should have size 2
    librarians should have size 1
  }

  it should "support grouping by role" in {
    val users = List(student, faculty, librarian, student, faculty)
    
    val grouped = users.groupBy(_.role)
    
    grouped("Student") should have size 2
    grouped("Faculty") should have size 2
    grouped("Librarian") should have size 1
  }

  it should "support sorting by name" in {
    val users = List(
      User.Student(UserID.random(), "Charlie", "CS", "pass"),
      User.Faculty(UserID.random(), "Alice", "Math", "pass"),
      User.Librarian(UserID.random(), "Bob", "EMP001", "pass")
    )
    
    val sorted = users.sortBy(_.name)
    sorted.map(_.name) shouldBe List("Alice", "Bob", "Charlie")
  }

  "User business logic" should "handle role-based operations correctly" in {
    val allUsers = List(student, faculty, librarian)
    
    // Only librarians should be able to manage system
    val admins = allUsers.filter(_.canManageUsers)
    admins should have size 1
    admins.head shouldBe librarian
    
    // All should be able to borrow
    val borrowers = allUsers.filter(_.canBorrowBooks)
    borrowers should have size 3
  }

  it should "enforce loan limits correctly" in {
    student.maxLoansAllowed should be <= 10 // Reasonable upper bound
    faculty.maxLoansAllowed should be > student.maxLoansAllowed
    librarian.maxLoansAllowed shouldBe Int.MaxValue
  }

  it should "handle loan periods appropriately" in {
    student.loanPeriodDays should be > 0 // Students have time limits
    faculty.loanPeriodDays shouldBe -1 // Faculty unlimited
    librarian.loanPeriodDays shouldBe -1 // Librarians unlimited
  }

  "User equality and identity" should "be based on ID" in {
    val sameId = studentId
    val differentStudent = User.Student(sameId, "Different Name", "Different Major", "differentpass")
    
    // Users with same ID should be considered equal for business logic
    student.id shouldBe differentStudent.id
  }

  it should "have unique IDs" in {
    val users = (1 to 100).map(_ => 
      User.Student(UserID.random(), "Test User", "Test Major", "password")
    ).toList
    
    val uniqueIds = users.map(_.id).toSet
    uniqueIds should have size 100 // All IDs should be unique
  }
}
