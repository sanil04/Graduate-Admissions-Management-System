package com.sysc4806.gradadmissions.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.sysc4806.gradadmissions.security.RoleHomeSuccessHandler;

/**
 * Who can open which URL. Users log in with their email and password on /login; the user lookup
 * is {@link com.sysc4806.gradadmissions.security.AppUserDetailsService}.
 * Controller tests that should run with these rules add {@code @Import(SecurityConfig.class)}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/login", "/register", "/error", "/css/**", "/favicon.ico").permitAll()
						// Only reachable when spring.h2.console.enabled=true (never in prod).
						.requestMatchers("/h2-console/**").permitAll()
						// "/admin/**" also matches "/admin" itself.
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/professor/**").hasRole("PROFESSOR")
						.requestMatchers("/applicant/**", "/apply/**").hasRole("APPLICANT")
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.usernameParameter("email")
						.successHandler(new RoleHomeSuccessHandler())
						.permitAll())
				// Logout is a POST to /logout (with the CSRF token), so a link cannot log anyone out.
				.logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
				// The H2 console posts its own forms without our CSRF token and runs inside frames.
				.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		return http.build();
	}
}
