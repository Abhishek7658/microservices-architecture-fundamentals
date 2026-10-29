
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
