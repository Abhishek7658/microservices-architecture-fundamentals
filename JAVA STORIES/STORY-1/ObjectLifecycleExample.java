
class Laptop {
    String brand;

    Laptop(String brand) {
        this.brand = brand;
        System.out.println("Object created: " + brand);
    }

    void use() {
        System.out.println("Using " + brand + " laptop");
    }
}

public class ObjectLifecycleExample {
    public static void main(String[] args) {
        Laptop laptop = new Laptop("Dell");

        laptop.use();

        laptop = null;

        System.out.println("Object reference set to null");
        System.out.println("The object may now be eligible for garbage collection.");
    }
}
