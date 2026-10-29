
import java.util.ArrayList;
import java.util.List;

class Student {
    private List<String> subjects;

    Student(List<String> subjects) {
        this.subjects = new ArrayList<>(subjects);
    }

    public List<String> getSubjects() {
        return new ArrayList<>(subjects);
    }
}

public class DefensiveCopyExample {
    public static void main(String[] args) {
        List<String> subjects = new ArrayList<>();
        subjects.add("Java");
        subjects.add("SQL");

        Student student = new Student(subjects);

        subjects.add("Python");

        System.out.println("Original list: " + subjects);
        System.out.println("Student list: " + student.getSubjects());
    }
}
