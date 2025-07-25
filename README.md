# Scala 3 Functional Library Management System

This project is part of the Functional Programming in Scala course at EFREI (2024-2025).
It implements a complete, modular library management system using purely functional programming techniques in Scala 3, demonstrating advanced language features and best practices.

## Advanced Features

### Scala 3 Language Features 
- **Union Types**: Type-safe error handling with `SearchResult[T] = Success[T] | LibraryError`
- **Opaque Types**: Type-safe domain modeling (ISBN, UserID, BookTitle)
- **Extension Methods**: Rich APIs for collections and domain objects
- **Given/Using**: Type class pattern for polymorphic behavior
- **Enums**: Rich domain hierarchies (User types, Transaction types)

### Core Functionality
- **Immutable Data Models**: Books, Users, Transactions with functional operations
- **Advanced Search**: Multi-criteria search with similarity algorithms
- **Reservation System**: Complex booking system with availability tracking
- **Authentication**: Role-based access control (Student, Faculty, Librarian)
- **Fee Management**: Overdue fee calculation and waiving system
- **JSON Persistence**: Complete serialization with error handling
- **Comprehensive Testing**: Property-based and integration tests

## Documentation Overview

The `docs/` folder contains all relevant technical, user, and testing documentation for this project:

| File | Description |
|------|-------------|
| [design.md](docs/design.md) | **System design document** detailing architecture, patterns, error handling, and scalability decisions of the Library Management System. |
| [scaladoc-guide.md](docs/scaladoc-guide.md) | **Guide to ScalaDoc**: How to generate, serve, and navigate the API documentation, with tips for both beginners and advanced users. |
| [testing-requirements-compliance.md](docs/testing-requirements-compliance.md) | **Compliance report** verifying that all testing and documentation criteria (section 2.1.5) are fully met. |
| [testing-user-manual.md](docs/testing-user-manual.md) | **Comprehensive testing manual** explaining how to run tests, understand coverage metrics, and validate error handling and performance. |
| [user-manual.md](docs/user-manual.md) | **User guide** describing how to run the CLI, login, and use all system functionalities across user types (Student, Faculty, Librarian). |

## Authors

This project was developed by:

- Marco Serhal  
- Paul Zamanian  
- Adam Mounir  
- Alice Guillou
- Rayane Fofana

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
│   │   ├── Book.scala              # Book entity with ISBN integration
│   │   ├── User.scala              # User hierarchy (Student/Faculty/Librarian)
│   │   ├── Transaction.scala       # Transaction types (Loan/Return/Reservation)
│   │   └── Results.scala           # Union type error handling
│   ├── services/         # Business Logic
│   │   └── LibraryCatalog.scala    # Main library management service
│   ├── utils/           # Infrastructure
│   │   ├── Types.scala             # Opaque type definitions
│   │   ├── Extensions.scala        # Extension methods
│   │   ├── TypeClasses.scala       # Type class instances
│   │   └── JsonIO.scala            # JSON persistence layer
│   └── Main.scala       # Application entry point
└── test/scala/          # Comprehensive Test Suite
    ├── library/         # Library system tests
    ├── models/          # Domain model tests
    │   ├── BookSpec.scala          # Book model tests
    │   ├── UserSpec.scala          # User hierarchy tests
    │   ├── TransactionSpec.scala   # Transaction system tests
    │   └── ResultsSpec.scala       # Error handling tests
    ├── services/        # Service layer tests
    └── utils/           # Utility tests
data/                    # Sample Data
├── books.json
├── users.json
└── transactions.json
docs/                    # Documentation
├── design.md           # Architecture documentation
└── user-manual.md      # Usage examples and tutorials
```

## Getting Started

### Prerequisites
- **Scala**: 3.3.1 or higher
- **JDK**: 11 or higher
- **sbt**: 1.9.0 or higher

### Quick Start
```bash
# Clone the repository
git clone https://github.com/MarcoSrhl/library-management-system-scala3.git
cd library-management-system-scala3

# Compile the project
sbt compile

# Run the application
sbt run

# Run tests
sbt test

# Run tests with coverage
sbt coverage test coverageReport
```

### Development Commands
```bash
# Interactive mode
sbt

# Watch mode for continuous compilation
sbt ~compile

# Run specific test suite
sbt "testOnly models.BookSpec"

# Format code
sbt scalafmt

# Check for compilation warnings
sbt "compile:scalafix --check"
```

## Testing

### Test Coverage
- **Unit Tests**: Individual component testing
- **Property-Based Tests**: ScalaCheck generators for robust validation
- **Integration Tests**: Cross-component interaction testing
- **Performance Tests**: Benchmarks for search operations

### Running Tests
```bash
# All tests
sbt test

# Specific test file
sbt "testOnly models.UserSpec"

# Tests with detailed output
sbt "test -- -oD"

# Coverage report
sbt coverage test coverageReport
```

## Dependencies

```scala
libraryDependencies ++= Seq(
  // Testing
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
  "org.scalatestplus" %% "scalacheck-1-17" % "3.2.17.0" % Test,
  // Functional Programming
  "org.typelevel" %% "cats-core" % "2.10.0",
  // JSON
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "io.circe" %% "circe-parser" % "0.14.6",
  "com.lihaoyi" %% "upickle" % "3.1.0"
)
```

## Documentation

### What is ScalaDoc?

**ScalaDoc** is Scala's official documentation system, similar to Javadoc for Java. It allows you to:

- **Automatically generate** HTML documentation from source code
- **Document** classes, methods, parameters and types with special comments
- **Create** interactive navigation between different elements
- **Include** usage examples directly in the documentation

#### How does it work?

ScalaDoc uses **special comments** that start with `/**`:

```scala
/**
 * Description of the class or method
 * @param name Parameter description
 * @return Description of return value
 * @example
 * {{{
 * val result = myMethod("example")
 * }}}
 */
def myMethod(name: String): String = s"Hello $name"
```

### ScalaDoc API Documentation

The project includes complete ScalaDoc documentation for all public APIs with detailed examples and usage patterns.

#### Generating ScalaDoc

```bash
# Generate HTML ScalaDoc documentation
sbt doc

# View the generated documentation
# Open target/scala-3.3.1/api/index.html in your browser

# On Windows
start target/scala-3.3.1/api/index.html

# On macOS
open target/scala-3.3.1/api/index.html

# On Linux
xdg-open target/scala-3.3.1/api/index.html
```

#### Solutions to Documentation Problems

If the generated ScalaDoc documentation doesn't work properly (JavaScript issues, navigation, etc.), here are several solutions:

**Solution 1: Local Server (Recommended)**
```bash
# Start a local HTTP server directly from project root
python -m http.server 8080 --directory "target/scala-3.3.1/api"

# Alternative: Navigate to the directory first
cd target/scala-3.3.1/api
python -m http.server 8080

# Open in browser
# Go to http://localhost:8080
```

**Solution 2: Simplified Documentation**
```bash
# Open the alternative HTML documentation
start docs/documentation-simple.html  # Windows
open docs/documentation-simple.html   # macOS
```

**Solution 3: Clean Regeneration**
```bash
# Clean and regenerate
sbt clean
sbt compile
sbt doc
```

#### Documentation Features
- **Complete API Coverage**: All public classes, objects and methods documented
- **Practical Examples**: Usage examples for each main component
- **Type Information**: Detailed documentation of parameters and return types
- **Cross References**: Links between related classes and methods
- **Search Function**: Integrated search throughout the documentation
- **Interactive Navigation**: Modern interface with package navigation

#### Key Documentation Sections
- **Core Models**: `Book`, `User`, `Transaction` with domain-specific examples
- **Library Management**: `LibraryCatalog` with complete usage scenarios
- **Search Operations**: `SearchOperations` with predicate composition examples
- **Type Safety**: Opaque types (`ISBN`, `UserID`) and their safe usage
- **Error Handling**: Union types and functional error handling patterns

#### Accessing Documentation

1. **Local Generation**:
   ```bash
   sbt doc
   open target/scala-3.3.1/api/index.html  # macOS
   # or
   start target/scala-3.3.1/api/index.html  # Windows
   # or  
   xdg-open target/scala-3.3.1/api/index.html  # Linux
   ```

2. **IDE Integration**:
   - **IntelliJ IDEA**: Ctrl+Q (Quick Documentation)
   - **VS Code with Metals**: Hover over symbols
   - **Vim with CoC**: `:CocCommand metals.hover`

3. **Documentation Structure**:
   ```
   target/scala-3.3.1/api/
   ├── index.html              # Main entry point
   ├── library/                # Main package
   │   ├── LibraryCatalog.html  # Catalog documentation
   │   ├── SearchOperations.html # Search operations guide
   │   ├── Book.html           # Book model documentation
   │   ├── User.html           # User hierarchy documentation
   │   └── ...
   └── utils/                  # Utility packages
       ├── Types.html          # Opaque types documentation
       ├── Extensions.html     # Extension methods guide
       └── ...
   ```

#### Documentation Best Practices Used

- **Complete Examples**: Each public method includes practical usage examples
- **Parameter Documentation**: All parameters explained with types and constraints
- **Return Value Clarification**: Clear explanation of return types and possible values
- **Exception Documentation**: Error conditions and exceptions clearly documented
- **Cross References**: Related methods and classes properly referenced
- **Version Information**: `@since` tags for version tracking
- **Author Information**: `@author` tags for maintainer contact

#### ScalaDoc Documentation Examples

Here are examples of the generated documentation for different components:

**1. Main Class (LibraryCatalog)**:
```scala
/**
 * Main library management catalog.
 * 
 * This class represents the complete state of a library with functional
 * operations to manage books, users and transactions.
 * 
 * @example
 * {{{
 * val catalog = LibraryCatalog.empty
 *   .addBook(Book(...))
 *   .addUser(User.Student(...))
 *   .loanBook(isbn, userId)
 * }}}
 */
```

**2. Methods with Validation**:
```scala
/**
 * Processes a book loan transaction.
 * 
 * @param isbn The ISBN of the book to borrow
 * @param userId The ID of the user requesting the loan
 * @return A new LibraryCatalog with the loan processed
 * @throws LibraryError.BookNotFound If the book doesn't exist
 * @throws LibraryError.UserLimitExceeded If user exceeds their limit
 */
```

**3. Opaque Types**:
```scala
/**
 * Opaque type for ISBNs ensuring validity.
 * 
 * @example
 * {{{
 * val isbn = ISBN.safe("978-0134685991") // Automatic validation
 * isbn.value // Access to underlying String value
 * }}}
 */
```

#### How to Use Generated Documentation

Once documentation is generated with `sbt doc`, you get a complete website you can browse:

1. **Homepage** (`index.html`):
   - Overview of all packages
   - Global search bar
   - Navigation by categories

2. **Package Pages**:
   - `library/`: Main components (LibraryCatalog, Book, User, etc.)
   - `utils/`: Utilities (Types, Extensions, TypeClasses)
   - `services/`: Business services

3. **Class Pages**:
   - Complete documentation for each class
   - List of methods with their signatures
   - Integrated usage examples
   - Links to related classes

4. **Interactive Search**:
   - Type any class or method name
   - Real-time filtering
   - Direct navigation to documentation

#### IDE Integration

**IntelliJ IDEA**:
- Place cursor on any class/method
- Press `Ctrl+Q` (Windows/Linux) or `Cmd+J` (macOS)
- ScalaDoc documentation displays directly

**VS Code with Metals**:
- Hover over any symbol with mouse
- Documentation appears in a popup
- Use `Ctrl+K Ctrl+I` to force display

#### Usage Tips

1. **Start with `LibraryCatalog`**: Main API entry point
2. **Explore examples**: Each method has concrete examples
3. **Follow links**: Navigation between related classes
4. **Use search**: Quickly find what you're looking for
5. **Check opaque types**: Understand how to use `ISBN`, `UserID`, etc.

### Additional Documentation

- **`docs/design.md`**: Detailed architecture and design decisions
- **`docs/user-manual.md`**: Complete usage guide with tutorials
- **`docs/scaladoc-guide.md`**: Complete guide for navigating ScalaDoc documentation
- **`docs/documentation-simple.html`**: Alternative documentation if ScalaDoc doesn't work
- **Tests**: Test suite also serves as documentation by example
- **Online ScalaDoc**: Complete API documentation generated automatically

> **Tip**: Check `docs/scaladoc-guide.md` to learn how to navigate the generated documentation effectively!

> **ScalaDoc Problems?** Open `docs/documentation-simple.html` for an alternative version or use the local server with `python -m http.server 8080` in `target/scala-3.3.1/api/`

## Architecture

### Design Principles
- **Functional Programming**: Immutable data structures and pure functions
- **Type Safety**: Leveraging Scala 3's type system for compile-time guarantees
- **Separation of Concerns**: Clear boundaries between models, services, and utilities
- **Error Handling**: Functional error handling with Either, Try, and Union types
- **Testability**: Comprehensive test coverage with property-based testing

### Key Patterns
- **Domain-Driven Design**: Rich domain models with business logic
- **Type Classes**: Polymorphic behavior with given/using
- **Extension Methods**: Enhanced APIs without inheritance
- **Union Types**: Type-safe error handling
- **Opaque Types**: Zero-cost abstractions for domain safety

## Academic Requirements Compliance

### Advanced Scala 3 Features (125 points) - Complete
- Union Types: Complete implementation
- Opaque Types: Type-safe domain modeling
- Extension Methods: Rich API enhancements
- Given/Using: Type class polymorphism
- Enums: Rich domain hierarchies

### Error Handling (35 points) - Complete
- Functional error handling patterns
- Comprehensive error recovery
- Type-safe error hierarchies

### Code Quality (40 points) - Complete
- Clean architecture
- Comprehensive test coverage
- Following Scala conventions

## Instructor

Said Boudjelda – EFREI 2024–2025  
Contact: mohamed-said.boudjelda@intervenants.efrei.net
