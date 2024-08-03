package bugIdeas;

public class Ideas_2010_09_17 {
    private static int bufferSize = 0;

    private static int[] dataBuffer = new int[bufferSize];

    private void resize(int size) {
        synchronized (dataBuffer) {
            System.out.println("Entering synchronized block (resizing to: " + size + ")");
            bufferSize = size;
            if (dataBuffer.length < size) {
                dataBuffer = new int[size]; // danger modifying mutex!!!!
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("Sleep was interrupted");
            }
            System.out.println("Exiting synchronized block");
        }
    }

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Ideas_2010_09_17 instance1 = new Ideas_2010_09_17();
                instance1.resize(100);
            }
        }).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.out.println("Sleep was interrupted");
        }

        Ideas_2010_09_17 instance2 = new Ideas_2010_09_17();
        instance2.resize(10000);
    }

}
