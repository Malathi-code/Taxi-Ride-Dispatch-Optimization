
public class Driver {
    int id;
    String name;
    String location;
    double x;
    double y;
    boolean available;

    public Driver(int id, String name, String location,
                  double x, double y) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.x = x;
        this.y = y;
        this.available = true;
    }

    public void display() {
        System.out.println(
            id + " | " + name + " | " + location +
            " | Available: " + available
        );
    }
}
