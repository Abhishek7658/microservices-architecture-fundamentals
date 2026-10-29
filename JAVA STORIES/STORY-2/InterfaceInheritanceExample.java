
interface Father {
    void work();
}

interface Mother {
    void cook();
}

class Child implements Father, Mother {
    public void work() {
        System.out.println("Child works");
    }

    public void cook() {
        System.out.println("Child cooks");
    }
}

public class InterfaceInheritanceExample {
    public static void main(String[] args) {
        Child child = new Child();

        child.work();
        child.cook();
    }
}
