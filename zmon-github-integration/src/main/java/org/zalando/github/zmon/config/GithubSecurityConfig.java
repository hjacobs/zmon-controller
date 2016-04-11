package org.zalando.github.zmon.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.social.security.SocialUserDetailsService;
import org.springframework.social.security.SpringSocialConfigurer;
import org.zalando.github.zmon.service.GithubResourceServerTokenServices;
import org.zalando.zmon.security.AuthorityService;
import org.zalando.zmon.security.WebSecurityConstants;
import org.zalando.zmon.security.ZmonResourceServerConfigurer;
import org.zalando.zmon.security.service.ChainedResourceServerTokenServices;
import org.zalando.zmon.security.service.PresharedTokensResourceServerTokenServices;
import org.zalando.zmon.security.service.SimpleSocialUserDetailsService;
import org.zalando.zmon.security.tvtoken.TvTokenService;
import org.zalando.zmon.security.tvtoken.ZMonTvRememberMeServices;

import com.google.common.collect.ImmutableList;

/**
 * Nothing to add here.
 *
 * @author jbellmann
 */
@Configuration
@EnableWebSecurity
// The EnableResourceServer creates a WebSecurityConfigurerAdapter with a hard-coded Order (of 3).
@EnableResourceServer
@Order(4)
public class GithubSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    AuthorityService authorityService;

    @Autowired
    Environment environment;

    @Autowired
    private TvTokenService TvTokenService;

    @Autowired
    public void configureGlobal(final AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsManager());
    }

    @Override
    public void configure(final WebSecurity web) throws Exception {
        web.ignoring().antMatchers(WebSecurityConstants.IGNORED_PATHS);
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    // J-
    // @formatter:off
    @Override
    protected void configure( HttpSecurity http) throws Exception {

        http = http.authenticationProvider(new RememberMeAuthenticationProvider("ZMON_TV"));

        http
            .apply(new SpringSocialConfigurer())
            .and()
                .formLogin()
                    .loginPage("/signin")
                    .failureUrl("/signin?error=bad_credentials")
                    .permitAll()
            .and()
                .logout()
                    .logoutUrl("/logout")
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessUrl("/signin?logout=true")
                    .permitAll()
            .and()
                .authorizeRequests()
                .antMatchers("/**")
                    .authenticated()
            .and()
                .rememberMe()
                .rememberMeServices(new ZMonTvRememberMeServices(TvTokenService))
            .and()
                .csrf()
                    .disable()
                .headers()
                    .frameOptions()
                        .disable();
    }
    // @formatter:on
    // J+

    @Bean
    public SocialUserDetailsService socialUserDetailsService() {
        return new SimpleSocialUserDetailsService(userDetailsService());
    }

    @Override
    protected UserDetailsService userDetailsService() {
        return userDetailsManager();
    }

    @Bean
    public UserDetailsManager userDetailsManager() {
        return new InMemoryUserDetailsManager(new ArrayList<UserDetails>());
    }

    @Bean
    public ResourceServerConfigurer zmonResourceServerConfigurer() {
        final List<ResourceServerTokenServices> chain = ImmutableList.of(
                new PresharedTokensResourceServerTokenServices(authorityService, environment),
                new GithubResourceServerTokenServices(authorityService, environment));
        return new ZmonResourceServerConfigurer(new ChainedResourceServerTokenServices(chain));
    }
}
