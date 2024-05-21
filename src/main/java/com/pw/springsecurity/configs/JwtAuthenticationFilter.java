package com.pw.springsecurity.configs;

import com.pw.springsecurity.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final HandlerExceptionResolver handlerExceptionResolver;

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    /**
     *
     * We override doFilterInternal who get from our servlet request send by le client, header info, here it will get from the header the content of attribute
     * Authorization
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        //there is not header Authorization or no token, we let filter chain to do normal filtering and get out of function
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        /* in this case we get the token we extract email from jwt token and we get the context

         */
        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // user exist in token and he is not authenticated because the context have not been set yet
            if (userEmail != null && authentication == null) {
                /* we find in db if that user exist we have implemented and declare as bean our own UserDetailsService to find user in ApplicationConfiguration.jave
                so this.userDetailsService.loadUserByUsername(userEmail); will call the implementation define in our bean (userRepository.findByEmail(username))
                 */
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // if token is valid filter set the context with authentication Object (principal, credentials authorities or roles and with authenticated param set as true)to validate authentication
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            /**
             * A try-catch block wraps the logic and uses the HandlerExceptionResolver to forward the error to the global exception handler.
             * We will see how it can be helpful to do the exception forwarding.
             */
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
