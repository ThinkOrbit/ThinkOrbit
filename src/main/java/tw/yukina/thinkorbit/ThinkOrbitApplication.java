package tw.yukina.thinkorbit;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ThinkOrbitApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(ThinkOrbitApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
