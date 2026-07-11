package site.vnstyz.myblog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class MyblogApplicationTests {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testPasswordEncoder() {
        String passwd = "12345678";
        String encode = passwordEncoder.encode(passwd);
        System.out.println(encode);
        System.out.println(passwordEncoder.matches(passwd, encode));
//        $2a$10$cpbglzZrXZu857Gg7hFhWekQVzTdd3rI9QnG67WtkVcSmk.HbJl/2
//        $2a$10$Lnxlo8cdPpuEnQIIvvSWUuGGiofTx/fSkyWZIFTPNHZO64oJ7JUtq
    }





    @Test
    void contextLoads(ApplicationContext context) {
        System.out.println("Spring context loaded successfully");
        System.out.println("Available beans: " + context.getBeanDefinitionCount());
    }

}
