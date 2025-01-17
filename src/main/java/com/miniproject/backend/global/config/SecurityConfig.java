package com.miniproject.backend.global.config;

import com.miniproject.backend.global.jwt.JwtAuthorizationFilter;
import com.miniproject.backend.global.jwt.auth.AuthTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthTokenProvider authTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                .cors()//기본 cors 설정
                .and()
                .csrf().disable()
                .formLogin().disable() //formLogin 인증 비활성화
                .httpBasic().disable() //httpBasic 인증 비활성화
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests().antMatchers(
                        "/oauth2/**", "/", "/login/**", "/signIn", "/refresh", "/swagger-ui/**", "/api-docs/**")
                .permitAll()
                .anyRequest().authenticated();

        http
                .apply(new customConfig());

        return http.build();
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    public class customConfig extends AbstractHttpConfigurer<customConfig, HttpSecurity> {
        @Override
        public void configure(HttpSecurity http) throws Exception {
            AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
            http
                    .addFilterAfter(new JwtAuthorizationFilter(authenticationManager, authTokenProvider), CorsFilter.class);
        }
    }
}
