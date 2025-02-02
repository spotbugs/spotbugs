package commonResources;

public class Vehicle {

    private String model;
    private int year;
    private String color;
    private int price;

    public Vehicle() {
    }

    public Vehicle(Builder builder) {
        this.model = builder.model;
        this.year = builder.year;
        this.color = builder.color;
        this.price = builder.price;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public static class Builder {
        private String model;
        private int year;
        private String color;
        private int price;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder setModel(String model) {
            this.model = model;
            return this;
        }

        public Builder setYear(int year) {
            this.year = year;
            return this;
        }

        public Builder setColor(String color) {
            this.color = color;
            return this;
        }

        public Builder setPrice(int price) {
            this.price = price;
            return this;
        }

        public Vehicle build() {
            return new Vehicle(this);
        }
    }
}
