package xyz.juandiii.ark.core.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter as a multipart form-data part.
 * If the parameter is a {@link java.nio.file.Path} or {@code byte[]}, it is sent as a file part.
 * Otherwise, it is sent as a text field.
 *
 * @author Juan Diego Lopez V.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestPart {

    String value();
}
