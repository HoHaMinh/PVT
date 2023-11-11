package com.hoaphat.pvt.config;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.service.account.IAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class MyCustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Autowired
    private IAccountService accountService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername(); // Get the username
        List<Account> accountList = accountService.findAll();
        for (int i = 0; i < accountList.size(); i++) {
            if (username.equals(accountList.get(i).getAccountName())) {
                username = accountList.get(i).getName();
            }
        }
        request.getSession().setAttribute("username", username);
        response.sendRedirect("/home");
    }
}
