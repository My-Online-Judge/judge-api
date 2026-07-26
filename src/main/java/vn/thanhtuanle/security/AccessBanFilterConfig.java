package vn.thanhtuanle.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link AccessBanFilter} at order -200 — before springSecurityFilterChain
 * (order -100) — so a ban covers every endpoint, including permitAll ones. Having this
 * registration bean also stops Boot from auto-registering the @Component filter a second
 * time. Lives here (not in SecurityConfig) so @WebMvcTest slices that @Import
 * SecurityConfig don't drag in the Redis-backed filter dependencies.
 */
@Configuration
public class AccessBanFilterConfig {

    @Bean
    public AccessBanFilter accessBanFilter(AccessBanMirror mirror,
                                           com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                                           io.micrometer.core.instrument.MeterRegistry registry) {
        return new AccessBanFilter(mirror, objectMapper, registry);
    }

    @Bean
    public FilterRegistrationBean<AccessBanFilter> accessBanFilterRegistration(AccessBanFilter filter) {
        FilterRegistrationBean<AccessBanFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(-200);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
