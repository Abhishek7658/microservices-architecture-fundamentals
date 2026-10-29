
class Number {
    int value;

    Number(int value) {
        this.value = value;
    }
}

public class OperatorLimitationsExample {
    public static void main(String[] args) {
        int a = 10;
        int b = 20;

        System.out.println("Integer addition: " + (a + b));
        System.out.println("String addition: " + ("Hello " + "Java"));

        Number n1 = new Number(10);
        Number n2 = new Number(20);

        // Java does not support n1 + n2
        System.out.println("Object values: " +
                (n1.value + n2.value));
    }
}
