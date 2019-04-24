package com.remondis.remap;

import static com.remondis.remap.Properties.asString;

import java.beans.PropertyDescriptor;
import java.util.Optional;

import com.remondis.propertypath.api.Get;
import com.remondis.propertypath.api.Getter;
import com.remondis.propertypath.api.PropertyPath;

/**
 * The property path operation maps the result of a property path that is applied to the source field to another field.
 * If the property path does not return a value, the mapping is omitted.
 *
 * @param <RS> The type of the source field.
 * @param <RD> The type of the destination field.
 * @author schuettec
 */
public class PropertyPathTransformation<RS, RD> extends Transformation {

  private static final String PROPERTY_PATH_MSG = "Replacing %s\n           with %s\n"
      + "           using property path: %s";
  private Get<RS, RD, ?> propertyPath;

  PropertyPathTransformation(Mapping<?, ?> mapping, PropertyDescriptor sourceProperty,
      PropertyDescriptor destinationProperty, PropertyPath<RD, RS, ?> propertyPath) {
    super(mapping, sourceProperty, destinationProperty);
    this.propertyPath = createGetter(sourceProperty, propertyPath);
  }

  @SuppressWarnings("unchecked")
  private Get<RS, RD, ?> createGetter(PropertyDescriptor sourceProperty, PropertyPath<RD, RS, ?> propertyPath) {
    return Getter.newFor((Class<RS>) sourceProperty.getPropertyType())
        .evaluate(propertyPath);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void performTransformation(PropertyDescriptor sourceProperty, Object source,
      PropertyDescriptor destinationProperty, Object destination) throws MappingException {
    Object sourceValue = readOrFail(sourceProperty, source);

    if (sourceValue == null) {
      // Skip if source value is null. Property paths are null-friendly.
      return;
    }

    try {
      Optional<RD> optional = propertyPath.from((RS) sourceValue);
      if (optional.isPresent()) {
        RD destinationValue = optional.get();
        writeOrFail(destinationProperty, destination, destinationValue);
      }
    } catch (Exception e) {
      throw new MappingException(
          String.format("The property path for mapping %s to %s evaluating %s failed with an exception.",
              asString(sourceProperty), asString(destinationProperty), propertyPath.toString()),
          e);
    }
  }

  @Override
  protected void validateTransformation() throws MappingException {
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((propertyPath == null) ? 0 : propertyPath.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PropertyPathTransformation other = (PropertyPathTransformation) obj;
    if (propertyPath == null) {
      if (other.propertyPath != null) {
        return false;
      }
    } else if (!propertyPath.equals(other.propertyPath)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString(boolean detailed) {
    return String.format(PROPERTY_PATH_MSG, asString(sourceProperty, detailed), asString(destinationProperty, detailed),
        propertyPath.toString(detailed));
  }
}