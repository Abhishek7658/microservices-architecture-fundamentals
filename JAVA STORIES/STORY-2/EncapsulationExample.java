
class Student {
    private int age = 20;

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}

public class EncapsulationExample {
    public static void main(String[] args) {
        Student s = new Student();

        System.out.println("Age: " + s.getAge());

        s.setAge(21);

        System.out.println("Updated age: " + s.getAge());
    }
}
