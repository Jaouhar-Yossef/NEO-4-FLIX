package com.neo4flix.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-secret-for-unit-tests-only-0123456789")
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
