# Library Management System - Design Document

## 1. Overview

The Library Management System is a comprehensive Scala 3 application demonstrating advanced functional programming concepts and modern language features. This document outlines the architectural decisions, design patterns, and implementation strategies used throughout the system.

## 2. Architectural Decisions

### 2.1 Functional Programming Paradigm

**Decision**: Adopt pure functional programming with immutable data structures.

**Rationale**:
- Eliminates side effects and makes code more predictable
- Enables easier testing and reasoning about code behavior
- Leverages Scala 3's advanced type system for compile-time guarantees
- Supports concurrent operations without synchronization concerns

**Implementation**:
- All domain models are immutable case classes
- State changes return new instances rather than mutating existing ones
- Side effects are isolated to specific layers (I/O operations)

### 2.2 Type-Driven Development

**Decision**: Leverage Scala 3's advanced type system for domain modeling.

**Rationale**:
- Prevents entire categories of runtime errors at compile time
- Makes the code self-documenting through expressive types
- Enables compiler-assisted refactoring and maintenance

**Implementation**:
- Opaque types for domain-specific primitives (ISBN, UserID)
- Union types for type-safe error handling
- Enums for rich domain hierarchies
- Extension methods for enhanced APIs

### 2.3 Layered Architecture

**Decision**: Organize code into distinct layers with clear responsibilities.

**Structure**:
```
models/    <- Domain entities and business rules
services/  <- Business logic and orchestration
utils/     <- Infrastructure and cross-cutting concerns
```

**Benefits**:
- Clear separation of concerns
- Easy to test individual components
- Supports modular development and maintenance

## 3. Design Patterns

### 3.1 Type Class Pattern

**Implementation**: `utils/TypeClasses.scala`

**Purpose**: Enable ad-hoc polymorphism for domain types without inheritance.

**Examples**:
- `Displayable[T]`: Consistent display formatting across types
- `Validatable[T]`: Validation logic for domain objects
- `Similarity[T]`: Similarity comparison for recommendation engine

**Benefits**:
- Retroactive extension of types with new capabilities
- Type-safe polymorphism without runtime overhead
- Clean separation of data and behavior

### 3.2 Extension Methods Pattern

**Implementation**: `utils/Extensions.scala` and throughout domain models

**Purpose**: Enhance existing types with domain-specific operations.

**Examples**:
```scala
extension (books: List[Book])
  def availableOnly: List[Book] = books.filter(_.isAvailable)
  def byGenre(genre: String): List[Book] = books.filter(_.genre.contains(genre))
  def displaySummary: String = // it generates summary statistics
```

**Benefits**:
- Natural, fluent APIs
- Composition over inheritance
- Type-safe method chaining

### 3.3 Union Types for Error Handling

**Implementation**: `models/Results.scala`

**Purpose**: Type-safe error handling without exceptions.

**Design**:
```scala
type SearchResult[T] = Success[T] | LibraryError

enum LibraryError:
  case BookNotFound(isbn: String)
  case UserNotFound(userId: String)
  case ValidationError(message: String)
  // ... other error types
```

**Benefits**:
- Compile-time guarantee that errors are handled
- Expressive error types that carry context
- Functional composition of error-prone operations

### 3.4 Opaque Types for Domain Safety

**Implementation**: `utils/Types.scala`

**Purpose**: Zero-cost abstractions for domain-specific types.

**Design**:
```scala
opaque type ISBN = String
opaque type UserID = UUID
opaque type BookTitle = String

object ISBN:
  def apply(value: String): ISBN = value
  
extension (isbn: ISBN)
  def value: String = isbn
  def isValid: Boolean = // ISBN validation logic
```

**Benefits**:
- Type safety without runtime overhead
- Prevents mixing up similar primitive types
- Enables domain-specific validation and operations

## 4. Domain Model Design

### 4.1 User Hierarchy

**Implementation**: Scala 3 enums with rich behavior

```scala
enum User:
  case Student(id: UserID, name: String, major: String, password: String)
  case Faculty(id: UserID, name: String, department: String, password: String)
  case Librarian(id: UserID, name: String, employeeId: String, password: String)
```

**Design Decisions**:
- Enums provide pattern matching exhaustiveness checking
- Role-based permissions encoded in extension methods
- Authentication integrated into the type system

### 4.2 Transaction System

**Implementation**: Event-driven architecture with transaction types

```scala
enum Transaction:
  case Loan(book: Book, user: User, timestamp: LocalDateTime, dueDate: Option[LocalDateTime])
  case Return(book: Book, user: User, timestamp: LocalDateTime)
  case Reservation(book: Book, user: User, timestamp: LocalDateTime, startDate: LocalDateTime, endDate: LocalDateTime)
```

**Design Decisions**:
- Immutable event log maintains complete system history
- Complex business rules implemented through helper methods
- Temporal queries support availability checking and fee calculation

### 4.3 Error Handling Strategy

**Approach**: Functional error handling with multiple strategies

1. **Option Types**: For nullable values and optional results
2. **Either Types**: For operations that can fail with context
3. **Try Types**: For exception-prone operations (I/O)
4. **Union Types**: For type-safe error hierarchies

**Example**:
```scala
def loanBook(isbn: ISBN, userId: UserID): Either[LibraryError, LibraryCatalog] =
  for
    book <- findBook(isbn).toRight(LibraryError.BookNotFound(isbn.value))
    user <- findUser(userId).toRight(LibraryError.UserNotFound(userId.value))
    _ <- validateLoanEligibility(book, user)
    updatedCatalog <- processLoan(book, user)
  yield updatedCatalog
```

## 5. Performance Considerations

### 5.1 Immutable Collections

**Choice**: Scala's persistent data structures

**Benefits**:
- Structural sharing minimizes memory overhead
- Thread-safe by default
- Efficient functional operations

**Trade-offs**:
- Slightly higher memory usage than mutable alternatives
- Copy-on-write semantics for large collections

### 5.2 Search Operations

**Implementation**: Multi-criteria search with similarity algorithms

**Optimizations**:
- Lazy evaluation for large datasets
- Indexed search for common queries
- Configurable similarity thresholds

### 5.3 JSON Serialization

**Choice**: uPickle library for serialization

**Benefits**:
- Macro-based code generation for performance
- Type-safe serialization/deserialization
- Support for Scala 3 language features

## 6. Testing Strategy

### 6.1 Property-Based Testing

**Implementation**: ScalaCheck generators for domain objects

**Benefits**:
- Tests business invariants across large input spaces
- Discovers edge cases that unit tests might miss
- Provides confidence in complex algorithms

**Example**:
```scala
property("loan limits are enforced") {
  forAll(userGen, bookListGen) { (user, books) =>
    val loans = books.take(user.maxLoansAllowed + 1)
    val catalog = processLoans(user, loans)
    catalog.activeLoansFor(user.id) <= user.maxLoansAllowed
  }
}
```

### 6.2 Integration Testing

**Approach**: End-to-end testing of business workflows

**Coverage**:
- Complete user journeys (registration → loan → return)
- Administrative operations
- Error recovery scenarios
- Persistence operations

### 6.3 Test Organization

**Structure**:
- Unit tests for individual components
- Property-based tests for algorithms
- Integration tests for workflows
- Performance benchmarks for critical operations

## 7. Extensibility and Maintenance

### 7.1 Adding New Features

**Process**:
1. Define new domain types in `models/`
2. Add business logic to `services/`
3. Create supporting utilities in `utils/`
4. Write comprehensive tests

**Example**: Adding a new user type
```scala
enum User:
  // ... existing cases
  case Visitor(id: UserID, name: String, email: String)
```

### 7.2 Configuration Management

**Approach**: Type-safe configuration with defaults

**Implementation**:
- Loan limits and periods configurable per user type
- Search similarity thresholds adjustable
- I/O timeouts and retry logic configurable

### 7.3 Error Handling Evolution

**Strategy**: Centralized error definitions with contextual information

**Benefits**:
- Easy to add new error types
- Consistent error handling across the application
- Rich error context for debugging and user feedback

## 8. Conclusion

This design achieves the goals of demonstrating advanced Scala 3 features while building a practical, maintainable system. The functional programming approach, combined with Scala 3's type system, provides both safety and expressiveness. The modular architecture supports extension and maintenance, while comprehensive testing ensures reliability. The system serves as an excellent example of modern Scala development practices and can be used as a reference for similar projects requiring type safety, functional programming, and comprehensive error handling. 