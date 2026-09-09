package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.application.iam.account.model.security.AuthenticatedSocialIdentity;

public interface AuthenticatedSocialIdentityPortIn {

    AuthenticatedSocialIdentity getAuthenticatedIdentityOrThrow();
}
