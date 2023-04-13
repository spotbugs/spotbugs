package ghIssues.issue543;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.immutables.value.Generated;

/**
 * Immutable implementation of {@link FoobarValue}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableFoobarValue.builder()}.
 */
@Generated(from = "FoobarValue", generator = "Immutables")
@SuppressWarnings({"all"})
//@javax.annotation.processing.Generated("org.immutables.processor.ProxyProcessor") // can be ignored
public final class ImmutableFoobarValue extends FoobarValue {
  private final int foo;
  private final String bar;
  private final List<Integer> buz;
  private final Set<Long> crux;

  private ImmutableFoobarValue(
      int foo,
      String bar,
      List<Integer> buz,
      Set<Long> crux) {
    this.foo = foo;
    this.bar = bar;
    this.buz = buz;
    this.crux = crux;
  }

  /**
   * @return The value of the {@code foo} attribute
   */
  @Override
  public int foo() {
    return foo;
  }

  /**
   * @return The value of the {@code bar} attribute
   */
  @Override
  public String bar() {
    return bar;
  }

  /**
   * @return The value of the {@code buz} attribute
   */
  @Override
  public List<Integer> buz() {
    return buz;
  }

  /**
   * @return The value of the {@code crux} attribute
   */
  @Override
  public Set<Long> crux() {
    return crux;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FoobarValue#foo() foo} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for foo
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFoobarValue withFoo(int value) {
    if (this.foo == value) return this;
    return new ImmutableFoobarValue(value, this.bar, this.buz, this.crux);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FoobarValue#bar() bar} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for bar
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFoobarValue withBar(String value) {
    String newValue = Objects.requireNonNull(value, "bar");
    if (this.bar.equals(newValue)) return this;
    return new ImmutableFoobarValue(this.foo, newValue, this.buz, this.crux);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link FoobarValue#buz() buz}.
   * @param elements The elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableFoobarValue withBuz(int... elements) {
    ArrayList<Integer> wrappedList = new ArrayList<>(elements.length);
    for (int element : elements) {
      wrappedList.add(element);
    }
    List<Integer> newValue = createUnmodifiableList(false, wrappedList);
    return new ImmutableFoobarValue(this.foo, this.bar, newValue, this.crux);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link FoobarValue#buz() buz}.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param elements An iterable of buz elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableFoobarValue withBuz(Iterable<Integer> elements) {
    if (this.buz == elements) return this;
    List<Integer> newValue = createUnmodifiableList(false, createSafeList(elements, true, false));
    return new ImmutableFoobarValue(this.foo, this.bar, newValue, this.crux);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link FoobarValue#crux() crux}.
   * @param elements The elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableFoobarValue withCrux(long... elements) {
    ArrayList<Long> wrappedList = new ArrayList<>(elements.length);
    for (long element : elements) {
      wrappedList.add(element);
    }
    Set<Long> newValue = createUnmodifiableSet(wrappedList);
    return new ImmutableFoobarValue(this.foo, this.bar, this.buz, newValue);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link FoobarValue#crux() crux}.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param elements An iterable of crux elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableFoobarValue withCrux(Iterable<Long> elements) {
    if (this.crux == elements) return this;
    Set<Long> newValue = createUnmodifiableSet(createSafeList(elements, true, false));
    return new ImmutableFoobarValue(this.foo, this.bar, this.buz, newValue);
  }

  /**
   * This instance is equal to all instances of {@code ImmutableFoobarValue} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(Object another) {
    if (this == another) return true;
    return another instanceof ImmutableFoobarValue
        && equalTo(0, (ImmutableFoobarValue) another);
  }

  private boolean equalTo(int synthetic, ImmutableFoobarValue another) {
    return foo == another.foo
        && bar.equals(another.bar)
        && buz.equals(another.buz)
        && crux.equals(another.crux);
  }

  /**
   * Computes a hash code from attributes: {@code foo}, {@code bar}, {@code buz}, {@code crux}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 5381;
    h += (h << 5) + foo;
    h += (h << 5) + bar.hashCode();
    h += (h << 5) + buz.hashCode();
    h += (h << 5) + crux.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code FoobarValue} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return "FoobarValue{"
        + "foo=" + foo
        + ", bar=" + bar
        + ", buz=" + buz
        + ", crux=" + crux
        + "}";
  }

  /**
   * Creates an immutable copy of a {@link FoobarValue} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable FoobarValue instance
   */
  public static ImmutableFoobarValue copyOf(FoobarValue instance) {
    if (instance instanceof ImmutableFoobarValue) {
      return (ImmutableFoobarValue) instance;
    }
    return ImmutableFoobarValue.builder()
        .from(instance)
        .build();
  }

  /**
   * Creates a builder for {@link ImmutableFoobarValue ImmutableFoobarValue}.
   * <pre>
   * ImmutableFoobarValue.builder()
   *    .foo(int) // required {@link FoobarValue#foo() foo}
   *    .bar(String) // required {@link FoobarValue#bar() bar}
   *    .addBuz|addAllBuz(int) // {@link FoobarValue#buz() buz} elements
   *    .addCrux|addAllCrux(long) // {@link FoobarValue#crux() crux} elements
   *    .build();
   * </pre>
   * @return A new ImmutableFoobarValue builder
   */
  public static ImmutableFoobarValue.Builder builder() {
    return new ImmutableFoobarValue.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableFoobarValue ImmutableFoobarValue}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @Generated(from = "FoobarValue", generator = "Immutables")
  public static final class Builder {
    private static final long INIT_BIT_FOO = 0x1L;
    private static final long INIT_BIT_BAR = 0x2L;
    private long initBits = 0x3L;

    private int foo;
    private String bar;
    private List<Integer> buz = new ArrayList<Integer>();
    private List<Long> crux = new ArrayList<Long>();

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code FoobarValue} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * Collection elements and entries will be added, not replaced.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(FoobarValue instance) {
      Objects.requireNonNull(instance, "instance");
      foo(instance.foo());
      bar(instance.bar());
      addAllBuz(instance.buz());
      addAllCrux(instance.crux());
      return this;
    }

    /**
     * Initializes the value for the {@link FoobarValue#foo() foo} attribute.
     * @param foo The value for foo 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder foo(int foo) {
      this.foo = foo;
      initBits &= ~INIT_BIT_FOO;
      return this;
    }

    /**
     * Initializes the value for the {@link FoobarValue#bar() bar} attribute.
     * @param bar The value for bar 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder bar(String bar) {
      this.bar = Objects.requireNonNull(bar, "bar");
      initBits &= ~INIT_BIT_BAR;
      return this;
    }

    /**
     * Adds one element to {@link FoobarValue#buz() buz} list.
     * @param element A buz element
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addBuz(int element) {
      this.buz.add(element);
      return this;
    }

    /**
     * Adds elements to {@link FoobarValue#buz() buz} list.
     * @param elements An array of buz elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addBuz(int... elements) {
      for (int element : elements) {
        this.buz.add(element);
      }
      return this;
    }


    /**
     * Sets or replaces all elements for {@link FoobarValue#buz() buz} list.
     * @param elements An iterable of buz elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder buz(Iterable<Integer> elements) {
      this.buz.clear();
      return addAllBuz(elements);
    }

    /**
     * Adds elements to {@link FoobarValue#buz() buz} list.
     * @param elements An iterable of buz elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAllBuz(Iterable<Integer> elements) {
      for (Integer element : elements) {
        this.buz.add(Objects.requireNonNull(element, "buz element"));
      }
      return this;
    }

    /**
     * Adds one element to {@link FoobarValue#crux() crux} set.
     * @param element A crux element
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addCrux(long element) {
      this.crux.add(element);
      return this;
    }

    /**
     * Adds elements to {@link FoobarValue#crux() crux} set.
     * @param elements An array of crux elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addCrux(long... elements) {
      for (long element : elements) {
        this.crux.add(element);
      }
      return this;
    }


    /**
     * Sets or replaces all elements for {@link FoobarValue#crux() crux} set.
     * @param elements An iterable of crux elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder crux(Iterable<Long> elements) {
      this.crux.clear();
      return addAllCrux(elements);
    }

    /**
     * Adds elements to {@link FoobarValue#crux() crux} set.
     * @param elements An iterable of crux elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAllCrux(Iterable<Long> elements) {
      for (Long element : elements) {
        this.crux.add(Objects.requireNonNull(element, "crux element"));
      }
      return this;
    }

    /**
     * Builds a new {@link ImmutableFoobarValue ImmutableFoobarValue}.
     * @return An immutable instance of FoobarValue
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableFoobarValue build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutableFoobarValue(foo, bar, createUnmodifiableList(true, buz), createUnmodifiableSet(crux));
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<>();
      if ((initBits & INIT_BIT_FOO) != 0) attributes.add("foo");
      if ((initBits & INIT_BIT_BAR) != 0) attributes.add("bar");
      return "Cannot build FoobarValue, some of required attributes are not set " + attributes;
    }
  }

  private static <T> List<T> createSafeList(Iterable<? extends T> iterable, boolean checkNulls, boolean skipNulls) {
    ArrayList<T> list;
    if (iterable instanceof Collection<?>) {
      int size = ((Collection<?>) iterable).size();
      if (size == 0) return Collections.emptyList();
      list = new ArrayList<>();
    } else {
      list = new ArrayList<>();
    }
    for (T element : iterable) {
      if (skipNulls && element == null) continue;
      if (checkNulls) Objects.requireNonNull(element, "element");
      list.add(element);
    }
    return list;
  }

  private static <T> List<T> createUnmodifiableList(boolean clone, List<T> list) {
    switch(list.size()) {
    case 0: return Collections.emptyList();
    case 1: return Collections.singletonList(list.get(0));
    default:
      if (clone) {
        return Collections.unmodifiableList(new ArrayList<>(list));
      } else {
        if (list instanceof ArrayList<?>) {
          ((ArrayList<?>) list).trimToSize();
        }
        return Collections.unmodifiableList(list);
      }
    }
  }

  /** Unmodifiable set constructed from list to avoid rehashing. */
  private static <T> Set<T> createUnmodifiableSet(List<T> list) {
    switch(list.size()) {
    case 0: return Collections.emptySet();
    case 1: return Collections.singleton(list.get(0));
    default:
      Set<T> set = new LinkedHashSet<>(list.size());
      set.addAll(list);
      return Collections.unmodifiableSet(set);
    }
  }
}
