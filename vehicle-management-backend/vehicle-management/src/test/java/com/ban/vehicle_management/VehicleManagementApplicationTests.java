package com.ban.vehicle_management;

import com.ban.vehicle_management.application.iam.account.port.out.SocialAccountRegistrationPortOut;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class VehicleManagementApplicationTests {

	@Autowired
	private SocialAccountRegistrationPortOut socialAccountRegistrationPortOut;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void socialAccountRegistrationLockExecutesOnPostgres() {
		socialAccountRegistrationPortOut.lockRegistration(
				SocialIdentityProvider.GOOGLE,
				"integration-test-google-subject",
				"integration-test@example.com"
		);
	}

}


