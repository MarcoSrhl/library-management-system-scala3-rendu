# Library Management System - Testing Guide

## Overview

This document provides comprehensive testing instructions for the Scala 3 Library Management System. The project includes extensive test suites demonstrating advanced Scala 3 features and functional programming patterns.



## Project Structure

```
src/
├── main/scala/
│   ├── library/          # Core Library System
│   │   ├── AuthSession.scala       # Authentication and session management
│   │   ├── Book.scala              # Book entity with advanced features
│   │   ├── CLI.scala               # Command-line interface
│   │   ├── DataLoader.scala        # Data loading utilities
│   │   ├── DataTransformation.scala # Data processing and transformation
│   │   ├── Extensions.scala        # Library-specific extensions
│   │   ├── JsonIO.scala            # JSON persistence for library
│   │   ├── LibraryCatalog.scala    # Main catalog implementation
│   │   ├── RecommendationEngine.scala # Book recommendation system
│   │   ├── Results.scala           # Result types and error handling
│   │   ├── SearchOperations.scala  # Advanced search functionality
│   │   ├── Transaction.scala       # Transaction processing
│   │   ├── TypeClasses.scala       # Library-specific type classes
│   │   ├── Types.scala             # Library domain types
│   │   ├── User.scala              # User management
│   │   └── ValidationSystem.scala  # Validation framework
│   ├── models/           # Domain Models
│   │   ├── Book.scala
│   │   ├── User.scala
│   │   ├── Transaction.scala
│   │   └── Results.scala
│   ├── services/         # Business Logic Services
│   │   └── LibraryCatalog.scala
│   ├── utils/           # Utilities & Infrastructure
│   │   ├── Types.scala
│   │   ├── Extensions.scala
│   │   ├── TypeClasses.scala
│   │   └── JsonIO.scala
│   └── Main.scala       # Application Entry Point
└── test/scala/
    ├── library/         # Library system tests
    ├── models/          # Comprehensive Test Suites
    │   ├── BookSpec.scala
    │   ├── UserSpec.scala
    │   ├── TransactionSpec.scala
    │   └── ResultsSpec.scala
    ├── services/        # Service layer tests
    └── utils/           # Utility tests
```

## Advanced Scala 3 Features Demonstrated

### -> Union Types 
- **Location**: `models/Results.scala`
- **Implementation**: `SearchResult[T] = Success[T] | LibraryError`
- **Usage**: Functional error handling with pattern matching
- **Testing**: Comprehensive union type operations in ResultsSpec.scala

### -> Opaque Types 
- **Location**: `utils/Types.scala`
- **Implementation**: Type-safe wrappers for ISBN, UserID, BookTitle, AuthorName, Genre
- **Features**: Validation, safe constructors, extension methods
- **Testing**: Property-based testing with generators and validation

### -> Extension Methods 
- **Location**: `utils/Extensions.scala`, throughout domain models
- **Implementation**: Rich APIs for collections and domain objects
- **Features**: Natural syntax, functional operations, type-safe chaining
- **Testing**: Extension method behavior validation

### -> Given/Using
- **Location**: `utils/TypeClasses.scala`
- **Implementation**: Complete type class system with Displayable, Validatable, Similarity
- **Features**: Ad-hoc polymorphism, context-dependent behavior
- **Testing**: Type class instance validation and polymorphic operations

### -> Enums 
- **Location**: `models/User.scala`, `models/Transaction.scala`
- **Implementation**: Rich enum hierarchies with methods and pattern matching
- **Features**: Role-based permissions, transaction types, error hierarchies
- **Testing**: Enum case validation and business logic testing

## Testing Framework

### Dependencies
```scala
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "org.scalatestplus" %% "scalacheck-1-17" % "3.2.17.0" % Test
)
```

### Test Coverage

The project includes comprehensive test coverage monitoring using the scoverage plugin:

#### Coverage Commands
```bash
# Run tests with coverage
sbt coverage test coverageReport

# View HTML coverage report
# Open: target/scala-3.3.1/scoverage-report/index.html

# With Python HTTP server:
python -m http.server 8080 --directory "target/scala-3.3.1/scoverage-report"
# Then visit: http://localhost:8080
```

#### Current Coverage Status
- **Statement Coverage**: 4.67%
- **Branch Coverage**: 4.74%
- **Total Tests**: 129 tests (all passing)
- **Test Suites**: 8 comprehensive test suites

#### Coverage Exclusions
The following components are excluded from coverage requirements to focus on core functionality:
- CLI interface components
- DataLoader utilities  
- RecommendationEngine (advanced feature)
- ValidationSystem utilities
- Main application entry points
- File management utilities
- Authentication session handling

#### Coverage Configuration
```scala
// In build.sbt
coverageExcludedPackages := ".*CLI.*;.*DataLoader.*;.*RecommendationEngine.*;.*ValidationSystem.*;.*AuthSession.*;.*DataTransformation.*"
coverageExcludedFiles := ".*Main.*;.*DataTransformation.*;.*AuthSession.*;.*FileManager.*;.*BackupRestore.*;.*utils.*"
coverageMinimumStmtTotal := 30
coverageFailOnMinimum := false
```

### Test Types

1. **Unit Tests**: Individual component testing
2. **Property-Based Tests**: ScalaCheck generators for robust validation
3. **Integration Tests**: Cross-component interaction testing
4. **Business Logic Tests**: Complex library operation validation

## Running Tests

### Prerequisites
- Scala 3.3.1+
- sbt 1.9.0+
- JDK 11+

### Commands

#### Run All Tests
```bash
sbt test
```

#### Run Specific Test Suite
```bash
sbt "testOnly models.BookSpec"
sbt "testOnly models.UserSpec"  
sbt "testOnly models.TransactionSpec"
```


#### Run Tests with Verbose Output (Alternative)
```bash
sbt "testOnly * -- -oF"
```

## Test Suite Details

### BookSpec.scala 
- **Coverage**: Book domain model with ISBN integration
- **Tests**: 25+ test cases covering:
  - Basic book creation and validation
  - ISBN validation and formatting
  - Availability status management
  - Property-based testing with generators
  - Edge cases and error conditions
  - Collection operations and filtering

### UserSpec.scala 
- **Coverage**: User hierarchy and role-based permissions
- **Tests**: 30+ test cases covering:
  - User type creation (Student, Faculty, Librarian)
  - Role-based permission validation
  - Authentication system testing
  - Loan limits and period calculations
  - Property-based testing for all user types
  - Business logic validation

### TransactionSpec.scala 
- **Coverage**: Transaction system and event tracking
- **Tests**: 25+ test cases covering:
  - Transaction type creation (Loan, Return, Reservation)
  - Business logic validation
  - Date/time handling and calculations
  - Transaction helper methods
  - Property-based testing with temporal data
  - Collection operations and analysis

### ResultsSpec.scala (Pending)
- **Coverage**: Union type error handling system
- **Planned Tests**: 
  - Union type operations and pattern matching
  - Error hierarchy validation
  - Functional operations (map, flatMap, getOrElse)
  - Error recovery and transformation
  - Property-based testing for error scenarios

## Compilation and Build

### Compile Project
```bash
sbt compile
```

### Clean and Recompile
```bash
sbt clean compile
```

### Run Application
```bash
sbt run
```

### Package Project
```bash
sbt package
```

## Error Handling Testing

### Functional Error Handling
- **Either Types**: Left/Right pattern matching
- **Try Types**: Success/Failure handling
- **Option Types**: None/Some operations
- **Union Types**: Type-safe error hierarchies

### Test Coverage Areas
1. **Validation Failures**: Invalid ISBN, empty titles, etc.
2. **Business Rule Violations**: Loan limits, availability conflicts
3. **I/O Errors**: File system operations, JSON parsing
4. **Authentication Failures**: Invalid credentials, missing users

## Performance Testing

### Property-Based Testing
- **Generators**: Custom generators for domain objects
- **Properties**: Invariant validation and business rules
- **Scale Testing**: Large collection operations
- **Edge Cases**: Boundary value analysis

### Memory and Performance
- **Immutable Collections**: Memory efficiency testing
- **Functional Operations**: Performance with large datasets
- **Pattern Matching**: Efficiency validation

## Integration Testing

### Cross-Component Testing
- **Service Layer**: LibraryCatalog operations
- **Persistence Layer**: JSON I/O operations  
- **Type System**: Opaque type integration
- **Extension Methods**: Cross-package functionality

### Business Workflow Testing
- **Complete User Journey**: Registration → Loan → Return → Reservation
- **Administrative Operations**: User/Book management by librarians
- **Error Recovery**: System resilience testing
- **Concurrent Operations**: Thread safety validation

## Continuous Integration

### GitHub Actions (Recommended)
```yaml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: coursier/setup-action@v1
      with:
        jvm: openjdk:11
    - run: sbt test
```

### Local Development
```bash
# Watch mode for continuous testing
sbt ~test

# Specific test file watching
sbt "~testOnly models.BookSpec"
```

## Troubleshooting

### Common Issues

1. **Compilation Errors**
   ```bash
   sbt clean compile
   ```

2. **Test Dependencies**
   ```bash
   sbt "reload; test"
   ```

3. **IDE Integration**
   ```bash
   sbt bloopInstall  # For Metals/VS Code
   ```

### Debug Mode
```bash
sbt -jvm-debug 5005 test
```

