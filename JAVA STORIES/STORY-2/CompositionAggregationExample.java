
class Engine {
    void start() {
        System.out.println("Engine started");
    }
}

// Composition: Car creates and owns its Engine
class Car {
    private Engine engine = new Engine();

    void startCar() {
        engine.start();
        System.out.println("Car started");
    }
}

// Aggregation: Driver receives an existing Car
class Driver {
    private Car car;

    Driver(Car car) {
        this.car = car;
    }

    void drive() {
        car.startCar();
        System.out.println("Driver is driving");
    }
}

public class CompositionAggregationExample {
    public static void main(String[] args) {
        Car car = new Car();
        car.startCar();

        Driver driver = new Driver(car);
        driver.drive();
    }
}
