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
        String accountName = userDetails.getUsername();
        String canonicalAccountName = accountName;
        String displayName = accountName;

        List<Account> accountList = accountService.findAll();
        for (int i = 0; i < accountList.size(); i++) {
            Account account = accountList.get(i);
            if (accountName != null
                    && account.getAccountName() != null
                    && accountName.equalsIgnoreCase(account.getAccountName())) {
                canonicalAccountName = account.getAccountName();
                String configuredName = account.getName();
                if (configuredName != null && !configuredName.trim().isEmpty()) {
                    displayName = configuredName.trim();
                }
                break;
            }
        }

        request.getSession().setAttribute("username", displayName);
        request.getSession().setAttribute("accountName", canonicalAccountName);
        response.sendRedirect("/home");
    }
}
