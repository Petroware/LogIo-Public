package no.petroware.logio.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A List implementation wrapping a native float array.
 * <p>
 * Useful if the array becomes <em>very</em> large as this is both
 * a lot faster and requires less storage than a List&lt;Float&gt;.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class FloatList implements List<Float>
{
  private float[] array_;

  private int size_ = 0;

  public FloatList(int capacity)
  {
    array_ = new float[capacity];
  }

  public FloatList()
  {
    // A large initial capacity to indicate the fact that the
    // class should mainly be with very large collections.
    this(1000);
  }

  /** {@inheritDoc} */
  @Override
  public boolean add(Float value)
  {
    ensureCapacity(size_ + 1);
    array_[size_++] = value != null ? value : Float.NaN;
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public void add(int index, Float value)
  {
    if (index < 0 || index > size_)
      throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size_);

    ensureCapacity(size_ + 1);
    System.arraycopy(array_, index, array_, index + 1, size_ - index);

    array_[index] = value != null ? value : Float.NaN;

    size_++;
  }

  /** {@inheritDoc} */
  @Override
  public boolean addAll(Collection<? extends Float> values)
  {
    for (Float value : values)
      add(value);
    return values.size() > 0;
  }

  /** {@inheritDoc} */
  @Override
  public boolean addAll(int index, Collection<? extends Float> values)
  {
    int d = 0;
    for (Float value : values) {
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
    if (!(value instanceof Float))
      return -1;

    float v = value != null ? (Float) value : Float.NaN;

    for (int index = 0; index < size_; index++)
      if (v == array_[index])
        return index;

    return -1;
  }

  /** {@inheritDoc} */
  @Override
  public int lastIndexOf(Object value)
  {
    if (!(value instanceof Float))
      return -1;

    float v = value != null ? (Float) value : Float.NaN;

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
    for (Float value : this)
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

    ListIterator<Float> e1 = listIterator();
    ListIterator<?> e2 = ((List) object).listIterator();

    while (e1.hasNext() && e2.hasNext()) {
      Float o1 = e1.next();
      Object o2 = e2.next();

      if (!(o1 == null ? o2 == null : o1.equals(o2)))
        return false;
    }

    return !(e1.hasNext() || e2.hasNext());
  }

  /** {@inheritDoc} */
  @Override
  public Float get(int index)
  {
    if (index < 0 || index > size_)
      throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size_);

    float v = array_[index];

    return Float.isNaN(v) ? null : v;
  }

  /** {@inheritDoc} */
  @Override
  public Float set(int index, Float value)
  {
    if (index < 0 || index > size_)
      throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size_);

    float v = value != null ? value : Float.NaN;

    float oldValue = array_[index];
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
  public List<Float> subList(int fromIndex, int toINdex)
  {
    throw new UnsupportedOperationException("Not supported. Use java.util.ArrayList instead");
  }

  /** {@inheritDoc} */
  @Override
  public ListIterator<Float> listIterator()
  {
    return new ListItr(0);
  }

  /** {@inheritDoc} */
  @Override
  public ListIterator<Float> listIterator(int index)
  {
    if (index < 0 || index > size_)
      throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size_);

    return new ListItr(index);
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Float> iterator()
  {
    return listIterator();
  }

  /** {@inheritDoc} */
  @Override
  public Float remove(int index)
  {
    // modificationCount++;
    float oldValue = array_[index];

    int nMoved = size_ - index - 1;
    if (nMoved > 0)
      System.arraycopy(array_, index + 1, array_, index, nMoved);

    size_--;

    return !Float.isNaN(oldValue) ? oldValue : null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean remove(Object object)
  {
    if (!(object instanceof Float))
      return false;

    Float value = (Float) object;
    float v = value != null ? value : Float.NaN;

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
      float value = array_[i];
      array[i] = !Float.isNaN(value) ? value : null;
    }

    return array;
  }

  /** {@inheritDoc} */
  @Override
  public <Float> Float[] toArray(Float[] array)
  {
    throw new UnsupportedOperationException("Use toArray() instead");
  }

  /**
   * Ensure that the backing list as enough capacity for the specified
   * number of entries.
   *
   * @param size  Size of elements. [0,&gt;.
   */
  private void ensureCapacity(int size)
  {
    int oldCapacity = array_.length;
    if (size > oldCapacity) {
      float[] oldArray = array_; // ??
      int newCapacity = (oldCapacity * 3) / 2 + 1;
      array_ = Arrays.copyOf(array_, newCapacity);
    }
  }

  public void trimToSize()
  {
    int capacity = array_.length;
    if (size_ < capacity)
      array_ = Arrays.copyOf(array_, size_);
  }

  private class Itr implements Iterator<Float>
  {
    protected int cursor_;       // index of next element to return
    protected int lastRet_ = -1; // index of last element returned; -1 if no such
    // private int expectedModificationCount_ = modificationCount;

    public boolean hasNext()
    {
      return cursor_ != size_;
    }

    public Float next()
    {
      checkForComodification();

      int i = cursor_;
      if (i >= size_)
        throw new NoSuchElementException();

      float[] array = array_;
      if (i >= array_.length)
        throw new ConcurrentModificationException();

      cursor_ = i + 1;

      float value = array_[lastRet_ = i];
      return !Float.isNaN(value) ? value : null;
    }

    public void remove()
    {
      if (lastRet_ < 0)
        throw new IllegalStateException();

      checkForComodification();

      try {
        FloatList.this.remove(lastRet_);
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
  private class ListItr extends Itr implements ListIterator<Float>
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

    public Float previous()
    {
      checkForComodification();

      int i = cursor_ - 1;
      if (i < 0)
        throw new NoSuchElementException();

      float[] array = array_;
      if (i >= array_.length)
        throw new ConcurrentModificationException();

      cursor_ = i;
      float value = array[lastRet_ = i];

      return !Float.isNaN(value) ? value : null;
    }

    public void set(Float value)
    {
      if (lastRet_ < 0)
        throw new IllegalStateException();

      checkForComodification();

      try {
        FloatList.this.set(lastRet_, value);
      }
      catch (IndexOutOfBoundsException exception) {
        throw new ConcurrentModificationException();
      }
    }

    public void add(Float value)
    {
      checkForComodification();

      try {
        int i = cursor_;
        FloatList.this.add(i, value);
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
