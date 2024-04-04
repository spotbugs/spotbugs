package commonResources;

public class CombinedThreadsInShutdownHook {

    public static CombinedThreadsInShutdownHook createStaticThreadTest() {
        return new CombinedThreadsInShutdownHook() {
            private Thread t;

            {
                t = new Thread(() -> {
                    while(isShutdown()) {
                        try {
                            Thread.sleep(1000);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
                t.setDaemon(true);
                t.start();

                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            }

            @Override
            public void shutdown() {
                if (!isShutdown()) {
                    super.shutdown();
                    t.interrupt();
                }
            }

            public boolean isShutdown() {
                return false;
            }
        };
    }

    private volatile boolean shutdown;

    public boolean isShutdown() {
        return shutdown;
    }

    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
    }
}
