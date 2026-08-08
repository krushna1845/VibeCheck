package com.krushna.moviebooking.payment.config;

/**
 * Disabled redundant SecurityConfig.
 * Payment service security filter chain is configured in {@link SecurityAndRetryConfig}.
 */
//@Configuration
//@EnableWebSecurity
public class SecurityConfig {

    //@Bean
    //public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //    http
    //        .csrf(AbstractHttpConfigurer::disable)
    //        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    //    return http.build();
    //}
}

