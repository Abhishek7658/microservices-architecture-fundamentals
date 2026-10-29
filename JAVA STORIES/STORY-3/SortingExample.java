
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class Student implements Comparable<Student> {
    String name;
    int age;

    Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public int compareTo(Student other) {
        return Integer.compare(this.age, other.age);
    }

    @Override
    public String toString() {
        return name + " - " + age;
    }
}

public class SortingExample {
    public static void main(String[] args) {
        List<Student> students = new ArrayList<>(Arrays.asList(
            new Student("Ravi", 21),
            new Student("Anu", 19),
            new Student("Kiran", 23)
        ));

        // Comparable: sort by age (natural order)
        students.sort(null);
        System.out.println("Comparable: " + students);

        // Comparator: sort by name
        students.sort(Comparator.comparing(s -> s.name));
        System.out.println("Comparator: " + students);
    }
}
