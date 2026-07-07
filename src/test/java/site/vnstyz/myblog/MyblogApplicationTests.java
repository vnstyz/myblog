package site.vnstyz.myblog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class MyblogApplicationTests {

    @Test
    void contextLoads(ApplicationContext context) {
        System.out.println("Spring context loaded successfully");
        System.out.println("Available beans: " + context.getBeanDefinitionCount());
    }

}
