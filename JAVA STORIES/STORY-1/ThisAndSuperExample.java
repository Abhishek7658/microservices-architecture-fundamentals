
class Parent {
    String name = "Parent";

    void display() {
        System.out.println("Parent display method");
    }
}

class Child extends Parent {
    String name = "Child";

    void showDetails() {
        // this refers to the current Child object
        System.out.println("this.name: " + this.name);

        // super accesses the Parent class member
        System.out.println("super.name: " + super.name);

        // Call the Parent class method
        super.display();
    }
}

public class ThisAndSuperExample {
    public static void main(String[] args) {
        Child child = new Child();
        child.showDetails();
    }
}
