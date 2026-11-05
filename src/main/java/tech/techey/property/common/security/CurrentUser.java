package tech.techey.property.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Injects the authenticated {@link UserPrincipal} into a controller method parameter.
 * Thin alias over {@link AuthenticationPrincipal} so controllers read
 * {@code @CurrentUser UserPrincipal principal} per architecture doc §3.3.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {
}
