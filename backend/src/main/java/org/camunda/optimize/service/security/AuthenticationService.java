package org.camunda.optimize.service.security;

import org.camunda.optimize.dto.optimize.query.CredentialsDto;
import org.camunda.optimize.service.exceptions.UnauthorizedUserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationService {

  @Autowired
  private EngineAuthenticationProvider engineAuthenticationProvider;

  @Autowired
  private TokenService tokenService;

  public String authenticateUser(CredentialsDto credentials) throws UnauthorizedUserException {
    boolean authorizedInEngine = engineAuthenticationProvider.authenticate(credentials);

    if (!authorizedInEngine) {
      throw new UnauthorizedUserException("Can't authorize user [" + credentials.getUsername() + "]");
    }

    // Issue a token for the user
    String token = tokenService.issueToken(credentials.getUsername());
    return token;
  }
}
