
class Employee {
    String name;
    int age;

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

    void displayDetails() {
        System.out.println("Name: " + name);
        System.out.println("Age: " + age);
    }
}

public class ConstructorChainingExample {
    public static void main(String[] args) {
        Employee employee = new Employee();
        employee.displayDetails();
    }
}
