package tech.apirest.mail.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tech.apirest.mail.Entity.Users;
import tech.apirest.mail.Services.UsersInterfaceImpl;

import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UsersInterfaceImpl usersInterface;

    public SecurityConfig(UsersInterfaceImpl usersInterface) {
        this.usersInterface = usersInterface;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(username -> {
            Users users = usersInterface.findByLog(username);
            if (users == null) {
                System.out.println("Utilisateur non trouvé : " + username);
                throw new UsernameNotFoundException("Utilisateur non trouvé");
            }
            System.out.println("Utilisateur trouvé : " + username);
            return new User(users.getUserid(), users.getPassword(), Collections.emptyList());
        }).passwordEncoder(passwordEncoder());
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/home", "/CreateAccount","/createMail","/webjars/**", "/css/**", "/js/**").permitAll() // Ressources accessibles sans authentification
                .anyRequest().authenticated() // Authentification requise pour toutes les autres requêtes
                .and()
                .formLogin()
                .loginPage("/login") // Page de connexion personnalisée
                .defaultSuccessUrl("/accueilMail", true) // Redirection après connexion réussie
                .failureUrl("/home?error=true") // Redirection après échec
                .permitAll()
                .and()
                .logout()
                .logoutUrl("/logout")

                .logoutSuccessUrl("/login?logout=true") // Redirection après déconnexion
                .permitAll();
    }
}
