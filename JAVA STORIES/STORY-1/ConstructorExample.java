
class Student {
    String name;
    int age;

    // Constructor
    Student(String name, int age) {
        this.name = name;
        this.age = age;
        System.out.println("Constructor called");
    }

    void displayDetails() {
        System.out.println("Name: " + name);
        System.out.println("Age: " + age);
    }
}

public class ConstructorExample {
    public static void main(String[] args) {
        Student student = new Student("Ravi", 21);
        student.displayDetails();
    }
}
