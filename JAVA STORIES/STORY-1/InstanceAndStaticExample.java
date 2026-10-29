
class Counter {
    int instanceCount = 0;
    static int totalCount = 0;

    Counter() {
        instanceCount++;
        totalCount++;
    }

    void displayCounts() {
        System.out.println("Instance count: " + instanceCount);
        System.out.println("Total count: " + totalCount);
    }
}

public class InstanceAndStaticExample {
    public static void main(String[] args) {
        Counter first = new Counter();
        Counter second = new Counter();

        System.out.println("First object:");
        first.displayCounts();

        System.out.println("Second object:");
        second.displayCounts();
    }
}
