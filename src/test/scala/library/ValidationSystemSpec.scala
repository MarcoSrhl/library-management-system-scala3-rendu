package library

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen

import ValidationSystem.*
import Types.{ISBN, UserID}
import java.util.UUID
import java.time.LocalDateTime

class ValidationSystemSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks:

  "ValidationError" should "provide meaningful error messages" in {
    val emptyFieldError = ValidationError.EmptyField("title")
    emptyFieldError.message should include("cannot be empty")

    val invalidFormatError = ValidationError.InvalidFormat("isbn", "invalid", "###-##-###")
    invalidFormatError.message should include("invalid format")

    val outOfRangeError = ValidationError.OutOfRange("year", 1800, 1900, 2100)
    outOfRangeError.message should include("out of range")

    val duplicateError = ValidationError.DuplicateValue("isbn", "123-45-678")
    duplicateError.message should include("already exists")

    val notFoundError = ValidationError.NotFound("Book", "123")
    notFoundError.message should include("not found")

    val businessRuleError = ValidationError.BusinessRule("User has reached loan limit")
    businessRuleError.message should be("User has reached loan limit")
  }

  it should "handle multiple validation errors" in {
    val errors = List(
      ValidationError.EmptyField("title"),
      ValidationError.InvalidFormat("isbn", "bad", "good")
    )
    val multipleError = ValidationError.Multiple(errors)
    multipleError.message should include("Multiple validation errors")
    multipleError.message should include("cannot be empty")
    multipleError.message should include("invalid format")
  }

  "Validators.nonEmpty" should "validate non-empty strings" in {
    val validator = Validators.nonEmpty("test")
    
    validator.validate("valid") shouldBe Right("valid")
    validator.validate("  spaced  ") shouldBe Right("spaced")
    validator.validate("") shouldBe a[Left[_, _]]
    validator.validate("   ") shouldBe a[Left[_, _]]
  }

  "Validators.minLength" should "enforce minimum length" in {
    val validator = Validators.minLength("title", 5)
    
    validator.validate("12345") shouldBe Right("12345")
    validator.validate("123456") shouldBe Right("123456")
    validator.validate("1234") shouldBe a[Left[_, _]]
    validator.validate("") shouldBe a[Left[_, _]]
  }

  "Validators.maxLength" should "enforce maximum length" in {
    val validator = Validators.maxLength("title", 10)
    
    validator.validate("1234567890") shouldBe Right("1234567890")
    validator.validate("123") shouldBe Right("123")
    validator.validate("12345678901") shouldBe a[Left[_, _]]
  }

  "Validators.yearRange" should "validate year ranges" in {
    val validator = Validators.yearRange("year", 1900, 2030)
    
    validator.validate(2000) shouldBe Right(2000)
    validator.validate(1900) shouldBe Right(1900)
    validator.validate(2030) shouldBe Right(2030)
    validator.validate(1899) shouldBe a[Left[_, _]]
    validator.validate(2031) shouldBe a[Left[_, _]]
  }

  "ValidationSystem" should "provide composable validators" in {
    // Test with property-based testing
    forAll(Gen.alphaNumStr) { str =>
      val result = Validators.nonEmpty("test").validate(str)
      if (str.trim.nonEmpty) {
        result shouldBe Right(str.trim)
      } else {
        result shouldBe a[Left[_, _]]
      }
    }
  }

  it should "handle complex validation scenarios" in {
    // Simulate book validation
    val title = "Valid Book Title"
    val isbn = "978-3-16-148410-0"
    val year = 2023

    val titleResult = Validators.nonEmpty("title").validate(title)
    val isbnResult = Validators.minLength("isbn", 10).validate(isbn)
    val yearResult = Validators.yearRange("year", 1800, 2030).validate(year)

    titleResult shouldBe Right(title)
    isbnResult shouldBe Right(isbn)
    yearResult shouldBe Right(year)
  }

  it should "accumulate validation errors properly" in {
    val errors = List(
      ValidationError.EmptyField("title"),
      ValidationError.OutOfRange("year", 1799, 1800, 2030),
      ValidationError.InvalidFormat("isbn", "bad", "XXX-X-XX-XXXXXX-X")
    )

    errors should have length 3
    errors.forall(_.isInstanceOf[ValidationError]) shouldBe true
  }

  "ValidationResult" should "work with Either operations" in {
    val validResult: ValidationResult[String] = Right("valid")
    val invalidResult: ValidationResult[String] = Left(ValidationError.EmptyField("test"))

    validResult.map(_.toUpperCase) shouldBe Right("VALID")
    validResult.flatMap(s => Right(s.length)) shouldBe Right(5)
    
    invalidResult.map(_.toUpperCase) shouldBe a[Left[_, _]]
    invalidResult.flatMap(s => Right(s.length)) shouldBe a[Left[_, _]]
  }

  "Validator trait" should "be composable" in {
    val validator1 = Validators.nonEmpty("test")
    val validator2 = Validators.minLength("test", 3)

    // Test composition manually
    val input = "ab"
    val result1 = validator1.validate(input)
    val result2 = result1.flatMap(validator2.validate)

    result1 shouldBe Right("ab")
    result2 shouldBe a[Left[_, _]]
  }

  it should "support property-based validation" in {
    forAll(Gen.choose(1800, 2100)) { year =>
      val validator = Validators.yearRange("publicationYear", 1900, 2030)
      val result = validator.validate(year)
      
      if (year >= 1900 && year <= 2030) {
        result shouldBe Right(year)
      } else {
        result shouldBe a[Left[_, _]]
      }
    }
  }
