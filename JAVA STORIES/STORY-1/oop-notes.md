# Java OOP Fundamentals — Notes

These notes cover the Java OOP programs created in `JAVA STORIES`.

## 1. Classes and Objects

A **class** is a blueprint. An **object** is an instance of a class.

Example from `ClassesAndObjects.java`:


class Student {
    String name;
    int age;

    void displayDetails() {
        System.out.println("Name: " + name);
        System.out.println("Age: " + age);
    }
}

public class ClassesAndObjects {
    public static void main(String[] args) {
        Student student = new Student();
        student.name = "Ravi";
        student.age = 21;
        student.displayDetails();
    }
}


Output:

Name: Ravi
Age: 21


- `Student` is the class.
- `new Student()` creates an object.
- `name` and `age` are instance fields.
- `displayDetails()` is an instance method.

Diagram:

Student class (blueprint)
  name, age, displayDetails()
             |
          creates
             v
Student object: name="Ravi", age=21


**Real-world use:** A school system can use one `Student` class to represent many students, each with their own details.

## 2. Constructors

A **constructor** initializes an object when it is created. Its name matches the class name and it has no return type.

Example from `ConstructorExample.java`:

```java
Student(String name, int age) {
    this.name = name;
    this.age = age;
    System.out.println("Constructor called");
}
```

When `new Student("Ravi", 21)` runs, the constructor assigns the values to the new object.

Key points:
- A constructor runs when an object is created with `new`.
- Constructors can accept parameters.
- Constructors have no return type, not even `void`.
- If no constructor is declared, Java normally provides a no-argument default constructor. If you declare a constructor, Java does not automatically add that default constructor.

**Real-world use:** A `Car` constructor can set the brand and model when a car object is created.

## 3. Constructor Chaining with `this()`

Constructor chaining occurs when one constructor calls another constructor in the same class using `this(...)`.

Example from `ConstructorChainingExample.java`:

```java
Employee() {
    this("Ravi");
    System.out.println("Default constructor called");
}

Employee(String name) {
    this(name, 21);
    System.out.println("One-parameter constructor called");
}

Employee(String name, int age) {
    this.name = name;
    this.age = age;
    System.out.println("Two-parameter constructor called");
}
```

Output order:
```text
Two-parameter constructor called
One-parameter constructor called
Default constructor called
```

Why? The no-argument constructor calls the one-parameter constructor, which calls the two-parameter constructor. The deepest call finishes first, then execution returns up the chain.

**Rule:** `this(...)` must be the first statement in a constructor.

Diagram:
```text
Employee()
   | this("Ravi")
   v
Employee(String name)
   | this(name, 21)
   v
Employee(String name, int age)
```

## 4. Instance and Static Members

- An **instance field** belongs to each object. Each object has its own copy.
- A **static field** belongs to the class and is shared by its objects.

Example from `InstanceAndStaticExample.java`:

```java
class Counter {
    int instanceCount = 0;
    static int totalCount = 0;

    Counter() {
        instanceCount++;
        totalCount++;
    }
}
```

After creating two `Counter` objects, each object's `instanceCount` is `1`, but `totalCount` is `2`.

| Feature | Instance member | Static member |
|---|---|---|
| Belongs to | Each object | Class |
| Copies | One per object | Shared |
| Example | `instanceCount` | `totalCount` |

Diagram:
```text
Counter class: static totalCount = 2
            shared
          /        \
         v          v
     Object 1    Object 2
     count = 1   count = 1
```

**Real-world use:** Each student has an individual name (instance field), while a school name could be shared (static field).

## 5. Access Modifiers and Encapsulation

Access modifiers control where members can be accessed.

| Modifier | General access |
|---|---|
| `public` | Accessible from other classes, subject to Java access rules |
| `private` | Only within the declaring class |
| `protected` | Same package and subclasses, subject to Java access rules |
| No modifier (package-private) | Within the same package |

**Encapsulation** means protecting an object's data and providing controlled ways to access or change it.

Example from `EncapsulationExample.java`:

```java
class BankAccount {
    private double balance;

    public void deposit(double amount) {
        if (amount > 0) {
            balance = balance + amount;
        }
    }

    public double getBalance() {
        return balance;
    }
}
```

Calling `deposit(1000)` and `deposit(500)` produces:
```text
Account balance: 1500.0
```

`balance` is private, so other classes cannot change it directly. `deposit()` checks that the amount is positive, and `getBalance()` allows the value to be read.

**Real-world use:** Banking software should control how an account balance changes rather than allowing arbitrary direct edits.

## 6. `this` and `super`

- `this` refers to the current object.
- `super` accesses a visible parent-class member or calls a parent implementation.

Example from `ThisAndSuperExample.java`:

```java
class Parent {
    String name = "Parent";

    void display() {
        System.out.println("Parent display method");
    }
}

class Child extends Parent {
    String name = "Child";

    void showDetails() {
        System.out.println("this.name: " + this.name);
        System.out.println("super.name: " + super.name);
        super.display();
    }
}
```

Output:
```text
this.name: Child
super.name: Parent
Parent display method
```

| `this` | `super` |
|---|---|
| Refers to the current object | Accesses the parent-class part of the object |
| `this.name` accesses the current object's field | `super.name` accesses the visible parent field |
| `this(...)` calls another constructor in the same class | `super(...)` calls a parent constructor |

If used, `this(...)` or `super(...)` must be the first statement in a constructor. A constructor cannot make both calls separately.

**Real-world use:** A `Manager` can extend `Employee`. It can use `super` to call the parent constructor and `this` to refer to its own fields.

## 7. Object Lifecycle and Garbage Collection

An object's lifecycle includes creation, use, and eventual memory reclamation when it is no longer reachable.

Example from `ObjectLifecycleExample.java`:

```java
Laptop laptop = new Laptop("Dell");
laptop.use();

laptop = null;
```

Lifecycle diagram:
```text
Create object with new
        |
        v
Use object through reference
        |
        v
Reference no longer points to object
        |
        v
If no reachable references remain:
object is eligible for garbage collection
        |
        v
JVM may reclaim memory later
```

Important points:
1. `new Laptop("Dell")` creates the object and calls its constructor.
2. `laptop.use()` uses the object.
3. `laptop = null` removes the reference held by this variable.
4. If no other reachable references exist, the object becomes eligible for garbage collection.
5. The JVM decides when collection happens; the exact time is not guaranteed.

Setting a reference to `null` does not directly destroy an object. Another reference may still point to it.

**Real-world use:** A long-running application creates temporary objects while processing requests. Garbage collection can reclaim memory used by unreachable objects.

## 8. Quick Revision

| Concept | Remember |
|---|---|
| Class | Blueprint for objects |
| Object | Instance of a class |
| Constructor | Initializes an object during creation |
| Constructor chaining | Calls another constructor using `this(...)` |
| Instance field | Separate value for each object |
| Static field | Shared class-level value |
| `private` | Restricts access to the declaring class |
| Encapsulation | Protects data and controls access |
| `this` | Current object |
| `super` | Accesses parent-class members or constructors |
| Garbage collection | JVM reclaims memory from eligible unreachable objects |

## 9. Running Programs in VS Code

1. Open the `JAVA STORIES` folder.
2. Open the Java file to test.
3. Click **Run** or **Run main** above its `main` method.
4. Check the Debug Console for output.
5. Compare the output with the result observed during testing.

## Completion Checklist

- [x] Classes and objects
- [x] Constructors
- [x] Constructor chaining
- [x] Instance and static members
- [x] Access modifiers and encapsulation
- [x] `this` and `super`
- [x] Object lifecycle basics
- [x] Explanations, code examples, diagrams, real-world uses, and comparisons
- [ ] Review the remaining VS Code Problems items, if needed
