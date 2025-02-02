class RunFinalizersOnExit {

    int f() {
        System.runFinalizersOnExit(true);
        return 42;
    }

}
