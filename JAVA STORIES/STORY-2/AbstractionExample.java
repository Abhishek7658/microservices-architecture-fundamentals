
abstract class Animal {
    abstract void sound();
}

interface Pet {
    void play();
}

class Dog extends Animal implements Pet {
    @Override
    void sound() {
        System.out.println("Dog barks");
    }

    @Override
    public void play() {
        System.out.println("Dog plays");
    }
}

public class AbstractionExample {
    public static void main(String[] args) {
        Dog dog = new Dog();

        dog.sound();
        dog.play();
    }
}
