package no.petroware.logio.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A List implementation wrapper for a native double array.
 * <p>
 * Useful if the array becomes <em>very</em> large as this is both
 * a lot faster and requires less storage than a List&lt;Double&gt;.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class DoubleList implements List<Double>
{
  /** Back-end array. */
  private double[] array_;

  /** Current size. [0,&gt;. */
  private int size_ = 0;

  /**
   * Create a new Double list with the specified capacity.
   *
   * @param capacity  Initial capacity. [1,&gt;.
   * @throws IllegalArgumentException  If capacity is &lt; 1.
   */
  public DoubleList(int capacity)
  {
    if (capacity < 1)
      throw new IllegalArgumentException("Invalid capacity: " + capacity);

    array_ = new double[capacity];
  }

  /**
   * Create a new Double list with default capacity.
   */
  public DoubleList()
 {
    // A large initial capacity to indicate the fact that the
    // class should mainly be with very large collections.
    this(1000);
  }

  /** {@inheritDoc} */
  @Override
  public boolean add(Double value)
  {
    ensureCapacity(size_ + 1);
    array_[size_++] = value != null ? value : Double.NaN;
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public void add(int index, Double value)
  {
    if (index < 0 || index > size_)
      throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size_);

    ensureCapacity(size_ + 1);
    System.arraycopy(array_, index, array_, index + 1, size_ - index);

    array_[index] = value != null ? value : Double.NaN;

    size_++;
  }

  /** {@inheritDoc} */
  @Override
  public boolean addAll(Collection<? extends Double> values)
  {
    for (Double value : values)
      add(value);
    return values.size() > 0;
  }

  /** {@inheritDoc} */
  @Override
  public boolean addAll(int index, Collection<? extends Double> values)
  {
    int d = 0;
    for (Double value : values) {
      add(index + d, value);
      d++;
    }
    return values.size() > 0;
  }

  /** {@inheritDoc} */
  @Override
  public void clear()
  {
    size_ = 0;
  }

  /** {@inheritDoc} */
  @Override
  public boolean contains(Object value)
  {
    return indexOf(value) != -1;
  }

  /** {@inheritDoc} */
  @Override
  public int indexOf(Object value)
  {
    if (!(value instanceof Double))
      return -1;

    double v = value != null ? (Double) value : Double.NaN;

    for (int index = 0; index < size_; index++)
      if (v == array_[index])
        return index;

    return -1;
  }

  /** {@inheritDoc} */
  @Override
  public int lastIndexOf(Object value)
  {
    if (!(value instanceof Double))
      return -1;

    double v = value != null ? (Double) value : Double.NaN;

    for (int index = size_ - 1; index >= 0; index--)
      if (v == array_[index])
        return index;

    return -1;
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsAll(Collection<?> collection)
  {
    for (Object value : collection)
      if (!contains(value))
        return false;

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    int hashCode = 1;
    for (Double value : this)
      hashCode = 31 * hashCode + (value == null ? 0 : value.hashCode());

    return hashCode;
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object object)
  {
    if (object == this)
      return true;

    if (!(object instanceof List))
      return false;

    ListIterator<Double> e1 = listIterator();
    ListIterator<?> e2 = ((List) object).listIterator();

    while (e1.hasNext() && e2.hasNext()) {
      Double o1 = e1.next();
      Object o2 = e2.next();

      if (!(o1 == null ? o2 == null : o1.equals(o2)))
        return false;
    }

    return !(e1.hasNext() || e2.hasNext());
  }

  /** {@inheritDoc} */
  @Override
  public Double get(int index)
  {
    if (index < 0 || index >= size_)
      throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size_);

    double v = array_[index];

    return Double.isNaN(v) ? null : v;
  }

  /** {@inheritDoc} */
  @Override
  public Double set(int index, Double value)
  {
    if (index < 0 || index >= size_)
      throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size_);

    double v = value != null ? value : Double.NaN;

    double oldValue = array_[index];
    array_[index] = v;

    return !Double.isNaN(oldValue) ? oldValue : null;
  }

  /** {@inheritDoc} */
  @Override
  public int size()
  {
    return size_;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEmpty()
  {
    return size_ == 0;
  }

  /** {@inheritDoc} */
  @Override
  public List<Double> subList(int fromIndex, int toINdex)
  {
    throw new UnsupportedOperationException("Not supported. Use java.util.ArrayList instead");
  }

  /** {@inheritDoc} */
  @Override
  public ListIterator<Double> listIterator()
  {
    return new ListItr(0);
  }

  /** {@inheritDoc} */
  @Override
  public ListIterator<Double> listIterator(int index)
  {
    if (index < 0 || index > size_)
      throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size_);

    return new ListItr(index);
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Double> iterator()
  {
    return listIterator();
  }

  /** {@inheritDoc} */
  @Override
  public Double remove(int index)
  {
    // modificationCount++;
    double oldValue = array_[index];

    int nMoved = size_ - index - 1;
    if (nMoved > 0)
      System.arraycopy(array_, index + 1, array_, index, nMoved);

    size_--;

    return !Double.isNaN(oldValue) ? oldValue : null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean remove(Object object)
  {
    if (!(object instanceof Double))
      return false;

    Double value = (Double) object;
    double v = value != null ? value : Double.NaN;

    for (int i = 0; i < size_; i++) {
      if (array_[i] == v) {
        remove(i);
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean removeAll(Collection<?> collection)
  {
    throw new UnsupportedOperationException("Not supported. Use java.util.ArrayList instead");
  }

  /** {@inheritDoc} */
  @Override
  public boolean retainAll(Collection<?> collection)
  {
    throw new UnsupportedOperationException("Not supported. Use java.util.ArrayList instead");
  }

  /** {@inheritDoc} */
  @Override
  public Object[] toArray()
  {
    Object[] array = new Object[size_];
    for (int i = 0; i < size_; i++) {
      double value = array_[i];
      array[i] = !Double.isNaN(value) ? value : null;
    }

    return array;
  }

  /** {@inheritDoc} */
  @Override
  public <Double> Double[] toArray(Double[] array)
  {
    throw new UnsupportedOperationException("Use toArray() instead");
  }

  /**
   * Ensure that the backing list has enough capacity for the specified
   * number of entries.
   *
   * @param size  Size of elements. [0,&gt;.
   */
  private void ensureCapacity(int size)
  {
    int oldCapacity = array_.length;
    if (size > oldCapacity) {
      double[] oldArray = array_;
      int newCapacity = (oldCapacity * 3) / 2 + 1;
      array_ = Arrays.copyOf(array_, newCapacity);
    }
  }

  /**
   * Trim the back-end array to the actual size of the list.
   * Typically done to save space when the list will grow no longer.
   */
  public void trimToSize()
  {
    int capacity = array_.length;
    if (size_ < capacity)
      array_ = Arrays.copyOf(array_, size_);
  }

  private class Itr implements Iterator<Double>
  {
    protected int cursor_;       // index of next element to return
    protected int lastRet_ = -1; // index of last element returned; -1 if no such
    // private int expectedModificationCount_ = modificationCount;

    public boolean hasNext()
    {
      return cursor_ != size_;
    }

    public Double next()
    {
      checkForComodification();

      int i = cursor_;
      if (i >= size_)
        throw new NoSuchElementException();

      if (i >= array_.length)
        throw new ConcurrentModificationException();

      cursor_ = i + 1;

      double value = array_[lastRet_ = i];
      return !Double.isNaN(value) ? value : null;
    }

    public void remove()
    {
      if (lastRet_ < 0)
        throw new IllegalStateException();

      checkForComodification();

      try {
        DoubleList.this.remove(lastRet_);
        cursor_ = lastRet_;
        lastRet_ = -1;
        //expectedModCount = modCount;
      }
      catch (IndexOutOfBoundsException exception) {
        throw new ConcurrentModificationException();
      }
    }

    protected void checkForComodification()
    {
      // if (modCount != expectedModCount)
      //  throw new ConcurrentModificationException();
    }
  }

  /**
   * An optimized version of AbstractList.ListItr
   */
  private class ListItr extends Itr implements ListIterator<Double>
  {
    private ListItr(int index)
    {
      cursor_ = index;
    }

    public boolean hasPrevious()
    {
      return cursor_ != 0;
    }

    public int nextIndex()
    {
      return cursor_;
    }

    public int previousIndex()
    {
      return cursor_ - 1;
    }

    public Double previous()
    {
      checkForComodification();

      int i = cursor_ - 1;
      if (i < 0)
        throw new NoSuchElementException();

      double[] array = array_;
      if (i >= array_.length)
        throw new ConcurrentModificationException();

      cursor_ = i;
      double value = array[lastRet_ = i];

      return !Double.isNaN(value) ? value : null;
    }

    public void set(Double value)
    {
      if (lastRet_ < 0)
        throw new IllegalStateException();

      checkForComodification();

      try {
        DoubleList.this.set(lastRet_, value);
      }
      catch (IndexOutOfBoundsException exception) {
        throw new ConcurrentModificationException();
      }
    }

    public void add(Double value)
    {
      checkForComodification();

      try {
        int i = cursor_;
        DoubleList.this.add(i, value);
        cursor_ = i + 1;
        lastRet_ = -1;
        // expectedModCount = modCount;
      }
      catch (IndexOutOfBoundsException exception) {
        throw new ConcurrentModificationException();
      }
    }
  }
}
