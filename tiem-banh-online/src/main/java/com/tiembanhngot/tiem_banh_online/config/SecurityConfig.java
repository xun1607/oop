package com.tiembanhngot.tiem_banh_online.config;

import com.tiembanhngot.tiem_banh_online.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // **Thêm để dùng @PreAuthorize**
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // **Bật @PreAuthorize, @PostAuthorize,...**
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // **Phân quyền cụ thể hơn**
                .requestMatchers(
                        "/",
                        "/products",          // Cho phép list page
                        "/products/*",        // Cho phép detail pages
                        "/api/v1/products/**",// Cho phép API (nếu có)
                        "/css/**", "/js/**", "/img/**", "/webjars/**", "/favicon.ico", // Tài nguyên tĩnh
                        "/login", "/register", // Trang đăng nhập, đăng ký
                        "/error"               // Trang lỗi mặc định
                ).permitAll()
                .requestMatchers(
                        "/cart/**",           // Giỏ hàng
                        "/checkout/**",       // Thanh toán (sẽ làm sau)
                        "/account/**",        // Trang tài khoản
                        "/orders/**"          // Trang đơn hàng cá nhân
                ).authenticated() // Yêu cầu đã đăng nhập
                // **Phân quyền cho Admin**
                .requestMatchers("/admin/**").hasRole("ADMIN") // Yêu cầu quyền ADMIN
                // Bất kỳ request nào khác chưa được định nghĩa ở trên đều yêu cầu xác thực
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login") // URL form login POST tới
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
            // .csrf(csrf -> csrf.disable()); // Tạm thời có thể tắt nếu FE chưa xử lý CSRF token trong AJAX

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
            .userDetailsService(customUserDetailsService)
            .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }
}