
class Animal {
    void eat() {
        System.out.println("Animal eats");
    }
}

// Single inheritance
class Dog extends Animal {
    void bark() {
        System.out.println("Dog barks");
    }
}

// Multilevel inheritance
class Puppy extends Dog {
    void weep() {
        System.out.println("Puppy weeps");
    }
}

// Hierarchical inheritance
class Cat extends Animal {
    void meow() {
        System.out.println("Cat meows");
    }
}

public class InheritanceExample {
    public static void main(String[] args) {
        Dog dog = new Dog();
        dog.eat();
        dog.bark();

        Puppy puppy = new Puppy();
        puppy.eat();
        puppy.bark();
        puppy.weep();

        Cat cat = new Cat();
        cat.eat();
        cat.meow();
    }
}
