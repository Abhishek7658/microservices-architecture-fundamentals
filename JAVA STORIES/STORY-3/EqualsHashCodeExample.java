
import java.util.Objects;

class Student {
    String name;
    int age;

    Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Student)) {
            return false;
        }

        Student other = (Student) obj;
        return age == other.age && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
}

public class EqualsHashCodeExample {
    public static void main(String[] args) {
        Student s1 = new Student("Ravi", 21);
        Student s2 = new Student("Ravi", 21);

        System.out.println("Equal: " + s1.equals(s2));
        System.out.println("Same hash code: " +
                (s1.hashCode() == s2.hashCode()));
    }
}
