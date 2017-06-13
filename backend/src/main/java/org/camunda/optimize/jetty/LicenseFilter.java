package org.camunda.optimize.jetty;

import org.camunda.bpm.licensecheck.InvalidLicenseException;
import org.camunda.optimize.service.license.LicenseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LicenseFilter implements Filter {

  private Logger logger = LoggerFactory.getLogger(LicenseManager.class);

  private static final String INDEX_PAGE = "/";
  private static final String LOGIN_PAGE = "/login";
  private static final String LICENSE_PAGE = "/license";

  private LicenseManager licenseManager;

  private SpringAwareServletConfiguration awareDelegate;

  public LicenseFilter(SpringAwareServletConfiguration awareDelegate) {
    this.awareDelegate = awareDelegate;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // nothing to do here
  }

  /**
   * Before the user can access the login page a license check is performed.
   * Whenever there is an invalid or no license, the user gets redirected to the license page.
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    setLicenseManager();
    HttpServletResponse servletResponse = (HttpServletResponse) response;
    HttpServletRequest servletRequest = (HttpServletRequest) request;
    String requestPath = servletRequest.getServletPath().toLowerCase();
    if (isIndexPage(requestPath) || isLoginPage(requestPath)) {
      String licenseAsString = retrieveLicense();
      if(!licenseManager.isValidOptimizeLicense(licenseAsString)) {
        logger.warn("Given License is invalid or not available, redirecting to license page!");
        servletResponse.sendRedirect(LICENSE_PAGE);
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private String retrieveLicense() {
    String license = null;
    try {
      license = licenseManager.retrieveStoredOptimizeLicense();
    } catch (InvalidLicenseException ignored) {
      // nothing to do
    }
    return license;
  }

  private void setLicenseManager() {
    if (licenseManager == null) {
      licenseManager = awareDelegate.getApplicationContext().getBean(LicenseManager.class);
    }
  }

  private boolean isIndexPage(String requestPath) {
    return requestPath.equals(INDEX_PAGE);
  }

  private boolean isLoginPage(String requestPath) {
    return requestPath.startsWith(LOGIN_PAGE);
  }

  @Override
  public void destroy() {
    // nothing to do here
  }
}
