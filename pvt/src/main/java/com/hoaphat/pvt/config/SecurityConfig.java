package com.hoaphat.pvt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    UserDetailsService userDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public AuthenticationSuccessHandler myAuthenticationSuccessHandler() {
        return new MyCustomAuthenticationSuccessHandler();
    }

    // 🔥 THÊM MỚI: Yêu cầu Spring Security bỏ qua hoàn toàn các file Media
    // Giúp Cloudflare Cache được file mà không bị dính Header "no-cache" hay "Set-Cookie"
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/img-local/**", "/vid-local/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests()
                .antMatchers("/", "/login", "/js/**", "/css/**").permitAll()
                .antMatchers("/home/employee/**").hasAnyRole("EMPLOYEE", "MANAGER")
                .antMatchers("/home/manager/**").hasRole("MANAGER")
                // ✅ Bắt tất cả route còn lại (kể cả /home) yêu cầu đăng nhập
                .anyRequest().authenticated();

        http.exceptionHandling().accessDeniedPage("/deny");

        http.formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/doLogin")
                .usernameParameter("accountname")
                .passwordParameter("password")
                .defaultSuccessUrl("/home", true)
                .successHandler(myAuthenticationSuccessHandler())
                .failureUrl("/?error=true");

        http.logout().logoutUrl("/logout");
    }
}