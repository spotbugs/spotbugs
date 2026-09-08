package ghIssues;

enum A {
  ENUM_VALUE_3;
  class B {
    int s = 0;       // <- reported UrF (TP)
  }
  boolean i = false; // <- should report UrF (FN)
}
